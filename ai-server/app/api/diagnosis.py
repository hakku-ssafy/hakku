"""Personal color diagnosis endpoint.

POST /api/diagnosis — multipart upload pipeline:
  preprocess → OpenAI image generation → storage → vision OCR → main-server update
"""

from fastapi import APIRouter, Depends, File, HTTPException, Request, UploadFile
from pydantic import BaseModel

from app.services import generator, storage, vision, main_client
from app.services.preprocess import resize_for_api

router = APIRouter()

_ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp", "image/gif"}


class DiagnosisResponse(BaseModel):
    personalColorType: str
    resultImageUrl: str


def _require_auth(request: Request) -> str:
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="인증이 필요합니다.")
    return auth


@router.post("/api/diagnosis", response_model=DiagnosisResponse)
async def diagnose(
    image: UploadFile = File(...),
    authorization: str = Depends(_require_auth),
):
    if image.content_type not in _ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"지원하지 않는 이미지 형식입니다: {image.content_type}",
        )

    raw_bytes = await image.read()

    # 1. Preprocess — resize per portrait/landscape rule
    processed = resize_for_api(raw_bytes)

    # 2. Generate analysis image via OpenAI gpt-image-1 (original photo discarded)
    result_image = await generator.generate_analysis_image(processed)

    # 3. Store result image in storage-server (kind=result)
    meta = await storage.upload_result_image(result_image)
    result_url = meta["url"]

    # 4. Extract personal color type via OpenAI Vision
    color_type = await vision.extract_color_from_image(result_image)
    if color_type is None:
        raise HTTPException(
            status_code=422,
            detail="퍼스널컬러 타입을 인식하지 못했습니다. 다시 시도해 주세요.",
        )

    # 5. Update user profile in main-server
    await main_client.update_user_diagnosis(authorization, color_type, result_url)

    return DiagnosisResponse(personalColorType=color_type, resultImageUrl=result_url)
