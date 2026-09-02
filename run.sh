#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if ! printf '%s' "${LANG:-}${LC_ALL:-}" | grep -qi utf; then
  export LANG=en_AU.UTF-8
  export LC_ALL=en_AU.UTF-8
fi
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

# compile first — required on a clean checkout (same as GitHub Actions)
exec mvn -q compile exec:java -Dexec.cleanupDaemonThreads=false
