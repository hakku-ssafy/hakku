"""Storage-server HTTP client.

Uploads result images to the Go storage-server as kind=result,
returns metadata including the image ID and URL.
"""

import httpx
from app.config import settings


async def upload_result_image(image_bytes: bytes) -> dict:
    """Upload image to storage-server with kind=result.

    Returns:
        {"id": str, "url": str, ...} metadata from storage-server.
    """
    url = f"{settings.storage_server_url}/storage/images"
    async with httpx.AsyncClient() as client:
        r = await client.post(
            url,
            params={"kind": "result"},
            content=image_bytes,
            headers={"Content-Type": "image/png"},
            timeout=30.0,
        )
        r.raise_for_status()
        meta = r.json()

    image_id = meta["id"]
    meta["url"] = f"{settings.storage_public_url.rstrip('/')}/images/{image_id}"
    return meta
