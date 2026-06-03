# ─── Build stage ───────────────────────────────────────────────────────────────
FROM node:22-alpine AS builder

WORKDIR /app

# Dependencies first for layer caching
COPY package*.json ./
RUN npm ci

# Copy source and build
COPY . .

RUN npm run build

# ─── Runtime stage ─────────────────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# SPA nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
