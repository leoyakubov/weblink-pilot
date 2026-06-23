#!/usr/bin/env bash

terminal_width() {
  local fallback="${1:-112}"
  local cols
  if cols="$(tput cols 2>/dev/null)"; then
    if [[ "$cols" =~ ^[0-9]+$ ]] && [ "$cols" -ge 60 ]; then
      printf '%s' "$cols"
      return
    fi
  fi
  printf '%s' "$fallback"
}

init_terminal_colors() {
  if [ -n "${NO_COLOR:-}" ] || [[ ! -t 1 ]]; then
    reset=''
    cyan=''
    green=''
    red=''
    yellow=''
    return
  fi

  reset=$'\033[0m'
  cyan=$'\033[36m'
  green=$'\033[32m'
  red=$'\033[31m'
  yellow=$'\033[33m'
}

print_box() {
  local title="$1"
  local width="${2:-$(terminal_width)}"
  if [ "$width" -gt 120 ]; then
    width=120
  fi
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
  border="||$(printf '%*s' $((width - 4)) '' | tr ' ' '=')||"
  printf '\n%b%s%b\n' "$cyan" "$border" "$reset"
  for line in "${lines[@]}"; do
    printf '|| %-*s ||\n' "$inner_width" "$line"
  done
  printf '%b%s%b\n\n' "$cyan" "$border" "$reset"
}

print_summary_border() {
  local color="${1:-$cyan}"
  local width="${2:-$(terminal_width)}"
  if [ "$width" -gt 120 ]; then
    width=120
  fi
  printf '%b||%s||%b\n' "$color" "$(printf '%*s' $((width - 4)) '' | tr ' ' '=')" "$reset"
}

print_summary_divider() {
  local color="${1:-$cyan}"
  local width="${2:-$(terminal_width)}"
  if [ "$width" -gt 120 ]; then
    width=120
  fi
  printf '%b||%s||%b\n' "$color" "$(printf '%*s' $((width - 4)) '' | tr ' ' '-')" "$reset"
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
  local width="${4:-$(terminal_width)}"
  local inner_width=$((width - 4))
  local label_width=24
  local badge_width=10
  local details_width=$((inner_width - label_width - badge_width - 8))
  if [ "$width" -gt 120 ]; then
    width=120
    inner_width=$((width - 4))
    details_width=$((inner_width - label_width - badge_width - 8))
  fi
  color="$(status_color "$status")"
  badge="$(status_badge "$status")"
  if [ "$details_width" -lt 20 ]; then
    details_width=20
  fi
  label="$(truncate_text "$label" 28)"
  details="$(truncate_text "$details" "$details_width")"
  text="$(printf '|| %-24s | %-10s | %-*s ||' "$label" "$badge" "$details_width" "$details")"
  printf '%b%s%b\n' "$color" "$text" "$reset"
}

remove_stale_docker_containers() {
  local container_name
  for container_name in "$@"; do
    if docker ps -a --filter "name=^/${container_name}$" --format '{{.Names}}' | grep -qx "$container_name"; then
      printf '%bRemoving stale Docker container %s%b\n' "$yellow" "$container_name" "$reset"
      docker rm -f "$container_name" >/dev/null
    fi
  done
}

ensure_frontend_dependencies() {
  local frontend_dir="$1"
  local vite_probe_status=0
  local remove_node_modules_status=0

  if [ ! -d "$frontend_dir/node_modules" ]; then
    printf 'Frontend dependencies are missing. Installing them for the current platform...\n'
    (
      cd "$frontend_dir"
      npm install --package-lock=false
    )
    return
  fi

  (
    cd "$frontend_dir"
    node --input-type=module -e "import('vite').then(() => process.exit(0)).catch(() => process.exit(1))"
  ) || vite_probe_status=$?

  if [ "$vite_probe_status" -eq 0 ]; then
    return
  fi

  printf 'Frontend dependencies do not match the current platform. Refreshing them with npm install...\n'
  (
    cd "$frontend_dir"
    node --input-type=module -e "import fs from 'node:fs'; import path from 'node:path'; const dir = path.resolve('node_modules'); try { fs.rmSync(dir, { recursive: true, force: true, maxRetries: 5, retryDelay: 100 }); process.exit(0); } catch (error) { console.error(error); process.exit(1); }"
  ) || remove_node_modules_status=$?

  if [ "$remove_node_modules_status" -ne 0 ]; then
    printf 'Could not remove frontend/node_modules cleanly. Close any process using it, delete the folder manually, and rerun the script.\n' >&2
    return "$remove_node_modules_status"
  fi

  (
    cd "$frontend_dir"
    npm install --package-lock=false
  )

  (
    cd "$frontend_dir"
    node --input-type=module -e "import('vite').then(() => process.exit(0)).catch(() => process.exit(1))"
  ) || vite_probe_status=$?

  if [ "$vite_probe_status" -ne 0 ]; then
    printf 'Frontend dependencies still cannot load Vite. Remove frontend/node_modules and rerun the script from WSL.\n' >&2
    return "$vite_probe_status"
  fi
}

init_terminal_colors
