#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/lib/common.sh"

mode="${1:-all}"
mode_lc="$(printf '%s' "$mode" | tr '[:upper:]' '[:lower:]')"
case "$mode_lc" in
  all|full)
    run_backend=1
    run_frontend=1
    run_secret_scan=1
    ;;
  be|backend)
    run_backend=1
    run_frontend=0
    run_secret_scan=0
    ;;
  fe|frontend)
    run_backend=0
    run_frontend=1
    run_secret_scan=0
    ;;
  *)
    printf 'Unknown run mode "%s". Use all, be, or fe.\n' "$mode" >&2
    exit 2
    ;;
esac

backend_style_log="$(mktemp)"
backend_tests_log="$(mktemp)"
secret_scan_log="$(mktemp)"
frontend_style_log="$(mktemp)"
frontend_tests_log="$(mktemp)"
frontend_e2e_log="$(mktemp)"
frontend_build_log="$(mktemp)"
cleanup() {
  rm -f "$backend_style_log" "$backend_tests_log" "$secret_scan_log" "$frontend_style_log" "$frontend_tests_log" "$frontend_e2e_log" "$frontend_build_log"
}
trap cleanup EXIT
reset='\033[0m'
cyan='\033[36m'
green='\033[32m'
red='\033[31m'
yellow='\033[33m'

backend_style_status="SKIPPED"
backend_tests_status="SKIPPED"
secret_scan_status="SKIPPED"
frontend_style_status="SKIPPED"
frontend_tests_status="SKIPPED"
frontend_e2e_status="SKIPPED"
frontend_build_status="SKIPPED"

backend_tests_summary=""
frontend_tests_summary=""
frontend_e2e_summary=""

if [ "$run_backend" -eq 1 ]; then
  print_box "Running backend style: formatting (Spotless), API checks (Checkstyle)..."
  if bash "$repo_root/scripts/quality/backend-style.sh" 2>&1 | tee "$backend_style_log"; then
    backend_style_status="PASS"
  else
    backend_style_status="FAIL"
  fi

  if [ "$backend_style_status" = "PASS" ]; then
    print_box "Running backend tests: unit tests (JUnit, Mockito), integration tests (Testcontainers, Docker)..."
    if bash "$repo_root/scripts/quality/backend-tests.sh" 2>&1 | tee "$backend_tests_log"; then
      backend_tests_status="PASS"
    else
      backend_tests_status="FAIL"
    fi
  fi
fi

if [ "$run_secret_scan" -eq 1 ] && [ "$backend_tests_status" = "PASS" ]; then
  print_box "Running secret scan: repository secrets scan (Gitleaks)..."
  if bash "$repo_root/scripts/git/scan-secrets.sh" 2>&1 | tee "$secret_scan_log"; then
    secret_scan_status="PASS"
  else
    secret_scan_status="FAIL"
  fi
fi

if [ "$run_frontend" -eq 1 ] && { [ "$run_secret_scan" -eq 0 ] || [ "$secret_scan_status" = "PASS" ]; }; then
  print_box "Running frontend style: linting (ESLint), formatting (Prettier)..."
  if bash "$repo_root/scripts/quality/frontend-style.sh" 2>&1 | tee "$frontend_style_log"; then
    frontend_style_status="PASS"
  else
    frontend_style_status="FAIL"
  fi
fi

if [ "$frontend_style_status" = "PASS" ]; then
  print_box "Running frontend tests: component tests (Vitest, Vue Test Utils, JSDOM)..."
  if bash "$repo_root/scripts/quality/frontend-tests.sh" 2>&1 | tee "$frontend_tests_log"; then
    frontend_tests_status="PASS"
  else
    frontend_tests_status="FAIL"
  fi
fi

if [ "$frontend_tests_status" = "PASS" ]; then
  print_box "Running frontend e2e tests: browser flows (Playwright, node:test)..."
  if bash "$repo_root/scripts/quality/frontend-e2e.sh" 2>&1 | tee "$frontend_e2e_log"; then
    frontend_e2e_status="PASS"
  else
    frontend_e2e_status="FAIL"
  fi
fi

if [ "$frontend_e2e_status" = "PASS" ]; then
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
  frontend_tests_summary="$(node -e 'const fs=require("fs"); const path=process.argv[1]; const report=process.argv[2]; if (fs.existsSync(report)) { try { const json=JSON.parse(fs.readFileSync(report,"utf8")); console.log(`${json.numPassedTestSuites} files, ${json.numPassedTests} tests`); process.exit(0); } catch {} } if (fs.existsSync(path)) { const t=fs.readFileSync(path, "utf8").replace(/\u001b\[[0-9;]*[A-Za-z]/g,"").replace(/\r/g,""); const m=t.match(/Test Files\s+(\d+)\s+passed(?:\s+\((\d+)\))?.*?Tests\s+(\d+)\s+passed(?:\s+\((\d+)\))?/s); if (m) { console.log(`${m[1]} files, ${m[3]} tests`); } }' "$frontend_tests_log" "$repo_root/frontend/.vite/vitest/results.json" 2>/dev/null || true)"
fi
if [ -s "$frontend_e2e_log" ]; then
  frontend_e2e_summary="$(node -e 'const fs=require("fs"); const path=process.argv[1]; if (fs.existsSync(path)) { const t=fs.readFileSync(path, "utf8").replace(/\u001b\[[0-9;]*[A-Za-z]/g,"").replace(/\r/g,""); const m=t.match(/# tests\s+(\d+)/); if (m) { console.log(`${m[1]} tests`); process.exit(0); } const m2=t.match(/1\.\.(\d+)/); if (m2) { console.log(`${m2[1]} tests`); } }' "$frontend_e2e_log" 2>/dev/null || true)"
fi

print_box "Summary"
summary_color="$green"
if [ "$run_backend" -eq 1 ] && { [ "$backend_style_status" = "FAIL" ] || [ "$backend_tests_status" = "FAIL" ]; }; then
  summary_color="$red"
elif [ "$run_secret_scan" -eq 1 ] && [ "$secret_scan_status" = "FAIL" ]; then
  summary_color="$red"
elif [ "$run_frontend" -eq 1 ] && { [ "$frontend_style_status" = "FAIL" ] || [ "$frontend_tests_status" = "FAIL" ] || [ "$frontend_e2e_status" = "FAIL" ] || [ "$frontend_build_status" = "FAIL" ]; }; then
  summary_color="$red"
elif [ "$run_backend" -eq 1 ] && { [ "$backend_style_status" = "SKIPPED" ] || [ "$backend_tests_status" = "SKIPPED" ]; }; then
  summary_color="$yellow"
elif [ "$run_secret_scan" -eq 1 ] && [ "$secret_scan_status" = "SKIPPED" ]; then
  summary_color="$yellow"
elif [ "$run_frontend" -eq 1 ] && { [ "$frontend_style_status" = "SKIPPED" ] || [ "$frontend_tests_status" = "SKIPPED" ] || [ "$frontend_e2e_status" = "SKIPPED" ] || [ "$frontend_build_status" = "SKIPPED" ]; }; then
  summary_color="$yellow"
fi

print_summary_border "$summary_color"
printf '%b|| %-24s | %-10s | %-57s ||%b\n' "$summary_color" "Step" "Status" "Details" "$reset"
print_summary_divider "$summary_color"

if [ "$run_backend" -eq 1 ]; then
  print_summary_row "backend style" "$backend_style_status" ""
  print_summary_row "backend tests" "$backend_tests_status" "$backend_tests_summary"
fi

if [ "$run_secret_scan" -eq 1 ]; then
  print_summary_row "secret scan" "$secret_scan_status" ""
fi

if [ "$run_frontend" -eq 1 ]; then
  print_summary_row "frontend style" "$frontend_style_status" ""
  print_summary_row "frontend tests" "$frontend_tests_status" "$frontend_tests_summary"
  print_summary_row "frontend e2e" "$frontend_e2e_status" "$frontend_e2e_summary"
  print_summary_row "frontend build" "$frontend_build_status" ""
fi

print_summary_border "$summary_color"

if [ "$run_backend" -eq 1 ] && { [ "$backend_style_status" != "PASS" ] || [ "$backend_tests_status" != "PASS" ]; }; then
  exit 1
fi
if [ "$run_secret_scan" -eq 1 ] && [ "$secret_scan_status" != "PASS" ]; then
  exit 1
fi
if [ "$run_frontend" -eq 1 ] && { [ "$frontend_style_status" != "PASS" ] || [ "$frontend_tests_status" != "PASS" ] || [ "$frontend_e2e_status" != "PASS" ] || [ "$frontend_build_status" != "PASS" ]; }; then
  exit 1
fi
