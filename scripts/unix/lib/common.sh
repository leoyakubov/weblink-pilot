#!/usr/bin/env bash

print_box() {
  local title="$1"
  local width="${2:-112}"
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
  local width="${2:-112}"
  printf '%b||%s||%b\n' "$color" "$(printf '%0.s=' $(seq 1 $((width - 4))))" "$reset"
}

print_summary_divider() {
  local color="${1:-$cyan}"
  local width="${2:-112}"
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
  label="$(truncate_text "$label" 28)"
  details="$(truncate_text "$details" 61)"
  text="$(printf '|| %-24s | %-10s | %-57s ||' "$label" "$badge" "$details")"
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
