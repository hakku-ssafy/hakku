"""stream_chat 오케스트레이션: 히스토리 주입 → (필요시) 도구 호출 → 스트리밍 + 기억 저장."""
from types import SimpleNamespace

from app.services import chat_service


# ── Fake OpenAI 스트리밍 객체 ────────────────────────────────────────────────
def _tool_call(index, call_id, name, arguments):
    return SimpleNamespace(
        index=index, id=call_id, type="function",
        function=SimpleNamespace(name=name, arguments=arguments),
    )


def _chunk(content=None, tool_calls=None, finish_reason=None):
    delta = SimpleNamespace(content=content, tool_calls=tool_calls)
    return SimpleNamespace(choices=[SimpleNamespace(delta=delta, finish_reason=finish_reason)])


async def _aiter(chunks):
    for c in chunks:
        yield c


class FakeCompletions:
    def __init__(self, scripted):
        self._scripted = scripted
        self.calls = []

    async def create(self, **kwargs):
        self.calls.append(kwargs)
        return _aiter(self._scripted[len(self.calls) - 1])


class FakeOpenAI:
    def __init__(self, scripted):
        self.chat = SimpleNamespace(completions=FakeCompletions(scripted))


class FakeStore:
    def __init__(self, history=None):
        self._history = history or []
        self.appended = []

    async def recent(self, user_id, now_ms):
        return list(self._history)

    async def append(self, user_id, role, content, now_ms):
        self.appended.append({"role": role, "content": content})


class FakeMainClient:
    def __init__(self):
        self.orders_called = False

    async def get_orders(self):
        self.orders_called = True
        return [{"id": 1, "status": "PAID", "totalAmount": 9800, "items": []}]

    async def get_wishlist(self, user_id):
        return []

    async def get_recommendations(self):
        return []


async def _collect(agen):
    return [c async for c in agen]


async def test_streams_content_and_persists_conversation():
    fake = FakeOpenAI([[
        _chunk(content="안녕"),
        _chunk(content="하세요", finish_reason="stop"),
    ]])
    store = FakeStore()
    out = "".join(await _collect(chat_service.stream_chat(
        "안녕", None, "image/jpeg",
        user_id="1", store=store, main_client=FakeMainClient(),
        now_ms=1000, openai_client=fake, model="gpt-4o",
    )))
    assert "안녕" in out and "하세요" in out
    assert out.strip().endswith("[DONE]")
    assert [m["role"] for m in store.appended] == ["user", "assistant"]
    assert store.appended[1]["content"] == "안녕하세요"


async def test_executes_tool_then_streams_final_answer():
    fake = FakeOpenAI([
        [
            _chunk(tool_calls=[_tool_call(0, "call_1", "get_order_history", "{}")]),
            _chunk(finish_reason="tool_calls"),
        ],
        [_chunk(content="주문 1건 있어요", finish_reason="stop")],
    ])
    store = FakeStore()
    main_client = FakeMainClient()
    out = "".join(await _collect(chat_service.stream_chat(
        "내 주문 알려줘", None, "image/jpeg",
        user_id="1", store=store, main_client=main_client,
        now_ms=1000, openai_client=fake, model="gpt-4o",
    )))
    assert main_client.orders_called
    assert "주문 1건 있어요" in out
    second_messages = fake.chat.completions.calls[1]["messages"]
    assert any(m.get("role") == "tool" for m in second_messages)


async def test_includes_prior_history_in_prompt():
    fake = FakeOpenAI([[_chunk(content="네", finish_reason="stop")]])
    store = FakeStore(history=[
        {"role": "user", "content": "이전 질문"},
        {"role": "assistant", "content": "이전 답변"},
    ])
    await _collect(chat_service.stream_chat(
        "또 질문", None, "image/jpeg",
        user_id="1", store=store, main_client=FakeMainClient(),
        now_ms=1, openai_client=fake, model="gpt-4o",
    ))
    messages = fake.chat.completions.calls[0]["messages"]
    assert any(m.get("content") == "이전 질문" for m in messages)
    assert any(m.get("content") == "이전 답변" for m in messages)
