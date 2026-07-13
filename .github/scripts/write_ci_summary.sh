#!/usr/bin/env bash
set -euo pipefail

summary_path="${GITHUB_STEP_SUMMARY:-}"

if [[ -z "$summary_path" ]]; then
  echo "GITHUB_STEP_SUMMARY is not available; skipping summary."
  exit 0
fi

channel="${DISTRIBUTION_CHANNEL:-GitHub Actions}"
status="${JOB_STATUS:-unknown}"
workflow="${WORKFLOW_NAME:-unknown}"
repository="${REPOSITORY:-unknown}"
branch="${BRANCH_NAME:-unknown}"
commit="${COMMIT_SHA:-unknown}"
actor="${ACTOR:-unknown}"
run_url="${RUN_URL:-}"
short_commit="${commit:0:8}"

{
  echo "## Ruuvi Android ${channel}"
  echo
  echo "| Field | Value |"
  echo "| --- | --- |"
  echo "| Status | ${status} |"
  echo "| Workflow | ${workflow} |"
  echo "| Repository | ${repository} |"
  echo "| Branch | ${branch} |"
  echo "| Commit | ${short_commit} |"
  echo "| Actor | ${actor} |"
  echo "| Run | ${run_url} |"
} >> "$summary_path"
