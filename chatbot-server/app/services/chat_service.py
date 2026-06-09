import base64
import json
from typing import AsyncIterator, Optional

from openai import AsyncOpenAI

from app.config import settings

client = AsyncOpenAI(api_key=settings.openai_api_key)

_SYSTEM_PROMPT = """당신은 학꾸(Hakku) 서비스의 학생증 꾸미기 도우미입니다.
사용자의 학생증 이미지를 보고 어떻게 꾸미면 좋을지 친절하고 구체적으로 조언해주세요.

- 여백, 색감, 사진 구도 등 현재 상태를 분석해주세요
- 스티커, 마스킹 테이프, 포토카드 등 구체적인 꾸미기 방법을 추천해주세요
- 한국어로 자연스럽고 친근하게 답변해주세요"""


async def stream_chat(
    message: str,
    image_bytes: Optional[bytes] = None,
    image_media_type: str = "image/jpeg",
) -> AsyncIterator[str]:
    content: list = []

    if image_bytes:
        b64 = base64.b64encode(image_bytes).decode()
        content.append({
            "type": "image_url",
            "image_url": {"url": f"data:{image_media_type};base64,{b64}"},
        })

    content.append({"type": "text", "text": message})

    stream = await client.chat.completions.create(
        model=settings.openai_model,
        messages=[
            {"role": "system", "content": _SYSTEM_PROMPT},
            {"role": "user", "content": content},
        ],
        stream=True,
    )

    async for chunk in stream:
        delta = chunk.choices[0].delta.content
        if delta:
            yield f"data: {json.dumps({'text': delta}, ensure_ascii=False)}\n\n"

    yield "data: [DONE]\n\n"
