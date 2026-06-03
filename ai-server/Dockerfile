# Build context must be the repo root:
#   docker build -f ai-server/Dockerfile .
# docker-compose.yml sets context: . and dockerfile: ai-server/Dockerfile
FROM python:3.13-slim

WORKDIR /app

COPY ai-server/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY ai-server/app/ ./app/
COPY src/ ./src/

ENV TEMPLATE_IMAGE_PATH=/app/src/result1.png

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
