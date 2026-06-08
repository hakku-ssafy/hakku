from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.api.chat import router as chat_router


def create_app() -> FastAPI:
    app = FastAPI(title="Hakku Chatbot Server", version="0.1.0")
    app.include_router(chat_router)

    @app.get("/health")
    def health() -> dict:
        return {"status": "ok"}

    Instrumentator().instrument(app).expose(app, endpoint="/metrics")

    return app


app = create_app()
