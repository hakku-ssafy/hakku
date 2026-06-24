import { describe, it, expect, beforeEach } from 'vitest'
import axios, { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import apiClient from '../client'

/**
 * 전역 axios 인터셉터의 401 → 리프레시 → 재시도 동작 검증.
 * 실제 서버 없이 mock adapter 로 요청을 가로채 401/refresh/재시도 흐름을 결정적으로 시뮬레이션한다.
 *
 * 배경: 상품 페이지에서 결제(보호된 payment-server 요청)를 누르면 만료 액세스 토큰이 처음으로 검증돼
 * 401 → 기존 인터셉터가 즉시 로그아웃시키던 버그. 리프레시-재시도로 무중단 세션을 보장한다.
 */

let adapter: (config: InternalAxiosRequestConfig) => Promise<AxiosResponse>
let locationHref: string

// 실제 axios 어댑터처럼 validateStatus 를 흉내낸다 — 2xx 는 resolve, 그 외는 AxiosError 로 reject
// (커스텀 adapter 는 settle 을 직접 호출해야 하므로 여기서 상태에 따라 분기한다).
function reply(status: number, data: unknown, config: InternalAxiosRequestConfig): Promise<AxiosResponse> {
  const response = { data, status, statusText: '', headers: {}, config, request: {} } as AxiosResponse
  if (status >= 200 && status < 300) return Promise.resolve(response)
  return Promise.reject(new AxiosError(`mock ${status}`, String(status), config, undefined, response))
}

const tick = () => new Promise((r) => setTimeout(r, 0))

beforeEach(() => {
  localStorage.clear()
  locationHref = ''
  Object.defineProperty(window, 'location', {
    configurable: true,
    writable: true,
    value: {
      get href() {
        return locationHref
      },
      set href(v: string) {
        locationHref = v
      },
    },
  })
  // apiClient(인스턴스)와 bare axios(refresh 호출) 모두 같은 mock adapter 로 라우팅.
  const route = (config: InternalAxiosRequestConfig) => adapter(config)
  apiClient.defaults.adapter = route
  axios.defaults.adapter = route
})

describe('apiClient 401 인터셉터', () => {
  it('쿠키 전송을 위해 withCredentials 가 켜져 있다', () => {
    expect(apiClient.defaults.withCredentials).toBe(true)
  })

  it('보호된 요청이 401이면 토큰을 갱신하고 원요청을 재시도해 성공한다', async () => {
    localStorage.setItem('accessToken', 'old')
    let refreshCalls = 0
    adapter = async (config) => {
      if (config.url?.includes('/auth/refresh')) {
        refreshCalls++
        return reply(200, { accessToken: 'new', tokenType: 'Bearer' }, config)
      }
      const auth = config.headers?.Authorization
      return auth === 'Bearer new' ? reply(200, { ok: true }, config) : reply(401, { message: 'expired' }, config)
    }

    const { data } = await apiClient.get('/payments/toss/prepare')

    expect(data).toEqual({ ok: true })
    expect(localStorage.getItem('accessToken')).toBe('new')
    expect(refreshCalls).toBe(1)
    expect(locationHref).toBe('') // 로그아웃/리다이렉트 없음
  })

  it('리프레시가 실패(401)하면 토큰을 지우고 /login 으로 로그아웃시킨다', async () => {
    localStorage.setItem('accessToken', 'old')
    adapter = async (config) => {
      if (config.url?.includes('/auth/refresh')) return reply(401, { message: 'no' }, config)
      return reply(401, { message: 'expired' }, config)
    }

    await expect(apiClient.get('/payments/toss/prepare')).rejects.toBeTruthy()

    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(locationHref).toBe('/login')
  })

  it('로그인 시도의 401은 리프레시·로그아웃 없이 그대로 에러를 던진다', async () => {
    localStorage.setItem('accessToken', 'old')
    let refreshCalls = 0
    adapter = async (config) => {
      if (config.url?.includes('/auth/refresh')) {
        refreshCalls++
        return reply(200, { accessToken: 'x' }, config)
      }
      return reply(401, { message: 'bad creds' }, config)
    }

    await expect(apiClient.post('/auth/login', {})).rejects.toBeTruthy()

    expect(refreshCalls).toBe(0)
    expect(locationHref).toBe('')
    expect(localStorage.getItem('accessToken')).toBe('old')
  })

  it('동시 다발 401에도 리프레시는 한 번만 수행된다', async () => {
    localStorage.setItem('accessToken', 'old')
    let refreshCalls = 0
    adapter = async (config) => {
      if (config.url?.includes('/auth/refresh')) {
        refreshCalls++
        await tick()
        return reply(200, { accessToken: 'new' }, config)
      }
      const auth = config.headers?.Authorization
      return auth === 'Bearer new' ? reply(200, { ok: true }, config) : reply(401, {}, config)
    }

    const [a, b] = await Promise.all([apiClient.get('/a'), apiClient.get('/b')])

    expect(a.data).toEqual({ ok: true })
    expect(b.data).toEqual({ ok: true })
    expect(refreshCalls).toBe(1)
  })
})
