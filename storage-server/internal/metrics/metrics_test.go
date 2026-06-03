package metrics_test

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/hakku/storage-server/internal/metrics"
)

func TestMetricsHandlerReturns200(t *testing.T) {
	h := metrics.Handler()
	req := httptest.NewRequest(http.MethodGet, "/metrics", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", rec.Code)
	}
}

func TestMetricsResponseContainsPrometheusMetrics(t *testing.T) {
	h := metrics.Handler()
	req := httptest.NewRequest(http.MethodGet, "/metrics", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)

	body := rec.Body.String()
	if !strings.Contains(body, "# HELP") {
		t.Errorf("expected Prometheus metrics format with # HELP lines, got: %s", body[:min(200, len(body))])
	}
}

func TestRecordUploadIncreasesCounter(t *testing.T) {
	reg := metrics.NewRegistry()
	reg.RecordUpload("raw")
	reg.RecordUpload("raw")
	reg.RecordUpload("result")

	if got := reg.UploadCount("raw"); got != 2 {
		t.Errorf("UploadCount(raw) = %d, want 2", got)
	}
	if got := reg.UploadCount("result"); got != 1 {
		t.Errorf("UploadCount(result) = %d, want 1", got)
	}
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
