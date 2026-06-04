// Command storage-server runs the Go image storage service (PRD §3.4).
// It serves /storage routes backed by a filesystem store rooted at
// STORAGE_BASE_PATH. When JWT_SECRET is set, result-kind images require
// a valid Bearer token and only the uploading user can download them.
package main

import (
	"encoding/base64"
	"log"
	"net/http"
	"os"

	"github.com/hakku/storage-server/internal/api"
	internaljwt "github.com/hakku/storage-server/internal/jwt"
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

	var opts []api.Option
	if jwtSecret := os.Getenv("JWT_SECRET"); jwtSecret != "" {
		keyBytes, err := base64.StdEncoding.DecodeString(jwtSecret)
		if err != nil {
			log.Fatalf("JWT_SECRET must be a base64-encoded string: %v", err)
		}
		opts = append(opts, api.WithJWT(internaljwt.New(keyBytes)))
		log.Printf("JWT auth enabled for result images")
	} else {
		log.Printf("WARNING: JWT_SECRET not set — result images are publicly accessible")
	}

	mux := http.NewServeMux()
	mux.Handle("/storage/", api.Handler(store, opts...))
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
