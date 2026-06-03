package storage_test

import (
	"bytes"
	"errors"
	"io"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/hakku/storage-server/internal/storage"
)

func newStore(t *testing.T) *storage.FSStore {
	t.Helper()
	store, err := storage.NewFSStore(t.TempDir())
	if err != nil {
		t.Fatalf("NewFSStore: %v", err)
	}
	return store
}

func TestFSStore_PutThenGet_RoundTrips(t *testing.T) {
	store := newStore(t)
	content := []byte("fake-png-bytes")

	meta, err := store.Put(storage.KindResult, "image/png", bytes.NewReader(content))
	if err != nil {
		t.Fatalf("Put: %v", err)
	}
	if meta.ID == "" {
		t.Fatal("expected non-empty id")
	}
	if meta.Kind != storage.KindResult {
		t.Errorf("kind = %q, want %q", meta.Kind, storage.KindResult)
	}
	if meta.ContentType != "image/png" {
		t.Errorf("contentType = %q, want image/png", meta.ContentType)
	}
	if meta.Size != int64(len(content)) {
		t.Errorf("size = %d, want %d", meta.Size, len(content))
	}
	if meta.CreatedAt.IsZero() {
		t.Error("expected createdAt to be set")
	}

	body, gotMeta, err := store.Get(meta.ID)
	if err != nil {
		t.Fatalf("Get: %v", err)
	}
	defer body.Close()
	got, err := io.ReadAll(body)
	if err != nil {
		t.Fatalf("ReadAll: %v", err)
	}
	if !bytes.Equal(got, content) {
		t.Errorf("body = %q, want %q", got, content)
	}
	if gotMeta.ID != meta.ID || gotMeta.ContentType != meta.ContentType || gotMeta.Size != meta.Size {
		t.Errorf("Get metadata = %+v, want %+v", gotMeta, meta)
	}
}

func TestFSStore_Stat_ReturnsMetadataWithoutBody(t *testing.T) {
	store := newStore(t)
	meta, err := store.Put(storage.KindRaw, "image/jpeg", strings.NewReader("jpeg"))
	if err != nil {
		t.Fatalf("Put: %v", err)
	}

	got, err := store.Stat(meta.ID)
	if err != nil {
		t.Fatalf("Stat: %v", err)
	}
	if got.ID != meta.ID || got.Kind != storage.KindRaw || got.Size != 4 {
		t.Errorf("Stat = %+v, want id=%s kind=raw size=4", got, meta.ID)
	}
}

func TestFSStore_Delete_RemovesImage(t *testing.T) {
	store := newStore(t)
	meta, err := store.Put(storage.KindRaw, "image/png", strings.NewReader("x"))
	if err != nil {
		t.Fatalf("Put: %v", err)
	}

	if err := store.Delete(meta.ID); err != nil {
		t.Fatalf("Delete: %v", err)
	}

	if _, _, err := store.Get(meta.ID); !errors.Is(err, storage.ErrNotFound) {
		t.Errorf("Get after delete err = %v, want ErrNotFound", err)
	}
	if _, err := store.Stat(meta.ID); !errors.Is(err, storage.ErrNotFound) {
		t.Errorf("Stat after delete err = %v, want ErrNotFound", err)
	}
}

func TestFSStore_UnknownID_ReturnsNotFound(t *testing.T) {
	store := newStore(t)

	if _, _, err := store.Get("nope"); !errors.Is(err, storage.ErrNotFound) {
		t.Errorf("Get err = %v, want ErrNotFound", err)
	}
	if _, err := store.Stat("nope"); !errors.Is(err, storage.ErrNotFound) {
		t.Errorf("Stat err = %v, want ErrNotFound", err)
	}
	if err := store.Delete("nope"); !errors.Is(err, storage.ErrNotFound) {
		t.Errorf("Delete err = %v, want ErrNotFound", err)
	}
}

func TestFSStore_InvalidKind_Rejected(t *testing.T) {
	store := newStore(t)

	if _, err := store.Put(storage.Kind("bogus"), "image/png", strings.NewReader("x")); !errors.Is(err, storage.ErrInvalidKind) {
		t.Errorf("Put invalid kind err = %v, want ErrInvalidKind", err)
	}
}

func TestNewFSStore_EmptyBasePath_Errors(t *testing.T) {
	if _, err := storage.NewFSStore(""); err == nil {
		t.Error("expected error for empty base path")
	}
}

func TestNewFSStore_BasePathIsFile_Errors(t *testing.T) {
	file := filepath.Join(t.TempDir(), "not-a-dir")
	if err := os.WriteFile(file, []byte("x"), 0o644); err != nil {
		t.Fatalf("setup: %v", err)
	}
	if _, err := storage.NewFSStore(file); err == nil {
		t.Error("expected error when base path is an existing file")
	}
}

// errReader fails partway to exercise Put's write-error cleanup path.
type errReader struct{}

func (errReader) Read([]byte) (int, error) { return 0, errors.New("boom") }

func TestFSStore_Put_ReaderError_CleansUp(t *testing.T) {
	base := t.TempDir()
	store, err := storage.NewFSStore(base)
	if err != nil {
		t.Fatalf("NewFSStore: %v", err)
	}

	if _, err := store.Put(storage.KindRaw, "image/png", errReader{}); err == nil {
		t.Fatal("expected error from failing reader")
	}

	entries, err := os.ReadDir(base)
	if err != nil {
		t.Fatalf("ReadDir: %v", err)
	}
	if len(entries) != 0 {
		t.Errorf("expected base dir empty after failed Put, found %d entries", len(entries))
	}
}

func TestFSStore_DistinctIDsPerPut(t *testing.T) {
	store := newStore(t)
	a, err := store.Put(storage.KindRaw, "image/png", strings.NewReader("a"))
	if err != nil {
		t.Fatalf("Put a: %v", err)
	}
	b, err := store.Put(storage.KindRaw, "image/png", strings.NewReader("b"))
	if err != nil {
		t.Fatalf("Put b: %v", err)
	}
	if a.ID == b.ID {
		t.Errorf("expected distinct ids, both = %s", a.ID)
	}
}
