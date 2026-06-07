"""OpenAI Image API wrapper for personal color analysis image generation.

Sends only the user's (preprocessed) photo to gpt-image-2 with a detailed
Korean personal-color draping prompt. No fixed design template is used —
the dashboard layout is produced from the prompt alone so the diagnosis is
driven by the actual person, not a template.

See: https://developers.openai.com/api/docs/guides/image-generation
"""

import base64

from openai import AsyncOpenAI

from app.config import settings

_PROMPT = """업로드된 인물 사진을 기반으로 퍼스널 컬러 드레이핑 분석 대시보드를 생성해 줘. 모든 텍스트는 한국어로 작성하고, 결과는 9:16 비율의 하나의 이미지로 만들어 줘.

- 얼굴 중심으로 피부 톤(밝기, 채도, 언더톤), 눈동자, 머리색, 대비감 분석
- 조명 영향을 고려해 실제 컬러 기준으로 판단
- 얼굴 및 원본 이미지는 절대 변경하지 않기

[구성]
1. 상단
- "퍼스널 컬러 분석 리포트"
- 웜톤 / 쿨톤 / 뉴트럴 요약
- 세부 타입 (예: 봄 웜 라이트, 겨울 쿨 딥)

2. 중앙 (핵심)
- 얼굴을 중심에 배치
- 4x2 컬러(총 8개)를 얼굴과 직접 비교되도록 구성
• 각 색상을 배경 또는 천처럼 얼굴 뒤에 적용
• 또는 얼굴 + 색상 미니 비교 프레임으로 구성
- 색상에 따라 얼굴이 밝아 보이거나 칙칙해 보이는 차이를 명확히 표현

3. 분석
- 피부 톤 / 눈동자 / 헤어 / 전체 인상
- 컬러 비교 기반으로 어울림 vs 안어울림 설명

4. 하단
- [추천 컬러]
- [피해야 할 컬러] (이유 포함)
- [스타일링 제안]

[스타일]
- 실제 퍼스널컬러 진단 느낌
- 깔끔하고 프리미엄한 대시보드 디자인
- 자연스러운 피부 표현, 과한 효과 금지"""

# 9:16 portrait — both edges are multiples of 16 (gpt-image-2 constraint)
_OUTPUT_SIZE = "1024x1824"


async def generate_analysis_image(user_image_bytes: bytes) -> bytes:
    """Generate a personal color analysis dashboard image from the user photo only."""
    client = AsyncOpenAI(api_key=settings.openai_api_key)

    response = await client.images.edit(
        model="gpt-image-2",
        image=[("user.jpg", user_image_bytes, "image/jpeg")],
        prompt=_PROMPT,
        size=_OUTPUT_SIZE,
        n=1,
    )

    return base64.b64decode(response.data[0].b64_json)
