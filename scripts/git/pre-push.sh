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
  print_box "Running backend style: formatting (Spotless), API checks (Checkstyle)"
  if bash "$repo_root/scripts/quality/backend-style.sh" 2>&1 | tee "$backend_style_log"; then
    backend_style_status="PASS"
  else
    backend_style_status="FAIL"
  fi

  if [ "$backend_style_status" = "PASS" ]; then
    print_box "Running backend coverage: tests, JaCoCo gates, aggregate report, SpotBugs"
    if bash "$repo_root/scripts/quality/backend-coverage.sh" 2>&1 | tee "$backend_tests_log"; then
      backend_tests_status="PASS"
    else
      backend_tests_status="FAIL"
    fi
  fi
fi

if [ "$run_secret_scan" -eq 1 ] && [ "$backend_tests_status" = "PASS" ]; then
  print_box "Running secret scan: repository secrets scan (Gitleaks)"
  if bash "$repo_root/scripts/git/scan-secrets.sh" 2>&1 | tee "$secret_scan_log"; then
    secret_scan_status="PASS"
  else
    secret_scan_status="FAIL"
  fi
fi

if [ "$run_frontend" -eq 1 ] && { [ "$run_secret_scan" -eq 0 ] || [ "$secret_scan_status" = "PASS" ]; }; then
  print_box "Running frontend style: linting (ESLint), formatting (Prettier)"
  if bash "$repo_root/scripts/quality/frontend-style.sh" 2>&1 | tee "$frontend_style_log"; then
    frontend_style_status="PASS"
  else
    frontend_style_status="FAIL"
  fi
fi

if [ "$frontend_style_status" = "PASS" ]; then
  print_box "Running frontend coverage: component tests with Vitest coverage thresholds"
  if bash "$repo_root/scripts/quality/frontend-coverage.sh" 2>&1 | tee "$frontend_tests_log"; then
    frontend_tests_status="PASS"
  else
    frontend_tests_status="FAIL"
  fi
fi

if [ "$frontend_tests_status" = "PASS" ]; then
  print_box "Running frontend e2e tests: browser flows (Playwright, node:test)"
  if bash "$repo_root/scripts/quality/frontend-e2e.sh" 2>&1 | tee "$frontend_e2e_log"; then
    frontend_e2e_status="PASS"
  else
    frontend_e2e_status="FAIL"
  fi
fi

if [ "$frontend_e2e_status" = "PASS" ]; then
  pushd "$repo_root/frontend" >/dev/null
  print_box "Building frontend: typecheck and production bundle (Vue TSC, Vite)"
  if npm run build 2>&1 | tee "$frontend_build_log"; then
    frontend_build_status="PASS"
  else
    frontend_build_status="FAIL"
  fi
  popd >/dev/null
fi

if [ -s "$backend_tests_log" ]; then
  backend_tests_summary="$(grep -Eo 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+' "$backend_tests_log" | tail -n 1 || true)"
  if [ "$backend_tests_status" = "PASS" ]; then
    backend_tests_summary="JaCoCo gates, aggregate report, SpotBugs"
    backend_coverage_report="$repo_root/backend/build-support/target/site/jacoco-aggregate/jacoco.csv"
    if [ -f "$backend_coverage_report" ]; then
      backend_coverage_summary="$(node -e 'const fs=require("fs"); const csv=fs.readFileSync(process.argv[1],"utf8").trim().split(/\r?\n/).slice(1); let lineMissed=0,lineCovered=0,branchMissed=0,branchCovered=0; for (const row of csv) { const cols=row.split(","); lineMissed+=Number(cols[7]||0); lineCovered+=Number(cols[8]||0); branchMissed+=Number(cols[5]||0); branchCovered+=Number(cols[6]||0); } const pct=(covered, missed)=> covered + missed === 0 ? "100.0" : ((covered/(covered+missed))*100).toFixed(1); console.log(`lines ${pct(lineCovered,lineMissed)}%, branches ${pct(branchCovered,branchMissed)}%`);' "$backend_coverage_report" 2>/dev/null || true)"
      if [ -n "$backend_coverage_summary" ]; then
        backend_tests_summary="$backend_coverage_summary"
      fi
    fi
  fi
fi
if [ -s "$frontend_tests_log" ]; then
  frontend_tests_summary="$(node -e 'const fs=require("fs"); const path=process.argv[1]; const report=process.argv[2]; if (fs.existsSync(report)) { try { const json=JSON.parse(fs.readFileSync(report,"utf8")); console.log(`${json.numPassedTestSuites} files, ${json.numPassedTests} tests`); process.exit(0); } catch {} } if (fs.existsSync(path)) { const t=fs.readFileSync(path, "utf8").replace(/\u001b\[[0-9;]*[A-Za-z]/g,"").replace(/\r/g,""); const m=t.match(/Test Files\s+(\d+)\s+passed(?:\s+\((\d+)\))?.*?Tests\s+(\d+)\s+passed(?:\s+\((\d+)\))?/s); if (m) { console.log(`${m[1]} files, ${m[3]} tests`); } }' "$frontend_tests_log" "$repo_root/frontend/.vite/vitest/results.json" 2>/dev/null || true)"
  frontend_coverage_report="$repo_root/frontend/coverage/coverage-summary.json"
  if [ -f "$frontend_coverage_report" ]; then
    frontend_coverage_summary="$(node -e 'const fs=require("fs"); const total=JSON.parse(fs.readFileSync(process.argv[1],"utf8")).total; console.log(`lines ${total.lines.pct.toFixed(1)}%, branches ${total.branches.pct.toFixed(1)}%`);' "$frontend_coverage_report" 2>/dev/null || true)"
    if [ -n "$frontend_coverage_summary" ]; then
      if [ -n "$frontend_tests_summary" ]; then
        frontend_tests_summary="$frontend_coverage_summary; $frontend_tests_summary"
      else
        frontend_tests_summary="$frontend_coverage_summary"
      fi
    fi
  fi
fi
if [ -s "$frontend_e2e_log" ]; then
  frontend_e2e_summary="$(node -e 'const fs=require("fs"); const path=process.argv[1]; if (fs.existsSync(path)) { const t=fs.readFileSync(path, "utf8").replace(/\u001b\[[0-9;]*[A-Za-z]/g,"").replace(/\r/g,""); const m=t.match(/# tests\s+(\d+)/) || t.match(/[^\S\r\n]tests\s+(\d+)/); if (m) { console.log(`${m[1]} tests`); process.exit(0); } const m2=t.match(/1\.\.(\d+)/); if (m2) { console.log(`${m2[1]} tests`); } }' "$frontend_e2e_log" 2>/dev/null || true)"
fi

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

summary_width="$(terminal_width)"
if [ "$summary_width" -lt 160 ]; then
  summary_width=160
fi
if [ "$summary_width" -gt 160 ]; then
  summary_width=160
fi
print_box "Summary" "$summary_width" "$summary_color"
print_summary_border "$summary_color" "$summary_width"
summary_label_width=20
summary_status_width=8
summary_details_width=$((summary_width - 4 - summary_label_width - summary_status_width - 8))
if [ "$summary_details_width" -lt 20 ]; then
  summary_details_width=20
fi
printf '%b|| %-*s | %-*s | %-*s ||%b\n' "$summary_color" "$summary_label_width" "Step" "$summary_status_width" "Status" "$summary_details_width" "Details" "$reset"
print_summary_divider "$summary_color" "$summary_width"

if [ "$run_backend" -eq 1 ]; then
  print_summary_row "backend style" "$backend_style_status" ""
  print_summary_row "backend coverage" "$backend_tests_status" "$backend_tests_summary"
fi

if [ "$run_secret_scan" -eq 1 ]; then
  print_summary_row "secret scan" "$secret_scan_status" ""
fi

if [ "$run_frontend" -eq 1 ]; then
  print_summary_row "frontend style" "$frontend_style_status" ""
  print_summary_row "frontend coverage" "$frontend_tests_status" "$frontend_tests_summary"
  print_summary_row "frontend e2e" "$frontend_e2e_status" "$frontend_e2e_summary"
  print_summary_row "frontend build" "$frontend_build_status" ""
fi

print_summary_border "$summary_color" "$summary_width"

if [ "$run_backend" -eq 1 ] && { [ "$backend_style_status" != "PASS" ] || [ "$backend_tests_status" != "PASS" ]; }; then
  exit 1
fi
if [ "$run_secret_scan" -eq 1 ] && [ "$secret_scan_status" != "PASS" ]; then
  exit 1
fi
if [ "$run_frontend" -eq 1 ] && { [ "$frontend_style_status" != "PASS" ] || [ "$frontend_tests_status" != "PASS" ] || [ "$frontend_e2e_status" != "PASS" ] || [ "$frontend_build_status" != "PASS" ]; }; then
  exit 1
fi
