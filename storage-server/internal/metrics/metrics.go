// Package metrics exposes Prometheus metrics for the storage server.
package metrics

import (
	"net/http"
	"strconv"
	"sync"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// Registry tracks storage-specific counters.
type Registry struct {
	mu      sync.Mutex
	uploads map[string]int64

	uploadCounter *prometheus.CounterVec
	httpRequests  *prometheus.CounterVec
	httpDuration  *prometheus.HistogramVec
	reg           *prometheus.Registry
}

// NewRegistry creates a Registry with its own Prometheus registry.
func NewRegistry() *Registry {
	reg := prometheus.NewRegistry()
	reg.MustRegister(prometheus.NewGoCollector())
	reg.MustRegister(prometheus.NewProcessCollector(prometheus.ProcessCollectorOpts{}))
	counter := prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "storage_uploads_total",
		Help: "Total number of image uploads by kind (raw|result).",
	}, []string{"kind"})
	httpReqs := prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "storage_http_requests_total",
		Help: "Total HTTP requests by method, route and status.",
	}, []string{"method", "route", "status"})
	httpDur := prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "storage_http_request_duration_seconds",
		Help:    "HTTP request latency in seconds.",
		Buckets: prometheus.DefBuckets,
	}, []string{"method", "route"})
	reg.MustRegister(counter, httpReqs, httpDur)

	return &Registry{
		uploads:       make(map[string]int64),
		uploadCounter: counter,
		httpRequests:  httpReqs,
		httpDuration:  httpDur,
		reg:           reg,
	}
}

// InstrumentHandler wraps h with request count and latency histograms.
func (r *Registry) InstrumentHandler(route string, h http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
		start := time.Now()
		rec := &statusRecorder{ResponseWriter: w, status: http.StatusOK}
		h.ServeHTTP(rec, req)
		status := strconv.Itoa(rec.status)
		r.httpRequests.WithLabelValues(req.Method, route, status).Inc()
		r.httpDuration.WithLabelValues(req.Method, route).Observe(time.Since(start).Seconds())
	})
}

type statusRecorder struct {
	http.ResponseWriter
	status int
}

func (r *statusRecorder) WriteHeader(code int) {
	r.status = code
	r.ResponseWriter.WriteHeader(code)
}

// Handler serves this Registry's metrics (upload counters + runtime collectors).
func (r *Registry) Handler() http.Handler {
	return promhttp.HandlerFor(r.reg, promhttp.HandlerOpts{})
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
