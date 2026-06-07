import axios from 'axios'

export type AuthContext = 'login' | 'signup'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** 이메일 형식이 올바른지 검사한다. */
export function isValidEmail(email: string): boolean {
  return EMAIL_RE.test(email.trim())
}

/**
 * 로그인/회원가입 실패를 사용자에게 친절한 한국어 메시지로 변환한다.
 * 원시 상태코드("Request failed with status code 400") 대신 무엇이 문제인지 설명한다.
 */
export function authErrorMessage(error: unknown, context: AuthContext): string {
  const fallback =
    context === 'signup'
      ? '회원가입에 실패했어요. 잠시 후 다시 시도해 주세요.'
      : '로그인에 실패했어요. 잠시 후 다시 시도해 주세요.'

  if (!axios.isAxiosError(error)) {
    return fallback
  }
  if (!error.response) {
    return '서버에 연결할 수 없어요. 인터넷 연결을 확인한 뒤 다시 시도해 주세요.'
  }

  const status = error.response.status
  const serverMessage = (error.response.data as { message?: string } | undefined)?.message

  switch (status) {
    case 400:
      return context === 'signup'
        ? '입력하신 정보를 다시 확인해 주세요. 이메일 형식이 올바른지, 모든 항목을 빠짐없이 입력했는지 확인해 주세요.'
        : '이메일과 비밀번호를 모두 올바르게 입력했는지 확인해 주세요.'
    case 401:
      return '이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.'
    case 409:
      return '이미 가입된 이메일이에요. 로그인하거나 다른 이메일로 가입해 주세요.'
    case 429:
      return '요청이 너무 많아요. 잠시 후 다시 시도해 주세요.'
    default:
      if (status >= 500) {
        return '서버에 일시적인 문제가 발생했어요. 잠시 후 다시 시도해 주세요.'
      }
      return serverMessage ?? fallback
  }
}
