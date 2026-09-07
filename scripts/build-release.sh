#!/usr/bin/env bash
set -euo pipefail

GRADLE_BIN="${GRADLE_BIN:-gradle}"
"$GRADLE_BIN" --no-daemon clean assembleRelease

printf '\nRelease output:\n'
find app/build/outputs/apk/release -maxdepth 1 -type f -name '*.apk' -print
