package api_test

import (
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/hakku/storage-server/internal/api"
	"github.com/hakku/storage-server/internal/storage"
)

func newServer(t *testing.T) http.Handler {
	t.Helper()
	store, err := storage.NewFSStore(t.TempDir())
	if err != nil {
		t.Fatalf("NewFSStore: %v", err)
	}
	return api.Handler(store)
}

func upload(t *testing.T, h http.Handler, kindQuery, contentType, body string) storage.Metadata {
	t.Helper()
	target := "/storage/images"
	if kindQuery != "" {
		target += "?kind=" + kindQuery
	}
	req := httptest.NewRequest(http.MethodPost, target, strings.NewReader(body))
	req.Header.Set("Content-Type", contentType)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusCreated {
		t.Fatalf("upload status = %d, want 201 (body %s)", rec.Code, rec.Body.String())
	}
	var meta storage.Metadata
	if err := json.Unmarshal(rec.Body.Bytes(), &meta); err != nil {
		t.Fatalf("decode metadata: %v", err)
	}
	return meta
}

func TestUploadThenDownload(t *testing.T) {
	h := newServer(t)
	meta := upload(t, h, "result", "image/png", "fake-png")

	if meta.ID == "" || meta.Kind != storage.KindResult || meta.Size != int64(len("fake-png")) {
		t.Fatalf("unexpected metadata: %+v", meta)
	}

	req := httptest.NewRequest(http.MethodGet, "/storage/images/"+meta.ID, nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("download status = %d, want 200", rec.Code)
	}
	if ct := rec.Header().Get("Content-Type"); ct != "image/png" {
		t.Errorf("Content-Type = %q, want image/png", ct)
	}
	if got, _ := io.ReadAll(rec.Body); string(got) != "fake-png" {
		t.Errorf("body = %q, want fake-png", got)
	}
}

func TestUpload_DefaultsToRawKind(t *testing.T) {
	h := newServer(t)
	meta := upload(t, h, "", "image/jpeg", "j")
	if meta.Kind != storage.KindRaw {
		t.Errorf("kind = %q, want raw", meta.Kind)
	}
}

func TestUpload_InvalidKind_400(t *testing.T) {
	h := newServer(t)
	req := httptest.NewRequest(http.MethodPost, "/storage/images?kind=bogus", strings.NewReader("x"))
	req.Header.Set("Content-Type", "image/png")
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Errorf("status = %d, want 400", rec.Code)
	}
}

func TestMetadataEndpoint(t *testing.T) {
	h := newServer(t)
	meta := upload(t, h, "raw", "image/png", "abc")

	req := httptest.NewRequest(http.MethodGet, "/storage/images/"+meta.ID+"/meta", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("meta status = %d, want 200", rec.Code)
	}
	var got storage.Metadata
	if err := json.Unmarshal(rec.Body.Bytes(), &got); err != nil {
		t.Fatalf("decode: %v", err)
	}
	if got.ID != meta.ID || got.Size != 3 || got.ContentType != "image/png" {
		t.Errorf("meta = %+v, want id=%s size=3 contentType=image/png", got, meta.ID)
	}
}

func TestDelete_RemovesImage(t *testing.T) {
	h := newServer(t)
	meta := upload(t, h, "raw", "image/png", "x")

	req := httptest.NewRequest(http.MethodDelete, "/storage/images/"+meta.ID, nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusNoContent {
		t.Fatalf("delete status = %d, want 204", rec.Code)
	}

	get := httptest.NewRequest(http.MethodGet, "/storage/images/"+meta.ID, nil)
	getRec := httptest.NewRecorder()
	h.ServeHTTP(getRec, get)
	if getRec.Code != http.StatusNotFound {
		t.Errorf("get after delete status = %d, want 404", getRec.Code)
	}
}

func TestDownload_UnknownID_404(t *testing.T) {
	h := newServer(t)
	req := httptest.NewRequest(http.MethodGet, "/storage/images/unknown", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Errorf("status = %d, want 404", rec.Code)
	}
}

func TestDelete_UnknownID_404(t *testing.T) {
	h := newServer(t)
	req := httptest.NewRequest(http.MethodDelete, "/storage/images/unknown", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusNotFound {
		t.Errorf("status = %d, want 404", rec.Code)
	}
}
