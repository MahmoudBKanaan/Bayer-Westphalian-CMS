# Bayer-Westphalian Campaign Management Platform — frontend image
# Sprint 17 item 686 — Docker frontend image build
#
# Multi-stage: Node 22 production build → nginx static SPA host. No API secrets baked in.

# --- Build stage ---
FROM node:22-alpine AS build
WORKDIR /app

# Keep Vite cache inside the image layer (vite.config defaults to a parent cache dir).
ENV VITE_CACHE_DIR=/app/.vite-cache

COPY package.json package-lock.json ./
RUN npm ci

COPY index.html ./
COPY tsconfig.json tsconfig.app.json tsconfig.node.json ./
COPY vite.config.ts ./
COPY eslint.config.js ./
COPY public ./public
COPY src ./src

# Production Vite/TypeScript build (lint/test stay in dedicated CI jobs 682–683).
RUN npm run build

# --- Runtime stage ---
FROM nginx:1.27-alpine AS runtime

# SPA config: client-side routing via try_files → index.html
COPY nginx.docker.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

# Default nginx entrypoint/cmd serve static assets on port 80.
