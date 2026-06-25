"""대화 기억 복원: 새로고침/재접속 시 프론트가 서버의 1시간 대화 기억을 받아오기 위한 로직.

테스트 venv 에는 fastapi 가 없으므로(서비스 계층만 단위 테스트) 라우트 대신 서비스 함수
chat_service.load_recent_history 를 검증한다. GET /history 라우트는 이 함수를 감싸는 얇은 어댑터다.
"""
from app.services import chat_service
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


async def test_load_recent_history_returns_messages_oldest_first():
    store = ConversationStore(FakeRedis(), window_seconds=3600)
    base = 1_000_000_000_000
    await store.append("u1", "user", "안녕", base)
    await store.append("u1", "assistant", "안녕하세요!", base + 1)

    result = await chat_service.load_recent_history(store, "u1", base + 2)

    assert result == {
        "messages": [
            {"role": "user", "content": "안녕"},
            {"role": "assistant", "content": "안녕하세요!"},
        ]
    }


async def test_load_recent_history_empty_when_no_conversation():
    store = ConversationStore(FakeRedis(), window_seconds=3600)

    result = await chat_service.load_recent_history(store, "ghost", 1000)

    assert result == {"messages": []}
