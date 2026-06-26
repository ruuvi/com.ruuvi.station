#!/usr/bin/env bash
set -euo pipefail

build_gradle="${1:-app/build.gradle}"
play_version_code_max=2100000000
epoch_2020_utc=1577836800
version_code_slots_per_second=3

version_name="$(sed -n 's/^[[:space:]]*versionName "\([^"]*\)".*/\1/p' "$build_gradle" | head -n 1)"
if [[ -z "$version_name" ]]; then
  echo "::error::Could not read versionName from ${build_gradle}"
  exit 1
fi

now_utc="$(date -u +%s)"
elapsed_seconds=$((now_utc - epoch_2020_utc))
run_slot=0
if [[ -n "${GITHUB_RUN_ID:-}" ]]; then
  run_slot=$((GITHUB_RUN_ID % version_code_slots_per_second))
fi
version_code=$((elapsed_seconds * version_code_slots_per_second + run_slot))

if (( version_code <= 0 || version_code >= play_version_code_max )); then
  echo "::error::Generated Android versionCode ${version_code} is outside the Google Play range"
  exit 1
fi

echo "ANDROID_VERSION_CODE=${version_code}"
echo "RUUVI_APP_VERSION=${version_name}"
echo "RUUVI_BUILD_NUMBER=${version_code}"

if [[ -n "${GITHUB_ENV:-}" ]]; then
  {
    echo "ANDROID_VERSION_CODE=${version_code}"
    echo "RUUVI_APP_VERSION=${version_name}"
    echo "RUUVI_BUILD_NUMBER=${version_code}"
  } >> "$GITHUB_ENV"
fi
