#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="${TARGET_DIR:-$ROOT_DIR/target}"

export PORT="${PORT:-3001}"
export APP_ENV="${APP_ENV:-prod}"
export APP_DATA_DIR="${APP_DATA_DIR:-$ROOT_DIR/data/prod}"
export APP_DB_PATH="${APP_DB_PATH:-$APP_DATA_DIR/personal-space-java.db}"
export APP_UPLOAD_DIR="${APP_UPLOAD_DIR:-$APP_DATA_DIR/uploads}"

mkdir -p "$APP_DATA_DIR" "$APP_UPLOAD_DIR" "$(dirname "$APP_DB_PATH")"

jar_path="${JAR_PATH:-}"
if [ -z "$jar_path" ]; then
    if [ ! -d "$TARGET_DIR" ]; then
        echo "未找到 $TARGET_DIR，请先执行：./mvnw -q -DskipTests package" >&2
        exit 1
    fi
    mapfile -t jars < <(find "$TARGET_DIR" -maxdepth 1 -type f -name '*.jar' ! -name '*.jar.original' | sort)
    if [ "${#jars[@]}" -eq 0 ]; then
        echo "未找到可运行 jar，请先执行：./mvnw -q -DskipTests package" >&2
        exit 1
    fi
    jar_path="${jars[0]}"
fi

if [ ! -f "$jar_path" ]; then
    echo "jar 不存在：$jar_path" >&2
    exit 1
fi

java_opts=()
if [ -n "${JAVA_OPTS:-}" ]; then
    read -r -a java_opts <<< "${JAVA_OPTS}"
fi

echo "Starting Personal Space Java Backend"
echo "JAR           : $jar_path"
echo "PORT          : $PORT"
echo "APP_ENV       : $APP_ENV"
echo "APP_DB_PATH   : $APP_DB_PATH"
echo "APP_UPLOAD_DIR: $APP_UPLOAD_DIR"

exec java "${java_opts[@]}" -jar "$jar_path"
