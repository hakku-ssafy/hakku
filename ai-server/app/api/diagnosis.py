"""Personal color diagnosis endpoint.

POST /api/diagnosis — accepts image, locks slot immediately (202), processes in background:
  preprocess → OpenAI image generation → storage → vision OCR → main-server update + notification

On error, resets diagnosis status to NONE so the user can retry.
"""

import logging

import httpx
from fastapi import APIRouter, BackgroundTasks, Depends, File, HTTPException, Request, UploadFile
from pydantic import BaseModel

from app.services import generator, storage, vision, main_client
from app.services.preprocess import resize_for_api

logger = logging.getLogger(__name__)

router = APIRouter()

_ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp", "image/gif"}


class DiagnosisAcceptedResponse(BaseModel):
    message: str


def _require_auth(request: Request) -> str:
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="인증이 필요합니다.")
    return auth


async def _run_pipeline(authorization: str, raw_bytes: bytes) -> None:
    """Background task: run the full diagnosis pipeline."""
    try:
        processed = resize_for_api(raw_bytes)
        result_image = await generator.generate_analysis_image(processed)
        meta = await storage.upload_result_image(result_image, authorization)
        result_url = meta["url"]
        color_type = await vision.extract_color_from_image(result_image)
        if color_type is None:
            logger.error("퍼스널컬러 인식 실패 — 진단 상태 초기화")
            await main_client.reset_diagnosis_status(authorization)
            return
        await main_client.update_user_diagnosis(authorization, color_type, result_url)
    except Exception:
        logger.exception("진단 파이프라인 실패 — 진단 상태 초기화")
        try:
            await main_client.reset_diagnosis_status(authorization)
        except Exception:
            logger.exception("진단 상태 초기화 실패")


@router.post("/api/diagnosis", status_code=202, response_model=DiagnosisAcceptedResponse)
async def diagnose(
    background_tasks: BackgroundTasks,
    image: UploadFile = File(...),
    authorization: str = Depends(_require_auth),
):
    if image.content_type not in _ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"지원하지 않는 이미지 형식입니다: {image.content_type}",
        )

    raw_bytes = await image.read()

    # Lock the slot — returns 409 if already requested or completed
    try:
        await main_client.request_diagnosis_start(authorization)
    except httpx.HTTPStatusError as exc:
        if exc.response.status_code == 409:
            raise HTTPException(status_code=409, detail="이미 진단 요청이 진행 중이거나 완료되었습니다.")
        raise HTTPException(status_code=502, detail="서버 연결에 실패했습니다. 잠시 후 다시 시도해 주세요.")
    except Exception:
        raise HTTPException(status_code=502, detail="서버 연결에 실패했습니다. 잠시 후 다시 시도해 주세요.")

    background_tasks.add_task(_run_pipeline, authorization, raw_bytes)

    return DiagnosisAcceptedResponse(message="진단 요청이 접수되었습니다. 완료되면 알림을 보내드립니다.")
