// Package api exposes the storage server's HTTP surface under /storage
// (PRD §3.4): upload raw/result images, download, delete, and read metadata.
package api

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strconv"

	"github.com/hakku/storage-server/internal/storage"
)

const defaultContentType = "application/octet-stream"

// Handler wires the storage routes onto a ServeMux backed by the given store.
func Handler(store storage.Store) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /storage/images", upload(store))
	mux.HandleFunc("GET /storage/images/{id}", download(store))
	mux.HandleFunc("GET /storage/images/{id}/meta", stat(store))
	mux.HandleFunc("DELETE /storage/images/{id}", remove(store))
	return mux
}

// upload stores the request body. The image kind comes from the ?kind= query
// (defaults to raw) and the content type from the Content-Type header.
func upload(store storage.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		kind := storage.Kind(r.URL.Query().Get("kind"))
		if kind == "" {
			kind = storage.KindRaw
		}
		contentType := r.Header.Get("Content-Type")
		if contentType == "" {
			contentType = defaultContentType
		}
		defer r.Body.Close()

		meta, err := store.Put(kind, contentType, r.Body)
		if err != nil {
			if errors.Is(err, storage.ErrInvalidKind) {
				writeError(w, http.StatusBadRequest, "invalid image kind")
				return
			}
			writeError(w, http.StatusInternalServerError, "failed to store image")
			return
		}
		writeJSON(w, http.StatusCreated, meta)
	}
}

func download(store storage.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		body, meta, err := store.Get(r.PathValue("id"))
		if err != nil {
			writeStoreError(w, err)
			return
		}
		defer body.Close()
		w.Header().Set("Content-Type", meta.ContentType)
		w.Header().Set("Content-Length", strconv.FormatInt(meta.Size, 10))
		w.WriteHeader(http.StatusOK)
		_, _ = io.Copy(w, body)
	}
}

func stat(store storage.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		meta, err := store.Stat(r.PathValue("id"))
		if err != nil {
			writeStoreError(w, err)
			return
		}
		writeJSON(w, http.StatusOK, meta)
	}
}

func remove(store storage.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if err := store.Delete(r.PathValue("id")); err != nil {
			writeStoreError(w, err)
			return
		}
		w.WriteHeader(http.StatusNoContent)
	}
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
