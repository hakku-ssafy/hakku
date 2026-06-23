import { describe, it, expect } from 'vitest'
import { toSameOriginStorageUrl } from './storageUrl'

describe('toSameOriginStorageUrl', () => {
  it('cross-origin 스토리지 절대 URL을 현재 origin 기준 상대경로로 변환한다', () => {
    expect(toSameOriginStorageUrl('https://hakku.rearleg.com/storage/images/abc123')).toBe(
      '/storage/images/abc123',
    )
  })

  it('쿼리스트링을 보존한다', () => {
    expect(toSameOriginStorageUrl('https://hakku.rearleg.com/storage/images/abc?v=2')).toBe(
      '/storage/images/abc?v=2',
    )
  })

  it('이미 상대경로면 그대로 둔다', () => {
    expect(toSameOriginStorageUrl('/storage/images/abc')).toBe('/storage/images/abc')
  })

  it('same-origin 절대 URL은 그대로 둔다', () => {
    const url = `${window.location.origin}/storage/images/abc`
    expect(toSameOriginStorageUrl(url)).toBe(url)
  })

  it('스토리지 경로가 아닌 cross-origin URL은 건드리지 않는다', () => {
    const url = 'https://cdn.example.com/assets/logo.png'
    expect(toSameOriginStorageUrl(url)).toBe(url)
  })

  it('빈 문자열은 그대로 반환한다', () => {
    expect(toSameOriginStorageUrl('')).toBe('')
  })
})
