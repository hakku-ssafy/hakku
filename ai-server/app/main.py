"""FastAPI application factory."""

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.api.diagnosis import router as diagnosis_router


def create_app() -> FastAPI:
    app = FastAPI(title="Hakku AI Server", version="0.1.0")
    app.include_router(diagnosis_router)

    @app.get("/health")
    def health() -> dict:
        return {"status": "ok"}

    Instrumentator().instrument(app).expose(app, endpoint="/metrics")

    return app


app = create_app()
