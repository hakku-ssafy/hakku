import { describe, it, expect } from 'vitest'
import { hangulToQwerty } from './hangulToQwerty'

describe('hangulToQwerty', () => {
  it('빈 문자열은 그대로 반환한다', () => {
    expect(hangulToQwerty('')).toBe('')
  })

  it('영문/숫자/기호는 변환 없이 통과한다', () => {
    expect(hangulToQwerty('pass123!')).toBe('pass123!')
    expect(hangulToQwerty('Abc_99')).toBe('Abc_99')
  })

  it('단일 자음을 두벌식 키로 변환한다', () => {
    expect(hangulToQwerty('ㄱ')).toBe('r')
    expect(hangulToQwerty('ㅎ')).toBe('g')
    expect(hangulToQwerty('ㅁ')).toBe('a')
  })

  it('단일 모음을 두벌식 키로 변환한다', () => {
    expect(hangulToQwerty('ㅏ')).toBe('k')
    expect(hangulToQwerty('ㅣ')).toBe('l')
  })

  it('쌍자음은 Shift 키(대문자)로 변환한다', () => {
    expect(hangulToQwerty('ㄲ')).toBe('R')
    expect(hangulToQwerty('ㅃ')).toBe('Q')
  })

  it('받침 없는 음절을 분해해 변환한다', () => {
    // 가 = ㄱ(r) + ㅏ(k)
    expect(hangulToQwerty('가')).toBe('rk')
    // 까 = ㄲ(R) + ㅏ(k)
    expect(hangulToQwerty('까')).toBe('Rk')
  })

  it('받침 있는 음절을 분해해 변환한다', () => {
    // 한 = ㅎ(g) + ㅏ(k) + ㄴ(s)
    expect(hangulToQwerty('한')).toBe('gks')
    // 안녕 = ㅇ(d)ㅏ(k)ㄴ(s) + ㄴ(s)ㅕ(u)ㅇ(d)
    expect(hangulToQwerty('안녕')).toBe('dkssud')
  })

  it('겹모음을 두 키로 변환한다', () => {
    // 와 = ㅇ(d) + ㅘ(h+k)
    expect(hangulToQwerty('와')).toBe('dhk')
    // 의 = ㅇ(d) + ㅢ(m+l)
    expect(hangulToQwerty('의')).toBe('dml')
  })

  it('겹받침을 두 키로 변환한다', () => {
    // 값 = ㄱ(r) + ㅏ(k) + ㅄ(q+t)
    expect(hangulToQwerty('값')).toBe('rkqt')
    // 닭 = ㄷ(e) + ㅏ(k) + ㄺ(f+r)
    expect(hangulToQwerty('닭')).toBe('ekfr')
  })

  it('한글과 영문/숫자가 섞인 문자열을 변환한다', () => {
    // 비밀 = ㅂ(q)ㅣ(l) + ㅁ(a)ㅣ(l)ㄹ(f)
    expect(hangulToQwerty('비밀123')).toBe('qlalf123')
  })

  it('이미 영문으로 변환된 결과에 다시 적용해도 동일하다(멱등성)', () => {
    const once = hangulToQwerty('비밀번호')
    expect(hangulToQwerty(once)).toBe(once)
  })
})
