#!/usr/bin/env bash
# Headless emulator lifecycle for agent:device jobs, driven by the android CLI.
#
#   harness/emulator.sh setup      KVM + android CLI + create the AVD for the configured profile
#   harness/emulator.sh snapshot   cold boot once, then stop (saves the quick-boot snapshot)
#   harness/emulator.sh start      boot headless (from the snapshot) and wait until ready
#   harness/emulator.sh stop
#   harness/emulator.sh log [N]    diagnostics: devices + last N logcat lines
#
# The android CLI picks the system image for the profile and blocks until the device is ready.
# We only add KVM access, a hard timeout, PATH exports for later steps, and diagnostics.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cfg="$root/harness/harness.yml"
PROFILE="$(yq -r .emulator.profile "$cfg")"
BOOT_TIMEOUT="$(yq -r .emulator.boot_timeout_seconds "$cfg")"

: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
export ANDROID_HOME ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$HOME/.local/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
AVD_FILE="${RUNNER_TEMP:-/tmp}/emulator.avd"

# Always point the CLI at the runner's SDK so AVDs, images and caches land in known places.
android() { command android --sdk="$ANDROID_HOME" "$@"; }

fail() {
  echo "::error::$1"
  echo "----- android emulator list -----"
  android emulator list || true
  echo "----- adb devices -----"
  adb devices -l 2>/dev/null || true
  exit 1
}

avd() { cat "$AVD_FILE" 2>/dev/null || android emulator list | head -n1; }

boot() { # $@ = extra flags for `android emulator start`
  local name start=$SECONDS
  name="$(avd)"
  [[ -n "$name" ]] || fail "No AVD found; run setup first."
  echo "Starting $name ($*)"
  timeout "$BOOT_TIMEOUT" android -v emulator start --headless "$@" "$name" ||
    fail "android emulator start failed or took longer than ${BOOT_TIMEOUT}s."
  adb devices | grep -q '^emulator-.*device$' || fail "Emulator started but adb can't see it."
  echo "Emulator ready in $((SECONDS - start))s"
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
    if ! type -P android >/dev/null; then
      curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh | bash
    fi
    android --version
    # Make android/adb available to later steps, including Claude's Bash tool in the Builder.
    if [[ -n "${GITHUB_PATH:-}" ]]; then
      printf '%s\n' "$HOME/.local/bin" "$ANDROID_HOME/platform-tools" "$ANDROID_HOME/emulator" >>"$GITHUB_PATH"
    fi
    if [[ -z "$(android emulator list)" ]]; then
      timeout 900 android -v emulator create "$PROFILE" || fail "android emulator create $PROFILE failed."
    fi
    android emulator list | head -n1 >"$AVD_FILE"
    echo "AVD: $(cat "$AVD_FILE") (profile $PROFILE)"
    ;;
  snapshot)
    boot --cold
    sleep 10
    android emulator stop "$(avd)" || true # a clean stop saves the quick-boot snapshot
    for _ in $(seq 1 30); do adb devices | grep -q '^emulator-' || break; sleep 2; done
    echo "Saved quick-boot snapshot for $(avd)"
    ;;
  start)
    boot
    for s in window_animation_scale transition_animation_scale animator_duration_scale; do
      timeout 10 adb shell settings put global "$s" 0 || true
    done
    timeout 10 adb shell input keyevent 82 || true # dismiss the lock screen
    ;;
  stop)
    timeout 30 android emulator stop "$(avd)" 2>/dev/null || timeout 20 adb emu kill 2>/dev/null || true
    ;;
  log)
    android emulator list || true
    adb devices -l || true
    timeout 20 adb logcat -d -t "${2:-200}" 2>/dev/null || true
    ;;
  *)
    echo "usage: $0 setup|snapshot|start|stop|log" >&2
    exit 2
    ;;
esac
