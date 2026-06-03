"""Image preprocessing for the personal color analysis pipeline.

Resize rules:
- Portrait (height > width): fix width to 1024px, scale height proportionally
- Landscape / square (width >= height): fix height to 1024px, scale width proportionally

Output is always JPEG. Transparent pixels are composited onto a white background.
"""

import io
from PIL import Image

TARGET = 1024
JPEG_QUALITY = 90


def _flatten_on_white(img: Image.Image) -> Image.Image:
    """Composite transparency onto white and return RGB."""
    if img.mode in ("RGBA", "LA") or (img.mode == "P" and "transparency" in img.info):
        rgba = img.convert("RGBA")
        background = Image.new("RGBA", rgba.size, (255, 255, 255, 255))
        return Image.alpha_composite(background, rgba).convert("RGB")
    return img.convert("RGB")


def resize_for_api(image_bytes: bytes) -> bytes:
    img = Image.open(io.BytesIO(image_bytes))
    w, h = img.size

    if h > w:
        # Portrait — fix width
        new_w = TARGET
        new_h = round(h * TARGET / w)
    else:
        # Landscape or square — fix height
        new_h = TARGET
        new_w = round(w * TARGET / h)

    if (w, h) != (new_w, new_h):
        img = img.resize((new_w, new_h), Image.LANCZOS)

    rgb = _flatten_on_white(img)

    buf = io.BytesIO()
    rgb.save(buf, format="JPEG", quality=JPEG_QUALITY)
    return buf.getvalue()
