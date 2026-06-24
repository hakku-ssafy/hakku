"""main-server 호출 클라이언트.

챗봇 고객센터 도구가 사용자 데이터를 조회할 때 사용한다. 사용자의 JWT 를 그대로
Authorization 헤더로 전달하므로, main-server 의 기존 인증/인가가 그대로 적용된다.
"""
import httpx


class MainServerClient:
    def __init__(self, base_url: str, token: str, timeout: float = 5.0):
        self._base = base_url.rstrip("/")
        self._headers = {"Authorization": f"Bearer {token}"}
        self._timeout = timeout

    async def _get(self, path: str):
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            response = await client.get(f"{self._base}{path}", headers=self._headers)
            response.raise_for_status()
            return response.json()

    async def get_orders(self) -> list:
        return await self._get("/api/orders")

    async def get_wishlist(self, user_id: str) -> list:
        return await self._get(f"/api/users/{user_id}/wishlist")

    async def get_recommendations(self) -> list:
        return await self._get("/api/recommendations")
