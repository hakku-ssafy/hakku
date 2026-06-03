"""OpenAI Vision service for extracting personal color type from result image.

Uses gpt-5.4-mini vision to read the generated dashboard image and extract the
personal color type text, then delegates to ocr.extract_personal_color_type().
"""

import base64
from openai import AsyncOpenAI

from app.config import settings
from app.services.ocr import extract_personal_color_type


_EXTRACT_PROMPT = (
    "이 이미지에서 퍼스널 컬러 진단 결과 텍스트를 찾아 그대로 추출해줘. "
    "예: '여름 쿨 라이트 (Summer Cool Light)'. 진단 결과 텍스트만 출력해."
)


async def extract_color_from_image(image_bytes: bytes) -> str | None:
    """Extract PersonalColorType enum name from the generated result image.

    Args:
        image_bytes: Generated result image bytes.

    Returns:
        PersonalColorType name like 'LIGHT_SUMMER', or None if unrecognised.
    """
    client = AsyncOpenAI(api_key=settings.openai_api_key)
    b64 = base64.b64encode(image_bytes).decode()

    response = await client.chat.completions.create(
        model="gpt-5.4-mini",
        messages=[
            {
                "role": "user",
                "content": [
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:image/png;base64,{b64}"},
                    },
                    {"type": "text", "text": _EXTRACT_PROMPT},
                ],
            }
        ],
        max_tokens=100,
        temperature=0,
    )

    raw_text = response.choices[0].message.content or ""
    return extract_personal_color_type(raw_text)
