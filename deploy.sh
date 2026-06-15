#!/usr/bin/env bash
#
# Unified deployment script.
#
# Coordinates the front/back-separated build:
#   1. builds the Vue frontend (Vue CLI)
#   2. bundles the build output into Spring Boot's static/ directory
#   3. builds and starts the full stack (MySQL + Spring Boot) via docker compose
#   4. waits for the backend to become reachable
#
# Usage: ./deploy.sh [profile]      (profile defaults to "prod")
#
# Secrets (DB_PASSWORD) are read from the gitignored .env file at the repo root;
# copy .env.example to .env first.
set -euo pipefail

PROFILE="${1:-prod}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$ROOT_DIR/vueblog"
BACKEND_DIR="$ROOT_DIR/blogserver"
STATIC_DIR="$BACKEND_DIR/src/main/resources/static"

log() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
err() { printf '\033[1;31mERROR:\033[0m %s\n' "$*" >&2; }

# --- Prerequisite checks -----------------------------------------------------
for cmd in node npm docker curl; do
  command -v "$cmd" >/dev/null 2>&1 || { err "required command not found: $cmd"; exit 1; }
done
if docker compose version >/dev/null 2>&1; then
  DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
else
  err "neither 'docker compose' nor 'docker-compose' is available"; exit 1
fi

# --- 1. Build the frontend ---------------------------------------------------
log "[1/4] Building frontend (Vue CLI)"
cd "$FRONTEND_DIR"
npm install
npm run build

# --- 2. Bundle the frontend into Spring Boot's static directory --------------
log "[2/4] Bundling frontend into Spring Boot static/"
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR"
cp -r "$FRONTEND_DIR/dist/." "$STATIC_DIR/"

# --- 3. Build and start the stack -------------------------------------------
log "[3/4] Building & starting containers (SPRING_PROFILES_ACTIVE=$PROFILE)"
cd "$ROOT_DIR"
export SPRING_PROFILES_ACTIVE="$PROFILE"
$DC up -d --build

# --- 4. Health check ---------------------------------------------------------
log "[4/4] Waiting for backend to become reachable on :8081"
for _ in $(seq 1 40); do
  # Any HTTP response (even 302/401) means the app is up and responding.
  if curl -s -o /dev/null -m 3 "http://localhost:8081/"; then
    log "Backend is up. Deployment complete."
    exit 0
  fi
  sleep 3
done
err "Backend did not become reachable within the timeout."
$DC ps
exit 1
