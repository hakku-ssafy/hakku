from typing import Optional

from fastapi import APIRouter, Depends, File, Form, UploadFile
from fastapi.responses import StreamingResponse

from app.auth import get_current_user_id
from app.services.chat_service import stream_chat

router = APIRouter(tags=["chat"])


@router.post("/stream")
async def chat_stream(
    message: str = Form(...),
    image: Optional[UploadFile] = File(None),
    user_id: str = Depends(get_current_user_id),
) -> StreamingResponse:
    image_bytes: Optional[bytes] = None
    media_type = "image/jpeg"

    if image:
        image_bytes = await image.read()
        media_type = image.content_type or "image/jpeg"

    return StreamingResponse(
        stream_chat(message, image_bytes, media_type),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )
