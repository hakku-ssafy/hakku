import os
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    openai_api_key: str = ""
    storage_server_url: str = "http://localhost:8080"
    main_server_url: str = "http://localhost:8081"
    template_image_path: str = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "..", "src", "result1.png"
    )


settings = Settings()
