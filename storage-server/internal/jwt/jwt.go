// Package jwt provides JWT validation for the storage server.
package jwt

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"strings"
	"time"
)

// HS256Validator validates HS256-signed JWT tokens.
// The key must be the raw decoded bytes of the base64-encoded JWT_SECRET.
type HS256Validator struct {
	key []byte
}

// New returns an HS256Validator using the given raw key bytes.
func New(key []byte) *HS256Validator {
	return &HS256Validator{key: key}
}

type jwtPayload struct {
	Sub  string `json:"sub"`
	Exp  int64  `json:"exp"`
	Type string `json:"type"`
}

// Subject validates the token signature and expiry, then returns the subject claim.
func (v *HS256Validator) Subject(token string) (string, error) {
	parts := strings.SplitN(token, ".", 3)
	if len(parts) != 3 {
		return "", errors.New("malformed token")
	}

	sig, err := base64.RawURLEncoding.DecodeString(parts[2])
	if err != nil {
		return "", errors.New("invalid signature encoding")
	}
	mac := hmac.New(sha256.New, v.key)
	mac.Write([]byte(parts[0] + "." + parts[1]))
	if !hmac.Equal(sig, mac.Sum(nil)) {
		return "", errors.New("invalid signature")
	}

	payload, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return "", errors.New("invalid payload encoding")
	}
	var c jwtPayload
	if err := json.Unmarshal(payload, &c); err != nil {
		return "", errors.New("invalid payload")
	}
	if c.Exp > 0 && time.Now().Unix() > c.Exp {
		return "", errors.New("token expired")
	}
	// 리프레시 토큰(24h, type=refresh)은 시크릿을 공유해 서명·만료가 유효하지만, Bearer 액세스 토큰으로 받아서는 안 된다.
	if c.Type == "refresh" {
		return "", errors.New("refresh token not accepted as access token")
	}
	if c.Sub == "" {
		return "", errors.New("missing subject")
	}
	return c.Sub, nil
}
