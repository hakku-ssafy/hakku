// Package storage persists image blobs and their metadata on the local
// filesystem (STORAGE_BASE_PATH). Each image is stored as two sidecar files:
// a {id}.bin blob and a {id}.json metadata record.
package storage

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
	"time"
)

// Kind distinguishes raw uploads (discarded after AI processing) from the
// AI-generated result images that are kept (see PRD §3.4 / AI pipeline).
type Kind string

const (
	KindRaw    Kind = "raw"
	KindResult Kind = "result"
)

func (k Kind) valid() bool { return k == KindRaw || k == KindResult }

// Metadata describes a stored image. It is persisted alongside the blob.
type Metadata struct {
	ID          string    `json:"id"`
	Kind        Kind      `json:"kind"`
	ContentType string    `json:"contentType"`
	Size        int64     `json:"size"`
	CreatedAt   time.Time `json:"createdAt"`
	OwnerID     string    `json:"ownerId,omitempty"` // non-empty for result images; identifies the uploading user
}

var (
	// ErrNotFound is returned when no image exists for the given id.
	ErrNotFound = errors.New("storage: image not found")
	// ErrInvalidKind is returned when Put receives an unknown kind.
	ErrInvalidKind = errors.New("storage: invalid image kind")
)

// Store is the image persistence boundary, kept abstract so the HTTP layer
// depends on behaviour rather than the filesystem implementation.
type Store interface {
	Put(kind Kind, contentType string, ownerID string, r io.Reader) (Metadata, error)
	Get(id string) (io.ReadCloser, Metadata, error)
	Stat(id string) (Metadata, error)
	Delete(id string) error
}

// FSStore is a filesystem-backed Store. Operations on distinct ids are
// independent, so it is safe for concurrent use across different images.
type FSStore struct {
	basePath string
}

var _ Store = (*FSStore)(nil)

// NewFSStore creates the base directory if needed and returns a store rooted there.
func NewFSStore(basePath string) (*FSStore, error) {
	if basePath == "" {
		return nil, errors.New("storage: base path is required")
	}
	if err := os.MkdirAll(basePath, 0o755); err != nil {
		return nil, fmt.Errorf("storage: create base path: %w", err)
	}
	return &FSStore{basePath: basePath}, nil
}

// Put writes the reader's bytes to a new blob and returns its metadata.
func (s *FSStore) Put(kind Kind, contentType string, ownerID string, r io.Reader) (Metadata, error) {
	if !kind.valid() {
		return Metadata{}, ErrInvalidKind
	}
	id, err := newID()
	if err != nil {
		return Metadata{}, err
	}

	blobPath := s.blobPath(id)
	f, err := os.Create(blobPath)
	if err != nil {
		return Metadata{}, fmt.Errorf("storage: create blob: %w", err)
	}
	size, err := io.Copy(f, r)
	if err != nil {
		_ = f.Close()
		_ = os.Remove(blobPath)
		return Metadata{}, fmt.Errorf("storage: write blob: %w", err)
	}
	if err := f.Close(); err != nil {
		_ = os.Remove(blobPath)
		return Metadata{}, fmt.Errorf("storage: close blob: %w", err)
	}

	meta := Metadata{
		ID:          id,
		Kind:        kind,
		ContentType: contentType,
		Size:        size,
		CreatedAt:   time.Now().UTC(),
		OwnerID:     ownerID,
	}
	if err := s.writeMeta(meta); err != nil {
		_ = os.Remove(blobPath)
		return Metadata{}, err
	}
	return meta, nil
}

// Get opens the blob for reading; the caller must Close the returned reader.
func (s *FSStore) Get(id string) (io.ReadCloser, Metadata, error) {
	meta, err := s.readMeta(id)
	if err != nil {
		return nil, Metadata{}, err
	}
	f, err := os.Open(s.blobPath(id))
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return nil, Metadata{}, ErrNotFound
		}
		return nil, Metadata{}, fmt.Errorf("storage: open blob: %w", err)
	}
	return f, meta, nil
}

// Stat returns metadata without opening the blob.
func (s *FSStore) Stat(id string) (Metadata, error) {
	return s.readMeta(id)
}

// Delete removes both the blob and its metadata.
func (s *FSStore) Delete(id string) error {
	if _, err := s.readMeta(id); err != nil {
		return err
	}
	if err := os.Remove(s.blobPath(id)); err != nil && !errors.Is(err, os.ErrNotExist) {
		return fmt.Errorf("storage: remove blob: %w", err)
	}
	if err := os.Remove(s.metaPath(id)); err != nil && !errors.Is(err, os.ErrNotExist) {
		return fmt.Errorf("storage: remove metadata: %w", err)
	}
	return nil
}

func (s *FSStore) writeMeta(meta Metadata) error {
	data, err := json.Marshal(meta)
	if err != nil {
		return fmt.Errorf("storage: marshal metadata: %w", err)
	}
	if err := os.WriteFile(s.metaPath(meta.ID), data, 0o644); err != nil {
		return fmt.Errorf("storage: write metadata: %w", err)
	}
	return nil
}

func (s *FSStore) readMeta(id string) (Metadata, error) {
	if !safeID(id) {
		return Metadata{}, ErrNotFound
	}
	data, err := os.ReadFile(s.metaPath(id))
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return Metadata{}, ErrNotFound
		}
		return Metadata{}, fmt.Errorf("storage: read metadata: %w", err)
	}
	var meta Metadata
	if err := json.Unmarshal(data, &meta); err != nil {
		return Metadata{}, fmt.Errorf("storage: unmarshal metadata: %w", err)
	}
	return meta, nil
}

func (s *FSStore) blobPath(id string) string { return filepath.Join(s.basePath, id+".bin") }
func (s *FSStore) metaPath(id string) string { return filepath.Join(s.basePath, id+".json") }

// safeID rejects ids that could escape the base directory.
func safeID(id string) bool {
	return id != "" && !strings.ContainsAny(id, `/\`) && !strings.Contains(id, "..")
}

func newID() (string, error) {
	buf := make([]byte, 16)
	if _, err := rand.Read(buf); err != nil {
		return "", fmt.Errorf("storage: generate id: %w", err)
	}
	return hex.EncodeToString(buf), nil
}
