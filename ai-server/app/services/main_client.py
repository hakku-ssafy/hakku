"""main-server HTTP client.

Provides three operations:
- request_diagnosis_start: lock the slot (NONE → PENDING), 409 if already requested
- update_user_diagnosis: set result and COMPLETED state + send notification
- reset_diagnosis_status: unlock on failure (PENDING → NONE)
"""

import httpx
from app.config import settings


async def request_diagnosis_start(authorization: str) -> None:
    """Lock the diagnosis slot in main-server (NONE → PENDING).

    Raises:
        httpx.HTTPStatusError: 409 if already requested/completed.
    """
    url = f"{settings.main_server_url}/api/users/me/diagnosis-request"
    async with httpx.AsyncClient() as client:
        r = await client.post(url, headers={"Authorization": authorization}, timeout=10.0)
        r.raise_for_status()


async def update_user_diagnosis(
    authorization: str,
    personal_color_type: str,
    result_image_url: str,
) -> None:
    """Update user's personal color and result image URL in main-server (PENDING → COMPLETED)."""
    url = f"{settings.main_server_url}/api/users/me/personal-color"
    async with httpx.AsyncClient() as client:
        r = await client.patch(
            url,
            json={
                "personalColor": personal_color_type,
                "resultImageUrl": result_image_url,
            },
            headers={"Authorization": authorization},
            timeout=10.0,
        )
        r.raise_for_status()


async def reset_diagnosis_status(authorization: str) -> None:
    """Reset diagnosis status to NONE on processing failure."""
    url = f"{settings.main_server_url}/api/users/me/diagnosis-request"
    async with httpx.AsyncClient() as client:
        r = await client.delete(url, headers={"Authorization": authorization}, timeout=10.0)
        r.raise_for_status()
