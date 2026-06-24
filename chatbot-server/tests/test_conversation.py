"""ConversationStore: 1시간 슬라이딩 윈도우 대화 기억(오래된 순 삭제)."""
from app.services.conversation import ConversationStore


class FakeRedis:
    def __init__(self):
        self.sets: dict[str, dict[str, float]] = {}
        self.expires: dict[str, int] = {}

    async def zadd(self, key, mapping):
        self.sets.setdefault(key, {}).update(mapping)

    async def expire(self, key, seconds):
        self.expires[key] = seconds

    async def zremrangebyscore(self, key, min_score, max_score):
        members = self.sets.get(key, {})
        for m in [m for m, s in members.items() if min_score <= s <= max_score]:
            del members[m]

    async def zrange(self, key, start, stop):
        members = self.sets.get(key, {})
        ordered = [m for m, _ in sorted(members.items(), key=lambda kv: kv[1])]
        return ordered[start:] if stop == -1 else ordered[start : stop + 1]


async def test_append_and_recent_returns_oldest_first():
    store = ConversationStore(FakeRedis(), window_seconds=3600)
    base = 1_000_000_000_000
    await store.append("u1", "user", "안녕", base)
    await store.append("u1", "assistant", "안녕하세요!", base + 1)

    assert await store.recent("u1", base + 2) == [
        {"role": "user", "content": "안녕"},
        {"role": "assistant", "content": "안녕하세요!"},
    ]


async def test_recent_prunes_messages_older_than_one_hour():
    store = ConversationStore(FakeRedis(), window_seconds=3600)
    now = 10_000_000_000_000
    await store.append("u1", "user", "옛날 메시지", now - 3_600_000 - 1000)
    await store.append("u1", "user", "최근 메시지", now - 1000)

    assert await store.recent("u1", now) == [{"role": "user", "content": "최근 메시지"}]


async def test_append_sets_ttl_for_idle_cleanup():
    redis = FakeRedis()
    store = ConversationStore(redis, window_seconds=3600)
    await store.append("u1", "user", "hi", 1000)
    assert redis.expires["chat:history:u1"] == 3600
