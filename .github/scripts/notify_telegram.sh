#!/usr/bin/env bash
set -euo pipefail

telegram_bot_token="${TELEGRAM_BOT_TOKEN:-}"
telegram_chat_id="${TELEGRAM_CHAT_ID:-}"

telegram_bot_token="${telegram_bot_token//[[:space:]]/}"
telegram_chat_id="${telegram_chat_id//[[:space:]]/}"

if [[ -z "$telegram_bot_token" || -z "$telegram_chat_id" ]]; then
  echo "Telegram secrets are not set; skipping notification."
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

if [[ "$status" != "success" ]]; then
  echo "Job status is ${status}; skipping Telegram notification."
  exit 0
fi

if [[ -n "${TELEGRAM_SUCCESS_TITLE:-}" ]]; then
  message="${TELEGRAM_SUCCESS_TITLE}"

  if [[ -n "${TELEGRAM_CHANGELOG_FILE:-}" && -f "${TELEGRAM_CHANGELOG_FILE}" ]]; then
    changelog="$(cat "${TELEGRAM_CHANGELOG_FILE}")"
    if [[ -n "$changelog" ]]; then
      message="${message}
Changelog:
${changelog}"
    else
      echo "Telegram changelog file is empty: ${TELEGRAM_CHANGELOG_FILE}"
    fi
  elif [[ -n "${TELEGRAM_CHANGELOG_FILE:-}" ]]; then
    echo "Telegram changelog file was not found: ${TELEGRAM_CHANGELOG_FILE}"
  fi

  if [[ -n "${TELEGRAM_SUCCESS_VERSION:-}" ]]; then
    message="${message}
Version: ${TELEGRAM_SUCCESS_VERSION}"
  fi

  if [[ -n "${TELEGRAM_SUCCESS_BUILD:-}" ]]; then
    message="${message}
Build: ${TELEGRAM_SUCCESS_BUILD}"
  fi

  if [[ -n "${TELEGRAM_SUCCESS_ROLLOUT:-}" ]]; then
    message="${message}
Rollout: ${TELEGRAM_SUCCESS_ROLLOUT}"
  fi

  if [[ -n "${TELEGRAM_SUCCESS_MENTION:-}" ]]; then
    message="${message}
${TELEGRAM_SUCCESS_MENTION}"
  fi
else
  message="Ruuvi Android ${channel}: ${status}
Workflow: ${workflow}
Repository: ${repository}
Branch: ${branch}
Commit: ${short_commit}
Actor: ${actor}
Run: ${run_url}"

  if [[ -n "${TELEGRAM_SUCCESS_MENTION:-}" ]]; then
    message="${message}
${TELEGRAM_SUCCESS_MENTION}"
  fi
fi

max_message_length=3900
if (( ${#message} > max_message_length )); then
  message="${message:0:max_message_length}
..."
fi

curl_args=(
  -sS
  -X POST
  "https://api.telegram.org/bot${telegram_bot_token}/sendMessage"
  -d "chat_id=${telegram_chat_id}"
  --data-urlencode "text=${message}"
  -d "disable_web_page_preview=true"
)

if ! response="$(curl "${curl_args[@]}" -w $'\n%{http_code}')"; then
  echo "Telegram notification request failed; continuing."
  exit 0
fi

http_status="${response##*$'\n'}"
response_body="${response%$'\n'*}"

if [[ "$http_status" -lt 200 || "$http_status" -ge 300 ]]; then
  echo "Telegram notification failed with HTTP ${http_status}: ${response_body}"
  echo "Continuing without failing the workflow."
fi
