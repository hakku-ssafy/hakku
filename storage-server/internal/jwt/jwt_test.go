package jwt

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"testing"
	"time"
)

func sign(t *testing.T, key []byte, claims map[string]any) string {
	t.Helper()
	header := base64.RawURLEncoding.EncodeToString([]byte(`{"alg":"HS256","typ":"JWT"}`))
	payloadJSON, err := json.Marshal(claims)
	if err != nil {
		t.Fatalf("marshal claims: %v", err)
	}
	payload := base64.RawURLEncoding.EncodeToString(payloadJSON)
	mac := hmac.New(sha256.New, key)
	mac.Write([]byte(header + "." + payload))
	sig := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	return header + "." + payload + "." + sig
}

func TestSubject_AcceptsAccessToken(t *testing.T) {
	key := []byte("test-key-0123456789abcdef")
	v := New(key)
	tok := sign(t, key, map[string]any{"sub": "42", "type": "access", "exp": time.Now().Add(time.Hour).Unix()})

	sub, err := v.Subject(tok)
	if err != nil || sub != "42" {
		t.Fatalf("access token: sub=%q err=%v, want 42/nil", sub, err)
	}
}

// A refresh token shares the secret and is signature/expiry-valid, but must NOT be
// usable as a Bearer access token on the storage server.
func TestSubject_RejectsRefreshToken(t *testing.T) {
	key := []byte("test-key-0123456789abcdef")
	v := New(key)
	tok := sign(t, key, map[string]any{"sub": "42", "type": "refresh", "exp": time.Now().Add(24 * time.Hour).Unix()})

	if _, err := v.Subject(tok); err == nil {
		t.Fatal("refresh token was accepted as an access token, want error")
	}
}
