#!/usr/bin/env sh
# Compatibility entrypoint. Use the documented transitional Gradle task.
set -eu
exec "$(dirname "$0")/gradlew" legacyDemo "$@"
