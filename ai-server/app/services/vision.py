"""OpenAI Vision service for extracting personal color type from result image.

Uses the Responses API (recommended for vision) with gpt-5.4-mini to read the
generated dashboard image and extract the personal color type text, then
delegates to ocr.extract_personal_color_type().

See: https://developers.openai.com/api/docs/guides/vision
"""

import base64

from openai import AsyncOpenAI

from app.config import settings
from app.services.ocr import extract_personal_color_type

_EXTRACT_PROMPT = (
    "이 이미지는 퍼스널 컬러 분석 대시보드입니다. "
    "상단의 '세부 타입' 라벨 한 줄을 그대로 추출해줘. "
    "형식 예: '세부 타입: 가을 소프트 (SOFT_AUTUMN)' 또는 '세부 타입: 여름 쿨 라이트 (LIGHT_SUMMER)'. "
    "괄호 안 ENUM 코드(SOFT_AUTUMN 등)가 있으면 반드시 포함해서 출력해. "
    "해당 라인만 출력해."
)


async def extract_color_from_image(image_bytes: bytes) -> str | None:
    """Extract PersonalColorType enum name from the generated result image."""
    client = AsyncOpenAI(api_key=settings.openai_api_key)
    b64 = base64.b64encode(image_bytes).decode()

    response = await client.responses.create(
        model="gpt-5.4-mini",
        reasoning={"effort": "low"},
        input=[
            {
                "role": "user",
                "content": [
                    {
                        "type": "input_image",
                        "image_url": f"data:image/png;base64,{b64}",
                        "detail": "high",
                    },
                    {"type": "input_text", "text": _EXTRACT_PROMPT},
                ],
            }
        ],
        max_output_tokens=512,
    )

    raw_text = response.output_text or ""
    return extract_personal_color_type(raw_text)
