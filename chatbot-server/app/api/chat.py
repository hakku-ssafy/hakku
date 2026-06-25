import time
from typing import Optional

from fastapi import APIRouter, Depends, File, Form, UploadFile
from fastapi.responses import StreamingResponse

from app.auth import AuthContext, get_auth_context
from app.config import settings
from app.redis_client import get_redis
from app.services.chat_service import load_recent_history, stream_chat
from app.services.conversation import ConversationStore
from app.services.main_server import MainServerClient

router = APIRouter(tags=["chat"])


@router.post("/stream")
async def chat_stream(
    message: str = Form(...),
    image: Optional[UploadFile] = File(None),
    auth: AuthContext = Depends(get_auth_context),
) -> StreamingResponse:
    image_bytes: Optional[bytes] = None
    media_type = "image/jpeg"

    if image:
        image_bytes = await image.read()
        media_type = image.content_type or "image/jpeg"

    store = ConversationStore(get_redis())
    main_client = MainServerClient(settings.main_server_url, auth.token)
    now_ms = int(time.time() * 1000)

    return StreamingResponse(
        stream_chat(
            message,
            image_bytes,
            media_type,
            user_id=auth.user_id,
            store=store,
            main_client=main_client,
            now_ms=now_ms,
        ),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


@router.get("/history")
async def chat_history(auth: AuthContext = Depends(get_auth_context)) -> dict:
    """새로고침/재접속 시 프론트가 대화를 복원하도록 최근(1시간) 대화 기억을 반환한다."""
    store = ConversationStore(get_redis())
    now_ms = int(time.time() * 1000)
    return await load_recent_history(store, auth.user_id, now_ms)
