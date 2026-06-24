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

	reg := metrics.NewRegistry()

	opts := []api.Option{api.WithMetrics(reg)}
	if jwtSecret := os.Getenv("JWT_SECRET"); jwtSecret != "" {
		keyBytes, err := base64.StdEncoding.DecodeString(jwtSecret)
		if err != nil {
			log.Fatalf("JWT_SECRET must be a base64-encoded string: %v", err)
		}
		opts = append(opts, api.WithJWT(internaljwt.New(keyBytes)))
		log.Printf("JWT auth enabled for result images")
	} else {
		// M-7: JWT_SECRET 누락 시 result 이미지가 무인증 공개된다. 운영에서 무음 보안 구멍이 되지 않도록 부팅을 중단한다.
		log.Fatalf("JWT_SECRET must be set — result images would be publicly accessible without it")
	}

	mux := http.NewServeMux()
	mux.Handle("/storage/", reg.InstrumentHandler("storage", api.Handler(store, opts...)))
	mux.Handle("GET /metrics", reg.Handler())

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
