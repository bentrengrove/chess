#!/usr/bin/env bash
# Headless emulator lifecycle for agent:device jobs on GitHub-hosted Ubuntu runners.
#
#   harness/emulator.sh setup      enable KVM, install the system image, create the AVD (idempotent)
#   harness/emulator.sh snapshot   cold boot once and save a quick-boot snapshot (run on cache miss)
#   harness/emulator.sh start      boot from the snapshot in the background and wait until ready
#   harness/emulator.sh stop
#
# Cache ~/.android/avd and $ANDROID_HOME/system-images between runs (see agent-builder.yml).
# Every wait is bounded: if the emulator dies or never shows up, we fail fast and print its log.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cfg="$root/harness/harness.yml"
conf() { yq -r ".emulator.$1" "$cfg"; }

API="$(conf api_level)"
TARGET="$(conf target)"
ARCH="$(conf arch)"
DEVICE="$(conf device)"
AVD="$(conf avd_name)"
BOOT_TIMEOUT="$(conf boot_timeout_seconds)"
IMAGE="system-images;android-${API};${TARGET};${ARCH}"

: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
# Pin every location explicitly so avdmanager and the emulator agree on where the AVD lives
# (newer tools otherwise pick different defaults, and the emulator exits with "unknown AVD").
export ANDROID_HOME ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME="$HOME/.android"
export ANDROID_EMULATOR_HOME="$HOME/.android"
export ANDROID_AVD_HOME="$HOME/.android/avd"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
LOG="${RUNNER_TEMP:-/tmp}/emulator.log"
PID_FILE="${RUNNER_TEMP:-/tmp}/emulator.pid"

emulator_args=(-avd "$AVD" -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect
  -camera-back none -camera-front none -memory 4096 -cores 3 -no-metrics)

fail() {
  echo "::error::$1"
  echo "----- emulator.log (last 150 lines) -----"
  tail -n 150 "$LOG" 2>/dev/null || echo "(no log)"
  echo "----- adb devices -----"
  adb devices -l || true
  exit 1
}

launch() { # $@ = extra emulator flags
  mkdir -p "$ANDROID_AVD_HOME"
  echo "Launching: emulator ${emulator_args[*]} $*"
  nohup emulator "${emulator_args[@]}" "$@" >"$LOG" 2>&1 &
  echo $! >"$PID_FILE"
}

wait_for_boot() {
  local pid start=$SECONDS state=""
  pid="$(cat "$PID_FILE")"
  adb start-server >/dev/null 2>&1 || true
  while :; do
    kill -0 "$pid" 2>/dev/null || fail "Emulator process exited during boot."
    ((SECONDS - start > BOOT_TIMEOUT)) && fail "Emulator did not boot within ${BOOT_TIMEOUT}s (last state: ${state:-not visible to adb})."
    if adb devices | grep -q '^emulator-.*device$'; then
      state="$(timeout 5 adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
      [[ "$state" == "1" ]] && break
      state="booting (sys.boot_completed='${state}')"
    else
      state="$(adb devices | awk '/^emulator-/{print $2}')"
    fi
    sleep 3
  done
  echo "Emulator booted in $((SECONDS - start))s"
}

case "${1:-}" in
  setup)
    if [[ -e /dev/kvm ]]; then
      echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' |
        sudo tee /etc/udev/rules.d/99-kvm4all.rules >/dev/null
      sudo udevadm control --reload-rules
      sudo udevadm trigger --name-match=kvm
    else
      echo "::warning::/dev/kvm not present; the emulator will be very slow or fail."
    fi
    (yes || true) | sdkmanager --licenses >/dev/null 2>&1 || true
    sdkmanager --install "platform-tools" "emulator" "$IMAGE" >/dev/null
    if ! avdmanager list avd -c 2>/dev/null | grep -qx "$AVD"; then
      echo no | avdmanager create avd --force -n "$AVD" -k "$IMAGE" -d "$DEVICE"
    fi
    echo "AVDs: $(emulator -list-avds | tr '\n' ' ')"
    emulator -list-avds | grep -qx "$AVD" || fail "Emulator can't see AVD '$AVD' in $ANDROID_AVD_HOME."
    emulator -accel-check || echo "::warning::Hardware acceleration check failed."
    # Make adb/emulator available to later steps (and to Claude's Bash tool in the Builder).
    if [[ -n "${GITHUB_PATH:-}" ]]; then
      printf '%s\n' "$ANDROID_HOME/platform-tools" "$ANDROID_HOME/emulator" "$ANDROID_HOME/cmdline-tools/latest/bin" >>"$GITHUB_PATH"
      printf 'ANDROID_AVD_HOME=%s\nANDROID_USER_HOME=%s\nANDROID_EMULATOR_HOME=%s\n' \
        "$ANDROID_AVD_HOME" "$ANDROID_USER_HOME" "$ANDROID_EMULATOR_HOME" >>"$GITHUB_ENV"
    fi
    ;;
  snapshot)
    launch -no-snapshot-load
    wait_for_boot
    sleep 10
    adb emu kill >/dev/null 2>&1 || true # saves the quick-boot snapshot on exit
    for _ in $(seq 1 60); do kill -0 "$(cat "$PID_FILE")" 2>/dev/null || break; sleep 2; done
    echo "Saved quick-boot snapshot for $AVD"
    ;;
  start)
    launch -no-snapshot-save
    wait_for_boot
    for s in window_animation_scale transition_animation_scale animator_duration_scale; do
      timeout 10 adb shell settings put global "$s" 0 || true
    done
    timeout 10 adb shell input keyevent 82 || true # dismiss the lock screen
    ;;
  log)
    tail -n "${2:-200}" "$LOG" 2>/dev/null || echo "(no log)"
    ;;
  stop)
    timeout 20 adb emu kill >/dev/null 2>&1 || true
    ;;
  *)
    echo "usage: $0 setup|snapshot|start|log|stop" >&2
    exit 2
    ;;
esac
