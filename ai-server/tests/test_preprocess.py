"""Image preprocessing tests — resize rules:
- Portrait (height > width): width becomes 1024px, height scales proportionally
- Landscape/square (width >= height): height becomes 1024px, width scales proportionally
"""

import io
import pytest
from PIL import Image

from app.services.preprocess import resize_for_api


def make_image(width: int, height: int) -> bytes:
    img = Image.new("RGB", (width, height), color=(128, 64, 32))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    return buf.getvalue()


class TestResizeForApi:
    def test_portrait_fixes_width_to_1024(self):
        # 600x900 is portrait
        data = make_image(600, 900)
        result = resize_for_api(data)
        img = Image.open(io.BytesIO(result))
        assert img.width == 1024
        assert img.height == pytest.approx(1536, abs=2)  # 900 * 1024/600

    def test_landscape_fixes_height_to_1024(self):
        # 1600x900 is landscape
        data = make_image(1600, 900)
        result = resize_for_api(data)
        img = Image.open(io.BytesIO(result))
        assert img.height == 1024
        assert img.width == pytest.approx(1820, abs=2)  # 1600 * 1024/900

    def test_square_fixes_height_to_1024(self):
        # 800x800 is treated as landscape (width >= height)
        data = make_image(800, 800)
        result = resize_for_api(data)
        img = Image.open(io.BytesIO(result))
        assert img.height == 1024
        assert img.width == 1024

    def test_already_portrait_1024_width_unchanged(self):
        # Already 1024 wide portrait — still valid, no upscale distortion
        data = make_image(1024, 1500)
        result = resize_for_api(data)
        img = Image.open(io.BytesIO(result))
        assert img.width == 1024

    def test_already_landscape_1024_height_unchanged(self):
        data = make_image(1500, 1024)
        result = resize_for_api(data)
        img = Image.open(io.BytesIO(result))
        assert img.height == 1024

    def test_returns_bytes(self):
        data = make_image(400, 600)
        result = resize_for_api(data)
        assert isinstance(result, bytes)
        assert len(result) > 0
