"""Unit tests for storage and main_client services using httpx mock."""

import pytest
import httpx
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.storage import upload_result_image
from app.services.main_client import (
    update_user_diagnosis,
    request_diagnosis_start,
    reset_diagnosis_status,
)


class TestUploadResultImage:
    async def test_uploads_with_kind_result(self):
        mock_response = MagicMock()
        mock_response.json.return_value = {"id": "abc123", "contentType": "image/png", "size": 99}
        mock_response.raise_for_status = MagicMock()

        with patch("app.services.storage.httpx.AsyncClient") as mock_client_cls:
            mock_client = AsyncMock()
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client.post = AsyncMock(return_value=mock_response)
            mock_client_cls.return_value = mock_client

            result = await upload_result_image(b"fake-image", "Bearer test-token")

        mock_client.post.assert_called_once()
        call_kwargs = mock_client.post.call_args
        assert call_kwargs.kwargs["params"] == {"kind": "result"}
        assert call_kwargs.kwargs["content"] == b"fake-image"
        assert call_kwargs.kwargs["headers"]["Authorization"] == "Bearer test-token"
        assert result["id"] == "abc123"
        assert "url" in result
        assert "abc123" in result["url"]

    async def test_raises_on_server_error(self):
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = httpx.HTTPStatusError(
            "500", request=MagicMock(), response=MagicMock()
        )

        with patch("app.services.storage.httpx.AsyncClient") as mock_client_cls:
            mock_client = AsyncMock()
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client.post = AsyncMock(return_value=mock_response)
            mock_client_cls.return_value = mock_client

            with pytest.raises(httpx.HTTPStatusError):
                await upload_result_image(b"fake-image", "Bearer test-token")


class TestUpdateUserDiagnosis:
    async def test_patches_main_server(self):
        mock_response = MagicMock()
        mock_response.raise_for_status = MagicMock()

        with patch("app.services.main_client.httpx.AsyncClient") as mock_client_cls:
            mock_client = AsyncMock()
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client.patch = AsyncMock(return_value=mock_response)
            mock_client_cls.return_value = mock_client

            await update_user_diagnosis(
                "Bearer token123",
                "LIGHT_SUMMER",
                "http://storage/result/abc.png",
            )

        mock_client.patch.assert_called_once()
        call_kwargs = mock_client.patch.call_args
        assert call_kwargs.kwargs["json"]["personalColor"] == "LIGHT_SUMMER"
        assert call_kwargs.kwargs["json"]["resultImageUrl"] == "http://storage/result/abc.png"
        assert call_kwargs.kwargs["headers"]["Authorization"] == "Bearer token123"

    async def test_raises_on_server_error(self):
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = httpx.HTTPStatusError(
            "401", request=MagicMock(), response=MagicMock()
        )

        with patch("app.services.main_client.httpx.AsyncClient") as mock_client_cls:
            mock_client = AsyncMock()
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client.patch = AsyncMock(return_value=mock_response)
            mock_client_cls.return_value = mock_client

            with pytest.raises(httpx.HTTPStatusError):
                await update_user_diagnosis("Bearer bad", "LIGHT_SUMMER", "http://x")


class TestRequestDiagnosisStart:
    async def test_posts_to_diagnosis_request(self):
        mock_response = MagicMock()
        mock_response.raise_for_status = MagicMock()

        with patch("app.services.main_client.httpx.AsyncClient") as mock_client_cls:
            mock_client = AsyncMock()
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client.post = AsyncMock(return_value=mock_response)
            mock_client_cls.return_value = mock_client

            await request_diagnosis_start("Bearer token123")

        mock_client.post.assert_called_once()
        call_kwargs = mock_client.post.call_args
        assert "/api/users/me/diagnosis-request" in call_kwargs.args[0]
        assert call_kwargs.kwargs["headers"]["Authorization"] == "Bearer token123"

    async def test_raises_409_on_duplicate(self):
        mock_resp = MagicMock()
        mock_resp.status_code = 409
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = httpx.HTTPStatusError(
            "409", request=MagicMock(), response=mock_resp
        )

        with patch("app.services.main_client.httpx.AsyncClient") as mock_client_cls:
            mock_client = AsyncMock()
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client.post = AsyncMock(return_value=mock_response)
            mock_client_cls.return_value = mock_client

            with pytest.raises(httpx.HTTPStatusError):
                await request_diagnosis_start("Bearer token123")


class TestResetDiagnosisStatus:
    async def test_deletes_diagnosis_request(self):
        mock_response = MagicMock()
        mock_response.raise_for_status = MagicMock()

        with patch("app.services.main_client.httpx.AsyncClient") as mock_client_cls:
            mock_client = AsyncMock()
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client.delete = AsyncMock(return_value=mock_response)
            mock_client_cls.return_value = mock_client

            await reset_diagnosis_status("Bearer token123")

        mock_client.delete.assert_called_once()
        call_kwargs = mock_client.delete.call_args
        assert "/api/users/me/diagnosis-request" in call_kwargs.args[0]
