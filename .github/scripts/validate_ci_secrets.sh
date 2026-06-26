#!/usr/bin/env bash
set -euo pipefail

missing=()

for name in "$@"; do
  if [[ -z "${!name:-}" ]]; then
    missing+=("$name")
  fi
done

if (( ${#missing[@]} > 0 )); then
  echo "::error::Missing required CI secrets or environment values: ${missing[*]}"
  printf 'Missing values:\n'
  printf -- '- %s\n' "${missing[@]}"
  exit 1
fi

echo "Required CI secrets are present."
