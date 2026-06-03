// Command storage-server runs the Go image storage service (PRD §3.4).
// It serves /storage routes backed by a filesystem store rooted at
// STORAGE_BASE_PATH.
package main

import (
	"log"
	"net/http"
	"os"

	"github.com/hakku/storage-server/internal/api"
	"github.com/hakku/storage-server/internal/metrics"
	"github.com/hakku/storage-server/internal/storage"
)

func main() {
	basePath := envOr("STORAGE_BASE_PATH", "/data/images")
	addr := envOr("STORAGE_ADDR", ":8080")

	store, err := storage.NewFSStore(basePath)
	if err != nil {
		log.Fatalf("storage init: %v", err)
	}

	mux := http.NewServeMux()
	mux.Handle("/storage/", api.Handler(store))
	mux.Handle("GET /metrics", metrics.Handler())

	log.Printf("storage-server listening on %s (base path %s)", addr, basePath)
	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatalf("server: %v", err)
	}
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
