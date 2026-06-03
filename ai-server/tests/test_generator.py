"""Unit tests for OpenAI image generation wrapper."""

from pathlib import Path
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.services import generator


@pytest.fixture
def template_file(tmp_path: Path) -> Path:
    path = tmp_path / "template.png"
    path.write_bytes(b"template-bytes")
    return path


class TestGenerateAnalysisImage:
    async def test_calls_images_edit_with_gpt_image_2_and_two_references(self, template_file: Path):
        mock_response = MagicMock()
        mock_response.data = [MagicMock(b64_json="aGVsbG8=")]

        mock_edit = AsyncMock(return_value=mock_response)
        mock_client = MagicMock()
        mock_client.images.edit = mock_edit

        with patch.object(generator, "settings") as mock_settings, patch(
            "app.services.generator.AsyncOpenAI", return_value=mock_client
        ):
            mock_settings.openai_api_key = "sk-test"
            mock_settings.template_image_path = str(template_file)

            result = await generator.generate_analysis_image(b"user-jpeg-bytes")

        assert result == b"hello"
        call_kwargs = mock_edit.await_args.kwargs
        assert call_kwargs["model"] == "gpt-image-2"
        assert call_kwargs["size"] == "1024x1824"
        images = call_kwargs["image"]
        assert len(images) == 2
        assert hasattr(images[0], "read")
        assert images[1] == ("user.jpg", b"user-jpeg-bytes", "image/jpeg")

    async def test_closes_template_file_after_request(self, template_file: Path):
        mock_response = MagicMock()
        mock_response.data = [MagicMock(b64_json="aGk=")]

        opened = []
        real_open = open

        def tracking_open(path, mode="rb"):
            f = real_open(path, mode)
            opened.append(f)
            return f

        mock_edit = AsyncMock(return_value=mock_response)
        mock_client = MagicMock()
        mock_client.images.edit = mock_edit

        with patch.object(generator, "settings") as mock_settings, patch(
            "app.services.generator.AsyncOpenAI", return_value=mock_client
        ), patch("builtins.open", side_effect=tracking_open):
            mock_settings.openai_api_key = "sk-test"
            mock_settings.template_image_path = str(template_file)

            await generator.generate_analysis_image(b"jpeg")

        assert len(opened) == 1
        assert opened[0].closed
