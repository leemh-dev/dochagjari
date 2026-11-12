#!/bin/bash
set -euo pipefail

fail() {
  printf '%s\n' "$*" >&2
  exit 1
}

(( $# > 0 )) || fail "Usage: $0 command [args...]"

script_dir="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
repo_dir="$(cd -P -- "$script_dir/../.." && pwd -P)"
dev_root="${DOCHAGJARI_DEV_ROOT:-$(dirname -- "$repo_dir")}"

[[ "$(uname -s)" == Darwin && "$(uname -m)" == arm64 ]] ||
  fail "This launcher requires an Apple Silicon Mac."

volume="$(dirname -- "$dev_root")"
[[ "$(dirname -- "$volume")" == /Volumes && -d "$dev_root" ]] ||
  fail "Connect the external SSD and set DOCHAGJARI_DEV_ROOT to its development directory."
[[ "$(cd -P -- "$dev_root" && pwd -P)" == "$dev_root" ]] ||
  fail "Use the physical SSD directory without a symlink."

volume_device="$(stat -f %d "$volume")"
[[ "$volume_device" != "$(stat -f %d /Volumes)" ]] ||
  fail "The external volume is not mounted."
[[ "$(stat -f %d "$dev_root")" == "$volume_device" && -w "$dev_root" ]] ||
  fail "The development directory must be writable on the mounted SSD."
[[ "$repo_dir" == "$dev_root/"* && -f "$repo_dir/gradlew" &&
    "$(stat -f %d "$repo_dir")" == "$volume_device" ]] ||
  fail "The project checkout must be inside the same SSD development directory."

export ANDROID_HOME="$dev_root/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_AVD_HOME="$dev_root/avd"
export GRADLE_USER_HOME="$dev_root/gradle"
export TMPDIR="$dev_root/tmp"

for path in "$ANDROID_HOME" "$ANDROID_AVD_HOME" "$GRADLE_USER_HOME" "$TMPDIR"; do
  [[ -d "$path" && ! -L "$path" && -w "$path" ]] ||
    fail "Missing or unwritable SSD directory: $path"
  [[ "$(stat -f %d "$path")" == "$volume_device" ]] ||
    fail "Development data directory is outside the SSD volume: $path"
done

export JAVA_HOME
JAVA_HOME="$(/usr/libexec/java_home -F -v 17 -a arm64)"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$JAVA_HOME/bin:$PATH"

cd -- "$repo_dir"
exec "$@"
