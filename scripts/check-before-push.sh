#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
backend_tests_log="$(mktemp)"
backend_coverage_log="$(mktemp)"
backend_style_log="$(mktemp)"
secret_scan_log="$(mktemp)"
frontend_tests_log="$(mktemp)"
frontend_coverage_log="$(mktemp)"
frontend_style_log="$(mktemp)"
frontend_build_log="$(mktemp)"
cleanup() { rm -f "$backend_style_log" "$backend_tests_log" "$backend_coverage_log" "$secret_scan_log" "$frontend_style_log" "$frontend_tests_log" "$frontend_coverage_log" "$frontend_build_log"; }
trap cleanup EXIT

print_box() {
  local title="$1"
  local width=62
  local inner_width=$((width - 4))
  local border
  border="||$(printf '%0.s=' $(seq 1 $((width - 4))))||"
  printf '\n%s\n' "$border"
  printf '|| %-*s ||\n' "$inner_width" "$title"
  printf '%s\n\n' "$border"
}

backend_style_status="SKIPPED"
backend_coverage_status="SKIPPED"
secret_scan_status="SKIPPED"
frontend_style_status="SKIPPED"
frontend_tests_status="SKIPPED"
frontend_coverage_status="SKIPPED"
frontend_build_status="SKIPPED"

backend_tests_summary=""
backend_coverage_summary=""
frontend_tests_summary=""
frontend_coverage_summary=""

print_box "Running backend style checks..."
if "$repo_root/scripts/backend/check-style.sh" 2>&1 | tee "$backend_style_log"; then
  backend_style_status="PASS"
else
  backend_style_status="FAIL"
fi

if [ "$backend_style_status" = "PASS" ]; then
  print_box "Running backend tests and coverage..."
  if "$repo_root/scripts/backend/check-coverage.sh" 2>&1 | tee "$backend_coverage_log"; then
    backend_coverage_status="PASS"
  else
    backend_coverage_status="FAIL"
  fi
fi

if [ "$backend_coverage_status" = "PASS" ]; then
  print_box "Running secret scan..."
  if "$repo_root/scripts/check-secrets.sh" 2>&1 | tee "$secret_scan_log"; then
    secret_scan_status="PASS"
  else
    secret_scan_status="FAIL"
  fi
fi

if [ "$secret_scan_status" = "PASS" ]; then
  print_box "Running frontend style checks..."
  if "$repo_root/scripts/frontend/check-style.sh" 2>&1 | tee "$frontend_style_log"; then
    frontend_style_status="PASS"
  else
    frontend_style_status="FAIL"
  fi
fi

if [ "$frontend_style_status" = "PASS" ]; then
  print_box "Running frontend tests..."
  if "$repo_root/scripts/frontend/test-frontend.sh" 2>&1 | tee "$frontend_tests_log"; then
    frontend_tests_status="PASS"
  else
    frontend_tests_status="FAIL"
  fi
fi

if [ "$frontend_tests_status" = "PASS" ]; then
  print_box "Running frontend coverage..."
  if "$repo_root/scripts/frontend/check-coverage.sh" 2>&1 | tee "$frontend_coverage_log"; then
    frontend_coverage_status="PASS"
  else
    frontend_coverage_status="FAIL"
  fi
fi

if [ "$frontend_coverage_status" = "PASS" ]; then
  pushd "$repo_root/frontend" >/dev/null
  print_box "Building frontend..."
  if npm run build 2>&1 | tee "$frontend_build_log"; then
    frontend_build_status="PASS"
  else
    frontend_build_status="FAIL"
  fi
  popd >/dev/null
fi

if [ -s "$backend_coverage_log" ]; then
  backend_tests_summary="$(grep -Eo 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+' "$backend_coverage_log" | tail -n 1 || true)"
fi
if [ -f "$repo_root/backend/coverage/target/site/jacoco-aggregate/jacoco.csv" ]; then
  backend_coverage_summary="$(awk -F, 'NR>1 { branch_missed += $6; branch_covered += $7; line_missed += $8; line_covered += $9 } END { if (line_missed + line_covered > 0) printf "lines %.2f%%, branches %.2f%%", (line_covered / (line_missed + line_covered)) * 100, (branch_covered / (branch_missed + branch_covered)) * 100 }' "$repo_root/backend/coverage/target/site/jacoco-aggregate/jacoco.csv" 2>/dev/null || true)"
fi
if [ -s "$frontend_tests_log" ]; then
  frontend_tests_summary="$(grep -Eo 'Test Files[[:space:]]+[0-9]+[[:space:]]+passed.*Tests[[:space:]]+[0-9]+[[:space:]]+passed' "$frontend_tests_log" | tail -n 1 || true)"
fi
if [ -f "$repo_root/frontend/coverage/coverage-summary.json" ]; then
  frontend_coverage_summary="$(node -e 'const fs=require("fs"); const p=process.argv[1]; if (fs.existsSync(p)) { const s=JSON.parse(fs.readFileSync(p, "utf8")).total; console.log(`lines ${s.lines.pct}%, branches ${s.branches.pct}%`); }' "$repo_root/frontend/coverage/coverage-summary.json" 2>/dev/null || true)"
fi

print_box "Summary"
printf -- '  [%-7s] backend style\n' "$backend_style_status"
printf -- '\n  [%-7s] backend quality %s %s\n' "$backend_coverage_status" "${backend_tests_summary:-}" "${backend_coverage_summary:-}"
printf -- '\n  [%-7s] secret scan\n' "$secret_scan_status"
printf -- '\n  [%-7s] frontend style\n' "$frontend_style_status"
printf -- '\n  [%-7s] frontend tests %s\n' "$frontend_tests_status" "${frontend_tests_summary:-}"
printf -- '\n  [%-7s] frontend coverage %s\n' "$frontend_coverage_status" "${frontend_coverage_summary:-}"
printf -- '\n  [%-7s] frontend build\n' "$frontend_build_status"

if [ "$backend_style_status" != "PASS" ] || [ "$backend_coverage_status" != "PASS" ] || [ "$secret_scan_status" != "PASS" ] || [ "$frontend_style_status" != "PASS" ] || [ "$frontend_tests_status" != "PASS" ] || [ "$frontend_coverage_status" != "PASS" ] || [ "$frontend_build_status" != "PASS" ]; then
  exit 1
fi
