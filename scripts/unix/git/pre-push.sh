#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
backend_style_log="$(mktemp)"
backend_tests_log="$(mktemp)"
secret_scan_log="$(mktemp)"
frontend_style_log="$(mktemp)"
frontend_tests_log="$(mktemp)"
frontend_build_log="$(mktemp)"
cleanup() { rm -f "$backend_style_log" "$backend_tests_log" "$secret_scan_log" "$frontend_style_log" "$frontend_tests_log" "$frontend_build_log"; }
trap cleanup EXIT
reset='\033[0m'
cyan='\033[36m'
green='\033[32m'
red='\033[31m'
yellow='\033[33m'

print_box() {
  local title="$1"
  local width=84
  local inner_width=$((width - 4))
  local border
  local -a lines=()
  local current=""
  local word
  for word in $title; do
    if [ -z "$current" ]; then
      current="$word"
    elif [ $(( ${#current} + 1 + ${#word} )) -le "$inner_width" ]; then
      current="$current $word"
    else
      lines+=("$current")
      current="$word"
    fi
  done
  if [ -n "$current" ]; then
    lines+=("$current")
  fi
  if [ ${#lines[@]} -eq 0 ]; then
    lines+=("")
  fi
  border="||$(printf '%0.s=' $(seq 1 $((width - 4))))||"
  printf '\n%s\n' "$border"
  for line in "${lines[@]}"; do
    printf '|| %-*s ||\n' "$inner_width" "$line"
  done
  printf '%s\n\n' "$border"
}

print_summary_border() {
  local color="${1:-$cyan}"
  local width=132
  printf '%b||%s||%b\n' "$color" "$(printf '%0.s=' $(seq 1 $((width - 4))))" "$reset"
}

print_summary_divider() {
  local color="${1:-$cyan}"
  local width=132
  printf '%b||%s||%b\n' "$color" "$(printf '%0.s-' $(seq 1 $((width - 4))))" "$reset"
}

status_color() {
  case "$1" in
    PASS) printf '%b' "$green" ;;
    FAIL) printf '%b' "$red" ;;
    SKIPPED) printf '%b' "$yellow" ;;
    *) printf '%b' "$yellow" ;;
  esac
}

status_badge() {
  case "$1" in
    PASS) printf '[PASS]' ;;
    FAIL) printf '[FAIL]' ;;
    SKIPPED) printf '[SKIP]' ;;
    *) printf '[%s]' "$1" ;;
  esac
}

truncate_text() {
  local text="$1"
  local max_len="$2"
  if [ "${#text}" -gt "$max_len" ]; then
    printf '%s...' "${text:0:max_len-3}"
  else
    printf '%s' "$text"
  fi
}

print_summary_row() {
  local label="$1"
  local status="$2"
  local details="$3"
  local color badge text
  color="$(status_color "$status")"
  badge="$(status_badge "$status")"
  label="$(truncate_text "$label" 30)"
  details="$(truncate_text "$details" 73)"
  text="$(printf '|| %-30s | %-10s | %-73s ||' "$label" "$badge" "$details")"
  printf '%b%s%b\n' "$color" "$text" "$reset"
}

backend_style_status="SKIPPED"
backend_tests_status="SKIPPED"
secret_scan_status="SKIPPED"
frontend_style_status="SKIPPED"
frontend_tests_status="SKIPPED"
frontend_build_status="SKIPPED"

backend_tests_summary=""
frontend_tests_summary=""

print_box "Running backend style: formatting (Spotless), API checks (Checkstyle)..."
if "$repo_root/scripts/unix/quality/backend-style.sh" 2>&1 | tee "$backend_style_log"; then
  backend_style_status="PASS"
else
  backend_style_status="FAIL"
fi

if [ "$backend_style_status" = "PASS" ]; then
  print_box "Running backend tests: unit tests (JUnit, Mockito), integration tests (Testcontainers, Docker)..."
  if "$repo_root/scripts/unix/quality/backend-tests.sh" 2>&1 | tee "$backend_tests_log"; then
    backend_tests_status="PASS"
  else
    backend_tests_status="FAIL"
  fi
fi

if [ "$backend_tests_status" = "PASS" ]; then
  print_box "Running secret scan: repository secrets scan (Gitleaks)..."
  if "$repo_root/scripts/unix/git/scan-secrets.sh" 2>&1 | tee "$secret_scan_log"; then
    secret_scan_status="PASS"
  else
    secret_scan_status="FAIL"
  fi
fi

if [ "$secret_scan_status" = "PASS" ]; then
  print_box "Running frontend style: linting (ESLint), formatting (Prettier)..."
  if "$repo_root/scripts/unix/quality/frontend-style.sh" 2>&1 | tee "$frontend_style_log"; then
    frontend_style_status="PASS"
  else
    frontend_style_status="FAIL"
  fi
fi

if [ "$frontend_style_status" = "PASS" ]; then
  print_box "Running frontend tests: component tests (Vitest, Vue Test Utils, JSDOM)..."
  if "$repo_root/scripts/unix/quality/frontend-tests.sh" 2>&1 | tee "$frontend_tests_log"; then
    frontend_tests_status="PASS"
  else
    frontend_tests_status="FAIL"
  fi
fi

if [ "$frontend_tests_status" = "PASS" ]; then
  pushd "$repo_root/frontend" >/dev/null
  print_box "Building frontend: typecheck and production bundle (Vue TSC, Vite)..."
  if npm run build 2>&1 | tee "$frontend_build_log"; then
    frontend_build_status="PASS"
  else
    frontend_build_status="FAIL"
  fi
  popd >/dev/null
fi

if [ -s "$backend_tests_log" ]; then
  backend_tests_summary="$(grep -Eo 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+' "$backend_tests_log" | tail -n 1 || true)"
fi
if [ -s "$frontend_tests_log" ]; then
  frontend_tests_summary="$(node -e 'const fs=require("fs"); const p=process.argv[1]; if (fs.existsSync(p)) { const t=fs.readFileSync(p, "utf8").replace(/\u001b\[[0-9;]*[A-Za-z]/g,"").replace(/\r/g,""); const m=t.match(/Test Files\s+(\d+)\s+passed(?:\s+\((\d+)\))?.*?Tests\s+(\d+)\s+passed(?:\s+\((\d+)\))?/s); if (m) { console.log(`Test Files ${m[1]} passed (${m[2] || m[1]})   Tests ${m[3]} passed (${m[4] || m[3]})`); } }' "$frontend_tests_log" 2>/dev/null || true)"
fi

print_box "Summary"
summary_color="$green"
if [ "$backend_style_status" = "FAIL" ] || [ "$backend_tests_status" = "FAIL" ] || [ "$secret_scan_status" = "FAIL" ] || [ "$frontend_style_status" = "FAIL" ] || [ "$frontend_tests_status" = "FAIL" ] || [ "$frontend_build_status" = "FAIL" ]; then
  summary_color="$red"
elif [ "$backend_style_status" = "SKIPPED" ] || [ "$backend_tests_status" = "SKIPPED" ] || [ "$secret_scan_status" = "SKIPPED" ] || [ "$frontend_style_status" = "SKIPPED" ] || [ "$frontend_tests_status" = "SKIPPED" ] || [ "$frontend_build_status" = "SKIPPED" ]; then
  summary_color="$yellow"
fi

print_summary_border "$summary_color"
printf '%b|| %-24s | %-10s | %-65s ||%b\n' "$summary_color" "Step" "Status" "Details" "$reset"
print_summary_divider "$summary_color"
print_summary_row "backend style" "$backend_style_status" ""
print_summary_row "backend tests" "$backend_tests_status" "$backend_tests_summary"
print_summary_row "secret scan" "$secret_scan_status" ""
print_summary_row "frontend style" "$frontend_style_status" ""
print_summary_row "frontend tests" "$frontend_tests_status" "$frontend_tests_summary"
print_summary_row "frontend build" "$frontend_build_status" ""
print_summary_border "$summary_color"

if [ "$backend_style_status" != "PASS" ] || [ "$backend_tests_status" != "PASS" ] || [ "$secret_scan_status" != "PASS" ] || [ "$frontend_style_status" != "PASS" ] || [ "$frontend_tests_status" != "PASS" ] || [ "$frontend_build_status" != "PASS" ]; then
  exit 1
fi
