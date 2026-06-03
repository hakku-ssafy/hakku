"""Diagnosis API endpoint integration tests.
All external calls (OpenAI, storage-server, main-server) are mocked.
"""

import io
import pytest
import httpx
from unittest.mock import AsyncMock, patch, MagicMock
from fastapi.testclient import TestClient
from PIL import Image

from app.main import create_app


def make_jpeg(width: int = 400, height: int = 600) -> bytes:
    img = Image.new("RGB", (width, height), color=(180, 120, 80))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    return buf.getvalue()


MOCK_RESULT_IMAGE = b"fake-result-image-bytes"
MOCK_STORAGE_ID = "abc123def456"
MOCK_PERSONAL_COLOR = "LIGHT_SUMMER"
MOCK_STORAGE_URL = f"http://localhost:8080/storage/images/{MOCK_STORAGE_ID}"

VALID_JWT = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.test"


@pytest.fixture
def client():
    app = create_app()
    return TestClient(app)


@pytest.fixture
def mock_pipeline():
    with (
        patch("app.services.main_client.request_diagnosis_start",
              new_callable=AsyncMock, return_value=None) as mock_start,
        patch("app.services.generator.generate_analysis_image",
              new_callable=AsyncMock, return_value=MOCK_RESULT_IMAGE) as mock_gen,
        patch("app.services.storage.upload_result_image",
              new_callable=AsyncMock,
              return_value={"id": MOCK_STORAGE_ID, "url": MOCK_STORAGE_URL}) as mock_storage,
        patch("app.services.vision.extract_color_from_image",
              new_callable=AsyncMock, return_value=MOCK_PERSONAL_COLOR) as mock_vision,
        patch("app.services.main_client.update_user_diagnosis",
              new_callable=AsyncMock, return_value=None) as mock_update,
        patch("app.services.main_client.reset_diagnosis_status",
              new_callable=AsyncMock, return_value=None) as mock_reset,
    ):
        yield {
            "start": mock_start,
            "gen": mock_gen,
            "storage": mock_storage,
            "vision": mock_vision,
            "update": mock_update,
            "reset": mock_reset,
        }


class TestDiagnosisEndpoint:
    def test_diagnosis_accepted_returns_202(self, client, mock_pipeline):
        image_bytes = make_jpeg(400, 600)
        response = client.post(
            "/api/diagnosis",
            headers={"Authorization": VALID_JWT},
            files={"image": ("photo.jpg", image_bytes, "image/jpeg")},
        )
        assert response.status_code == 202
        body = response.json()
        assert "message" in body

    def test_diagnosis_locks_slot_before_accepting(self, client, mock_pipeline):
        client.post(
            "/api/diagnosis",
            headers={"Authorization": VALID_JWT},
            files={"image": ("photo.jpg", make_jpeg(), "image/jpeg")},
        )
        mock_pipeline["start"].assert_called_once_with(VALID_JWT)

    def test_diagnosis_already_requested_returns_409(self, client, mock_pipeline):
        mock_response = MagicMock()
        mock_response.status_code = 409
        mock_pipeline["start"].side_effect = httpx.HTTPStatusError(
            "Conflict", request=MagicMock(), response=mock_response
        )

        response = client.post(
            "/api/diagnosis",
            headers={"Authorization": VALID_JWT},
            files={"image": ("photo.jpg", make_jpeg(), "image/jpeg")},
        )
        assert response.status_code == 409

    def test_diagnosis_calls_generator_with_preprocessed_image(self, client, mock_pipeline):
        image_bytes = make_jpeg(400, 600)
        client.post(
            "/api/diagnosis",
            headers={"Authorization": VALID_JWT},
            files={"image": ("photo.jpg", image_bytes, "image/jpeg")},
        )
        mock_pipeline["gen"].assert_called_once()
        called_image_bytes = mock_pipeline["gen"].call_args[0][0]
        preprocessed = Image.open(io.BytesIO(called_image_bytes))
        assert preprocessed.width == 1024  # portrait → width 1024

    def test_diagnosis_calls_storage_with_result_image(self, client, mock_pipeline):
        client.post(
            "/api/diagnosis",
            headers={"Authorization": VALID_JWT},
            files={"image": ("photo.jpg", make_jpeg(), "image/jpeg")},
        )
        mock_pipeline["storage"].assert_called_once_with(MOCK_RESULT_IMAGE)

    def test_diagnosis_calls_main_server_update(self, client, mock_pipeline):
        client.post(
            "/api/diagnosis",
            headers={"Authorization": VALID_JWT},
            files={"image": ("photo.jpg", make_jpeg(), "image/jpeg")},
        )
        mock_pipeline["update"].assert_called_once_with(
            VALID_JWT, MOCK_PERSONAL_COLOR, MOCK_STORAGE_URL
        )

    def test_diagnosis_no_auth_returns_401(self, client, mock_pipeline):
        response = client.post(
            "/api/diagnosis",
            files={"image": ("photo.jpg", make_jpeg(), "image/jpeg")},
        )
        assert response.status_code == 401

    def test_diagnosis_no_image_returns_422(self, client, mock_pipeline):
        response = client.post(
            "/api/diagnosis",
            headers={"Authorization": VALID_JWT},
        )
        assert response.status_code == 422

    def test_diagnosis_unsupported_media_type_returns_400(self, client, mock_pipeline):
        response = client.post(
            "/api/diagnosis",
            headers={"Authorization": VALID_JWT},
            files={"image": ("doc.pdf", b"pdf-bytes", "application/pdf")},
        )
        assert response.status_code == 400

    def test_diagnosis_pipeline_failure_resets_status(self, client, mock_pipeline):
        mock_pipeline["gen"].side_effect = Exception("OpenAI 오류")

        client.post(
            "/api/diagnosis",
            headers={"Authorization": VALID_JWT},
            files={"image": ("photo.jpg", make_jpeg(), "image/jpeg")},
        )
        mock_pipeline["reset"].assert_called_once_with(VALID_JWT)
