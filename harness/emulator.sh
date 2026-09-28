#!/usr/bin/env bash
# Headless emulator lifecycle for agent:device jobs on GitHub-hosted Ubuntu runners.
#
#   harness/emulator.sh setup      enable KVM, install the system image, create the AVD (idempotent)
#   harness/emulator.sh snapshot   cold boot once and save a quick-boot snapshot (run on cache miss)
#   harness/emulator.sh start      boot from the snapshot in the background and wait until ready
#   harness/emulator.sh stop
#
# Cache ~/.android/avd and $ANDROID_HOME/system-images between runs (see agent-builder.yml).
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
export ANDROID_HOME ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
LOG="${RUNNER_TEMP:-/tmp}/emulator.log"

emulator_args=(-avd "$AVD" -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect
  -camera-back none -camera-front none -memory 4096 -cores 3)

wait_for_boot() {
  local deadline=$((SECONDS + BOOT_TIMEOUT))
  adb wait-for-device
  until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
    if ((SECONDS > deadline)); then
      echo "Emulator did not boot within ${BOOT_TIMEOUT}s" >&2
      tail -n 100 "$LOG" >&2 || true
      exit 1
    fi
    sleep 2
  done
  echo "Emulator booted in ${SECONDS}s"
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
    yes | sdkmanager --licenses >/dev/null || true
    sdkmanager --install "platform-tools" "emulator" "$IMAGE" >/dev/null
    if ! avdmanager list avd -c | grep -qx "$AVD"; then
      echo no | avdmanager create avd --force -n "$AVD" -k "$IMAGE" -d "$DEVICE"
    fi
    ;;
  snapshot)
    emulator "${emulator_args[@]}" -no-snapshot-load >"$LOG" 2>&1 &
    wait_for_boot
    sleep 10
    adb emu kill # saves the quick-boot snapshot on exit
    wait || true
    echo "Saved quick-boot snapshot for $AVD"
    ;;
  start)
    nohup emulator "${emulator_args[@]}" -no-snapshot-save >"$LOG" 2>&1 &
    wait_for_boot
    for s in window_animation_scale transition_animation_scale animator_duration_scale; do
      adb shell settings put global "$s" 0
    done
    adb shell input keyevent 82 || true # dismiss the lock screen
    ;;
  stop)
    adb emu kill 2>/dev/null || true
    ;;
  *)
    echo "usage: $0 setup|snapshot|start|stop" >&2
    exit 2
    ;;
esac
