from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    openai_api_key: str = ""
    openai_model: str = "gpt-4o"
    jwt_secret: str = ""

    # 고객센터 도구가 사용자 데이터를 조회할 main-server 베이스 URL
    main_server_url: str = "http://main-server:8080"

    # 대화 기억(1시간 윈도우) 저장용 Redis
    redis_host: str = "redis"
    redis_port: int = 6379


settings = Settings()
