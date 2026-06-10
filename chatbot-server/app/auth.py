"""main-server가 발급한 HS256 JWT 검증 (로그인 사용자 전용 챗봇)."""
import base64

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.config import settings

_bearer = HTTPBearer(auto_error=False)


def get_current_user_id(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer),
) -> str:
    """Authorization: Bearer 토큰을 검증하고 사용자 ID(sub)를 반환한다."""
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="로그인이 필요한 서비스입니다.",
        )

    try:
        # main-server(JJWT)와 동일하게 base64 디코딩된 시크릿으로 검증
        key = base64.b64decode(settings.jwt_secret)
        payload = jwt.decode(credentials.credentials, key, algorithms=["HS256"])
    except (jwt.PyJWTError, ValueError):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="유효하지 않은 인증 정보입니다. 다시 로그인해주세요.",
        )

    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="유효하지 않은 인증 정보입니다. 다시 로그인해주세요.",
        )
    return user_id
