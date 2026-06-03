"""Tests for the /metrics Prometheus endpoint."""
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_metrics_endpoint_returns_200():
    response = client.get("/metrics")
    assert response.status_code == 200


def test_metrics_response_is_text_plain():
    response = client.get("/metrics")
    assert "text/plain" in response.headers["content-type"]


def test_metrics_contains_prometheus_format():
    response = client.get("/metrics")
    assert "# HELP" in response.text


def test_metrics_includes_http_request_counter():
    # Make a known request first so the counter exists
    client.get("/health")
    response = client.get("/metrics")
    assert "http_requests_total" in response.text or "http_request" in response.text
