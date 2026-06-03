"""Image preprocessing for the personal color analysis pipeline.

Resize rules:
- Portrait (height > width): fix width to 1024px, scale height proportionally
- Landscape / square (width >= height): fix height to 1024px, scale width proportionally
"""

import io
from PIL import Image

TARGET = 1024


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

    buf = io.BytesIO()
    fmt = img.format or "JPEG"
    img.save(buf, format=fmt)
    return buf.getvalue()
