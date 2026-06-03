// Package metrics exposes Prometheus metrics for the storage server.
package metrics

import (
	"net/http"
	"sync"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// Registry tracks storage-specific counters.
type Registry struct {
	mu      sync.Mutex
	uploads map[string]int64

	uploadCounter *prometheus.CounterVec
	reg           *prometheus.Registry
}

// NewRegistry creates a Registry with its own Prometheus registry.
func NewRegistry() *Registry {
	reg := prometheus.NewRegistry()
	counter := prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "storage_uploads_total",
		Help: "Total number of image uploads by kind (raw|result).",
	}, []string{"kind"})
	reg.MustRegister(counter)

	return &Registry{
		uploads:       make(map[string]int64),
		uploadCounter: counter,
		reg:           reg,
	}
}

// RecordUpload increments the upload counter for the given kind.
func (r *Registry) RecordUpload(kind string) {
	r.mu.Lock()
	r.uploads[kind]++
	r.mu.Unlock()
	r.uploadCounter.WithLabelValues(kind).Inc()
}

// UploadCount returns the in-process upload count for kind (used in tests).
func (r *Registry) UploadCount(kind string) int64 {
	r.mu.Lock()
	defer r.mu.Unlock()
	return r.uploads[kind]
}

// Handler returns an http.Handler that serves Prometheus metrics including
// Go runtime and process metrics plus storage-specific counters.
func Handler() http.Handler {
	reg := prometheus.NewRegistry()
	reg.MustRegister(prometheus.NewGoCollector())
	reg.MustRegister(prometheus.NewProcessCollector(prometheus.ProcessCollectorOpts{}))
	uploadCounter := prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "storage_uploads_total",
		Help: "Total number of image uploads by kind (raw|result).",
	}, []string{"kind"})
	reg.MustRegister(uploadCounter)
	return promhttp.HandlerFor(reg, promhttp.HandlerOpts{})
}
