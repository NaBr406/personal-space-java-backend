#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export PORT="${PORT:-3001}"
export APP_ENV="${APP_ENV:-sandbox}"
export APP_DATA_DIR="${APP_DATA_DIR:-$ROOT_DIR/data/sandbox}"
export APP_DB_PATH="${APP_DB_PATH:-$APP_DATA_DIR/personal-space-java-sandbox.db}"
export APP_UPLOAD_DIR="${APP_UPLOAD_DIR:-$APP_DATA_DIR/uploads}"

mkdir -p "$APP_DATA_DIR" "$APP_UPLOAD_DIR" "$(dirname "$APP_DB_PATH")"

echo "Starting Personal Space Java Backend (sandbox)"
echo "PORT          : $PORT"
echo "APP_ENV       : $APP_ENV"
echo "APP_DB_PATH   : $APP_DB_PATH"
echo "APP_UPLOAD_DIR: $APP_UPLOAD_DIR"

exec "$ROOT_DIR/mvnw" spring-boot:run
