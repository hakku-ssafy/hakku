"""대화 기억용 비동기 Redis 클라이언트(프로세스 단위 싱글톤).

decode_responses=True 로 zrange 가 str 멤버를 돌려주도록 한다(ConversationStore 가 json.loads).
"""
from redis.asyncio import Redis

from app.config import settings

_redis: Redis | None = None


def get_redis() -> Redis:
    global _redis
    if _redis is None:
        _redis = Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            decode_responses=True,
        )
    return _redis
