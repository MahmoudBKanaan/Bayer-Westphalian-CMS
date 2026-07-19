#!/bin/sh
set -eu

# Named volume mounts often arrive as root:root. Consent evidence storage must be
# writable by the non-root spring user for production readiness.
STORAGE_ROOT="${FILE_STORAGE_LOCAL_PATH:-/app/data/consent-evidence}"
if [ -d "$STORAGE_ROOT" ] || mkdir -p "$STORAGE_ROOT" 2>/dev/null; then
  chown -R spring:spring "$STORAGE_ROOT" 2>/dev/null || true
  chmod -R u+rwX "$STORAGE_ROOT" 2>/dev/null || true
fi

# Heap/container flags come from JAVA_TOOL_OPTIONS (Compose) and optional JAVA_OPTS.
# Do not default to an unbounded heap — that triggers OOM kills (exit 137) on small Docker hosts.
# shellcheck disable=SC2086
exec su-exec spring:spring java ${JAVA_OPTS:-} -jar /app/app.jar
