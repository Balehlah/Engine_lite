#!/usr/bin/env sh
# Compatibility entrypoint. Gradle Wrapper is the source of truth.
set -eu
exec "$(dirname "$0")/gradlew" clean test "$@"
