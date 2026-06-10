// Package api exposes the storage server's HTTP surface under /storage
// (PRD §3.4): upload raw/result images, download, delete, and read metadata.
package api

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strconv"
	"strings"

	"github.com/hakku/storage-server/internal/storage"
)

const defaultContentType = "application/octet-stream"

// Validator extracts and verifies a JWT Bearer token's subject claim.
// Return an error if the token is missing, malformed, or expired.
type Validator interface {
	Subject(token string) (string, error)
}

// Option configures a Handler.
type Option func(*router)

// WithJWT enables JWT authentication for result-kind image operations.
// Upload requires a valid token; download requires the token subject to match the owner.
func WithJWT(v Validator) Option {
	return func(r *router) { r.validator = v }
}

// UploadRecorder receives a notification for each successful upload.
type UploadRecorder interface {
	RecordUpload(kind string)
}

// WithMetrics records successful uploads to the given recorder.
func WithMetrics(rec UploadRecorder) Option {
	return func(r *router) { r.recorder = rec }
}

type router struct {
	store     storage.Store
	validator Validator      // nil = no auth required (dev mode)
	recorder  UploadRecorder // nil = no metrics
}

// Handler returns an HTTP handler for the storage routes.
// Pass WithJWT to enforce JWT authentication for result-kind images.
func Handler(store storage.Store, opts ...Option) http.Handler {
	r := &router{store: store}
	for _, o := range opts {
		o(r)
	}
	mux := http.NewServeMux()
	mux.HandleFunc("POST /storage/images", r.upload)
	mux.HandleFunc("GET /storage/images/{id}", r.download)
	mux.HandleFunc("GET /storage/images/{id}/meta", r.stat)
	mux.HandleFunc("DELETE /storage/images/{id}", r.remove)
	return mux
}

func (r *router) bearerSubject(req *http.Request) (string, error) {
	auth := req.Header.Get("Authorization")
	if !strings.HasPrefix(auth, "Bearer ") {
		return "", errors.New("missing bearer token")
	}
	return r.validator.Subject(strings.TrimPrefix(auth, "Bearer "))
}

func (r *router) upload(w http.ResponseWriter, req *http.Request) {
	kind := storage.Kind(req.URL.Query().Get("kind"))
	if kind == "" {
		kind = storage.KindRaw
	}
	contentType := req.Header.Get("Content-Type")
	if contentType == "" {
		contentType = defaultContentType
	}

	var ownerID string
	if kind == storage.KindResult && r.validator != nil {
		sub, err := r.bearerSubject(req)
		if err != nil {
			writeError(w, http.StatusUnauthorized, "authentication required")
			return
		}
		ownerID = sub
	}

	defer req.Body.Close()
	meta, err := r.store.Put(kind, contentType, ownerID, req.Body)
	if err != nil {
		if errors.Is(err, storage.ErrInvalidKind) {
			writeError(w, http.StatusBadRequest, "invalid image kind")
			return
		}
		writeError(w, http.StatusInternalServerError, "failed to store image")
		return
	}
	if r.recorder != nil {
		r.recorder.RecordUpload(string(meta.Kind))
	}
	writeJSON(w, http.StatusCreated, meta)
}

func (r *router) download(w http.ResponseWriter, req *http.Request) {
	body, meta, err := r.store.Get(req.PathValue("id"))
	if err != nil {
		writeStoreError(w, err)
		return
	}
	defer body.Close()

	if meta.Kind == storage.KindResult && r.validator != nil {
		sub, err := r.bearerSubject(req)
		if err != nil {
			writeError(w, http.StatusUnauthorized, "authentication required")
			return
		}
		if meta.OwnerID != "" && sub != meta.OwnerID {
			writeError(w, http.StatusForbidden, "access denied")
			return
		}
	}

	w.Header().Set("Content-Type", meta.ContentType)
	w.Header().Set("Content-Length", strconv.FormatInt(meta.Size, 10))
	w.WriteHeader(http.StatusOK)
	_, _ = io.Copy(w, body)
}

func (r *router) stat(w http.ResponseWriter, req *http.Request) {
	meta, err := r.store.Stat(req.PathValue("id"))
	if err != nil {
		writeStoreError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, meta)
}

func (r *router) remove(w http.ResponseWriter, req *http.Request) {
	if err := r.store.Delete(req.PathValue("id")); err != nil {
		writeStoreError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func writeStoreError(w http.ResponseWriter, err error) {
	if errors.Is(err, storage.ErrNotFound) {
		writeError(w, http.StatusNotFound, "image not found")
		return
	}
	writeError(w, http.StatusInternalServerError, "storage error")
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, msg string) {
	writeJSON(w, status, map[string]string{"error": msg})
}
