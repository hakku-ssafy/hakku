"""main-server HTTP client.

Calls PATCH /api/users/me/personal-color on behalf of the authenticated user,
forwarding their JWT so Spring Security resolves the user identity.
"""

import httpx
from app.config import settings


async def update_user_diagnosis(
    authorization: str,
    personal_color_type: str,
    result_image_url: str,
) -> None:
    """Update user's personal color and result image URL in main-server.

    Args:
        authorization: Full 'Bearer <token>' header value from the request.
        personal_color_type: PersonalColorType enum name, e.g. 'LIGHT_SUMMER'.
        result_image_url: Public URL of the stored result image.
    """
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
