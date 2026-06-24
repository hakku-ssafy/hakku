"""챗봇 오케스트레이션.

1) Redis 대화 기억(최근 1시간)을 프롬프트에 주입하고
2) 필요하면 고객센터 도구(주문/찜/추천)를 function-calling 으로 호출한 뒤
3) 최종 답변을 SSE 로 스트리밍하고, 사용자/어시스턴트 메시지를 기억에 저장한다.

마크다운 렌더링은 프론트가 담당하므로 시스템 프롬프트로 마크다운 사용을 유도한다.
무거운 의존성(openai/settings)은 함수 내부에서 지연 임포트해 단위 테스트가 가볍다.
"""
import base64
import json
import logging
from typing import AsyncIterator, Optional

from app.services.tools import TOOLS, execute_tool

logger = logging.getLogger(__name__)

MAX_TOOL_ROUNDS = 3

SYSTEM_PROMPT = """당신은 학꾸(Hakku) 서비스의 AI 도우미이자 고객센터입니다. 한국어로 친근하고 명확하게 답합니다.

역할
- 학생증 꾸미기 조언: 이미지가 있으면 여백·색감·구도를 분석하고 스티커·마스킹테이프·포토카드 등 구체적 방법을 추천합니다.
- 고객센터: 사용자의 주문 내역, 찜한 상품, 맞춤 상품 추천을 도와줍니다.

도구 사용
- 주문/배송/구매 이력 → get_order_history
- 찜/위시리스트 → get_wishlist
- 상품 추천 → recommend_products
- 사용자 데이터가 필요한 질문에는 반드시 도구로 최신 정보를 조회한 뒤 답하세요. 추측하지 마세요.

답변 형식
- 마크다운으로 답하세요(목록, 굵게, 링크 적극 활용).
- 상품을 안내할 때는 반드시 마크다운 링크 `[상품명](/products/{상품id})` 형태로 바로 이동할 수 있게 합니다.
- 정보가 없으면 솔직히 없다고 안내합니다."""


def _build_user_content(message: str, image_bytes: Optional[bytes], image_media_type: str) -> list:
    content: list = []
    if image_bytes:
        b64 = base64.b64encode(image_bytes).decode()
        content.append({
            "type": "image_url",
            "image_url": {"url": f"data:{image_media_type};base64,{b64}"},
        })
    content.append({"type": "text", "text": message})
    return content


def _sse(payload: dict) -> str:
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"


def _default_client():
    from openai import AsyncOpenAI

    from app.config import settings

    return AsyncOpenAI(api_key=settings.openai_api_key)


def _default_model() -> str:
    from app.config import settings

    return settings.openai_model


async def stream_chat(
    message: str,
    image_bytes: Optional[bytes] = None,
    image_media_type: str = "image/jpeg",
    *,
    user_id: str,
    store,
    main_client,
    now_ms: int,
    openai_client=None,
    model: Optional[str] = None,
) -> AsyncIterator[str]:
    if openai_client is None:
        openai_client = _default_client()
    if model is None:
        model = _default_model()

    history = await store.recent(user_id, now_ms)
    messages: list = [{"role": "system", "content": SYSTEM_PROMPT}]
    messages.extend({"role": m["role"], "content": m["content"]} for m in history)
    messages.append({"role": "user", "content": _build_user_content(message, image_bytes, image_media_type)})

    assistant_text = ""
    try:
        for _ in range(MAX_TOOL_ROUNDS):
            stream = await openai_client.chat.completions.create(
                model=model,
                messages=messages,
                tools=TOOLS,
                stream=True,
            )

            tool_buf: dict[int, dict] = {}
            finish_reason: Optional[str] = None

            async for chunk in stream:
                choice = chunk.choices[0]
                delta = choice.delta

                tool_calls = getattr(delta, "tool_calls", None)
                if tool_calls:
                    for tc in tool_calls:
                        buf = tool_buf.setdefault(tc.index, {"id": "", "name": "", "args": ""})
                        if getattr(tc, "id", None):
                            buf["id"] = tc.id
                        fn = getattr(tc, "function", None)
                        if fn is not None:
                            if getattr(fn, "name", None):
                                buf["name"] = fn.name
                            if getattr(fn, "arguments", None):
                                buf["args"] += fn.arguments

                content = getattr(delta, "content", None)
                if content:
                    assistant_text += content
                    yield _sse({"text": content})

                if choice.finish_reason:
                    finish_reason = choice.finish_reason

            if tool_buf and finish_reason == "tool_calls":
                ordered = [tool_buf[i] for i in sorted(tool_buf)]
                for idx, buf in enumerate(ordered):
                    if not buf["id"]:
                        buf["id"] = f"call_{idx}"
                messages.append({
                    "role": "assistant",
                    "content": None,
                    "tool_calls": [
                        {
                            "id": buf["id"],
                            "type": "function",
                            "function": {"name": buf["name"], "arguments": buf["args"] or "{}"},
                        }
                        for buf in ordered
                    ],
                })
                for buf in ordered:
                    result = await execute_tool(buf["name"], main_client, user_id)
                    messages.append({"role": "tool", "tool_call_id": buf["id"], "content": result})
                continue

            break
    except Exception:  # 스트리밍 경계 가드: 사용자에게 친절한 오류만 노출
        logger.exception("챗봇 응답 생성 실패")
        yield _sse({"error": "AI 응답 생성에 실패했어요. 잠시 후 다시 시도해주세요."})
        yield "data: [DONE]\n\n"
        return

    # 대화 기억 저장(1시간 슬라이딩 윈도우). 어시스턴트는 사용자보다 뒤 순서로.
    await store.append(user_id, "user", message, now_ms)
    if assistant_text:
        await store.append(user_id, "assistant", assistant_text, now_ms + 1)

    yield "data: [DONE]\n\n"
