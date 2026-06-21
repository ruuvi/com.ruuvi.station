#!/usr/bin/env bash
set -euo pipefail

build_gradle="${1:-app/build.gradle}"

version_name="$(sed -n 's/^[[:space:]]*versionName "\([^"]*\)".*/\1/p' "$build_gradle" | head -n 1)"
version_code="${ANDROID_VERSION_CODE:-}"

if [[ -z "$version_name" ]]; then
  echo "::error::Could not read versionName from ${build_gradle}"
  exit 1
fi

if [[ -z "$version_code" ]]; then
  echo "::error::ANDROID_VERSION_CODE is required; run .github/scripts/set_android_build_metadata.sh first in CI or export it locally"
  exit 1
fi

if [[ ! "$version_code" =~ ^[1-9][0-9]*$ ]]; then
  echo "::error::ANDROID_VERSION_CODE must be a positive integer"
  exit 1
fi

echo "RUUVI_APP_VERSION=${version_name}"
echo "RUUVI_BUILD_NUMBER=${version_code}"

if [[ -n "${GITHUB_ENV:-}" ]]; then
  {
    echo "RUUVI_APP_VERSION=${version_name}"
    echo "RUUVI_BUILD_NUMBER=${version_code}"
  } >> "$GITHUB_ENV"
fi
