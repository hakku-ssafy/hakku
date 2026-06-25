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

# _PROMPT = """업로드된 인물 사진을 기반으로 퍼스널 컬러 드레이핑 분석 대시보드를 생성해 줘. 모든 텍스트는 한국어로 작성하고, 결과는 9:16 비율의 하나의 이미지로 만들어 줘.

# - 얼굴 중심으로 피부 톤(밝기, 채도, 언더톤), 눈동자, 머리색, 대비감 분석
# - 조명 영향을 고려해 실제 컬러 기준으로 판단
# - 얼굴 및 원본 이미지는 절대 변경하지 않기

# [구성]
# 1. 상단
# - "퍼스널 컬러 분석 리포트"
# - 웜톤 / 쿨톤 / 뉴트럴 요약
# - 세부 타입 (예: 봄 웜 라이트, 겨울 쿨 딥)

# 2. 중앙 (핵심)
# - 얼굴을 중심에 배치
# - 4x2 컬러(총 8개)를 얼굴과 직접 비교되도록 구성
# • 각 색상을 배경 또는 천처럼 얼굴 뒤에 적용
# • 또는 얼굴 + 색상 미니 비교 프레임으로 구성
# - 색상에 따라 얼굴이 밝아 보이거나 칙칙해 보이는 차이를 명확히 표현

# 3. 분석
# - 피부 톤 / 눈동자 / 헤어 / 전체 인상
# - 컬러 비교 기반으로 어울림 vs 안어울림 설명

# 4. 하단
# - [추천 컬러]
# - [피해야 할 컬러] (이유 포함)
# - [스타일링 제안]

# [스타일]
# - 실제 퍼스널컬러 진단 느낌
# - 깔끔하고 프리미엄한 대시보드 디자인
# - 자연스러운 피부 표현, 과한 효과 금지"""

_PROMPT = """
퍼스널 컬러 드레이핑 분석 리포트 생성

업로드된 인물 사진을 기반으로 실제 퍼스널컬러 드레이핑 분석 리포트를 생성한다.

결과는 9:16 비율의 하나의 이미지로 제작한다.

모든 텍스트는 한국어로 작성한다.

⸻

가장 중요한 원칙

퍼스널컬러를 먼저 추론하지 않는다.

반드시 아래 순서를 따른다.

① 얼굴 특성 분석

↓

② 다양한 컬러 드레이핑 비교

↓

③ 드레이핑 결과를 근거로 최종 퍼스널컬러 결정

최종 타입을 먼저 정해놓고 그에 맞는 결과를 생성해서는 안 된다.

⸻

얼굴 분석

업로드된 사진을 기반으로 아래 요소를 각각 독립적으로 분석한다.

* 피부 밝기 (Light / Medium / Deep)
* 피부 언더톤 (Warm / Cool / Neutral)
* 피부 채도 (Soft / Clear)
* 얼굴 대비감 (Low / Medium / High)
* 눈동자 명도
* 눈동자 채도
* 헤어 명도
* 헤어 채도

조명의 영향을 받는 경우에는 과도하게 보정하지 말고, 확신이 부족하면 Neutral에 가깝게 판단한다.

얼굴이나 피부색을 임의로 수정하지 않는다.

원본 얼굴은 절대 변경하지 않는다.

⸻

드레이핑 비교 (가장 중요)

아래 8가지 대표 컬러를 얼굴에 각각 적용한다.

* Warm Ivory
* Peach Coral
* Camel
* Olive
* Cool Pink
* Lavender
* Royal Blue
* Charcoal

각 컬러는 얼굴 뒤의 천 또는 배경 형태로 자연스럽게 적용한다.

얼굴은 동일한 위치와 크기를 유지한다.

드레이핑마다 아래 항목을 비교한다.

* 피부가 맑아 보이는가
* 피부가 칙칙해지는가
* 홍조가 심해지는가
* 다크서클이 강조되는가
* 턱선 그림자가 진해지는가
* 입술 혈색이 살아나는가
* 눈동자가 선명해지는가
* 얼굴 윤곽이 또렷해지는가

드레이핑 결과를 기반으로 가장 잘 어울리는 계열을 선택한다.

⸻

퍼스널컬러 결정

드레이핑 결과와 얼굴 분석을 함께 사용하여 아래 16가지 타입 중 하나만 선택한다.

봄

* 봄 라이트
* 봄 브라이트
* 봄 트루
* 봄 웜

여름

* 여름 라이트
* 여름 뮤트
* 여름 트루
* 여름 쿨

가을

* 가을 소프트
* 가을 딥
* 가을 트루
* 가을 웜

겨울

* 겨울 브라이트
* 겨울 딥
* 겨울 트루
* 겨울 쿨

특정 타입을 선호하지 않는다.

입력 이미지의 특징에 따라 가장 근거가 높은 타입을 선택한다.

확신이 부족하면 뉴트럴 성향이 있는 타입을 선택한다.

⸻

결과 레이아웃

상단

퍼스널 컬러 분석 리포트

Warm / Cool / Neutral

최종 퍼스널컬러

판단 신뢰도
(높음 / 보통 / 낮음)

⸻

중앙

원본 얼굴

4×2 드레이핑 비교

각 드레이핑 아래에는

“잘 어울림”

“보통”

“어울리지 않음”

정도를 표시한다.

⸻

분석

아래 항목을 카드 형태로 정리한다.

* 피부톤
* 언더톤
* 눈동자
* 헤어
* 대비감

그리고

최종 타입을 선택한 이유를 아래 항목별로 설명한다.

* 언더톤 근거
* 명도 근거
* 채도 근거
* 대비감 근거
* 드레이핑 비교 결과

⸻

하단

추천 컬러

추천 메이크업

추천 헤어컬러

추천 액세서리

피해야 할 컬러

스타일링 팁

⸻

디자인

실제 퍼스널컬러 전문 컨설팅 리포트처럼 구성한다.

프리미엄하고 미니멀한 디자인을 사용한다.

불필요한 장식은 넣지 않는다.

가독성을 높인다.

얼굴은 자연스럽게 유지한다.

원본 얼굴을 절대 수정하거나 미화하지 않는다.

드레이핑은 얼굴을 비교하기 위한 참고 자료로 자연스럽게 표현한다.
"""

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
