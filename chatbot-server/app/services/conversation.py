"""대화 기억 저장소.

Redis Sorted Set(`chat:history:{user_id}`)에 메시지를 timestamp(ms) 점수로 저장한다.
조회할 때마다 1시간(window_seconds)이 지난 메시지를 오래된 순으로 제거하므로,
'1시간 슬라이딩 윈도우'로 최근 대화만 기억한다. 멀티 워커/재시작에도 유지된다.
"""
import json
from typing import Protocol

CONVERSATION_WINDOW_SECONDS = 3600


class _RedisLike(Protocol):
    async def zadd(self, key: str, mapping: dict) -> object: ...
    async def expire(self, key: str, seconds: int) -> object: ...
    async def zremrangebyscore(self, key: str, min_score: float, max_score: float) -> object: ...
    async def zrange(self, key: str, start: int, stop: int) -> list: ...


class ConversationStore:
    def __init__(self, redis: "_RedisLike", window_seconds: int = CONVERSATION_WINDOW_SECONDS):
        self._redis = redis
        self._window = window_seconds

    @staticmethod
    def _key(user_id: str) -> str:
        return f"chat:history:{user_id}"

    async def append(self, user_id: str, role: str, content: str, now_ms: int) -> None:
        """메시지를 timestamp 점수로 추가하고, 유휴 정리를 위해 TTL 을 갱신한다."""
        key = self._key(user_id)
        member = json.dumps({"role": role, "content": content, "ts": now_ms}, ensure_ascii=False)
        await self._redis.zadd(key, {member: now_ms})
        await self._redis.expire(key, self._window)

    async def recent(self, user_id: str, now_ms: int) -> list[dict]:
        """1시간 지난 메시지를 오래된 순으로 제거한 뒤, 남은 대화를 시간순으로 반환한다."""
        key = self._key(user_id)
        cutoff = now_ms - self._window * 1000
        await self._redis.zremrangebyscore(key, 0, cutoff)
        members = await self._redis.zrange(key, 0, -1)
        history: list[dict] = []
        for raw in members:
            data = json.loads(raw)
            history.append({"role": data["role"], "content": data["content"]})
        return history
