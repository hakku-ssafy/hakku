"""Unit tests for vision service (Responses API)."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.services import vision


class TestExtractColorFromImage:
    async def test_uses_responses_api_with_image_and_text_input(self):
        mock_response = MagicMock()
        mock_response.output_text = "세부 타입: 여름 쿨 라이트 (LIGHT_SUMMER)"

        mock_create = AsyncMock(return_value=mock_response)
        mock_client = MagicMock()
        mock_client.responses.create = mock_create

        with patch.object(vision, "settings") as mock_settings, patch(
            "app.services.vision.AsyncOpenAI", return_value=mock_client
        ), patch(
            "app.services.vision.extract_personal_color_type",
            return_value="LIGHT_SUMMER",
        ) as mock_ocr:
            mock_settings.openai_api_key = "sk-test"
            result = await vision.extract_color_from_image(b"png-bytes")

        assert result == "LIGHT_SUMMER"
        mock_create.assert_awaited_once()
        call_kwargs = mock_create.await_args.kwargs
        assert call_kwargs["model"] == "gpt-5.4-mini"
        assert call_kwargs["reasoning"] == {"effort": "low"}
        assert call_kwargs["max_output_tokens"] == 512

        content = call_kwargs["input"][0]["content"]
        assert content[0]["type"] == "input_image"
        assert content[0]["detail"] == "high"
        assert content[0]["image_url"].startswith("data:image/png;base64,")
        assert content[1]["type"] == "input_text"
        mock_ocr.assert_called_once_with("세부 타입: 여름 쿨 라이트 (LIGHT_SUMMER)")

    async def test_returns_none_when_output_text_empty(self):
        mock_response = MagicMock()
        mock_response.output_text = ""

        mock_client = MagicMock()
        mock_client.responses.create = AsyncMock(return_value=mock_response)

        with patch.object(vision, "settings") as mock_settings, patch(
            "app.services.vision.AsyncOpenAI", return_value=mock_client
        ), patch("app.services.vision.extract_personal_color_type", return_value=None):
            mock_settings.openai_api_key = "sk-test"
            result = await vision.extract_color_from_image(b"png-bytes")

        assert result is None
