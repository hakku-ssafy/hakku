# ─── Build stage ───────────────────────────────────────────────────────────────
FROM golang:1.26-alpine AS builder

WORKDIR /src

# Dependencies first for layer caching
COPY go.mod go.sum ./
RUN go mod download

# Copy source and build
COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o /storage-server ./cmd/storage-server

# ─── Runtime stage ─────────────────────────────────────────────────────────────
FROM alpine:3.21 AS runtime

RUN apk add --no-cache ca-certificates wget

COPY --from=builder /storage-server /storage-server

EXPOSE 8080

CMD ["/storage-server"]
