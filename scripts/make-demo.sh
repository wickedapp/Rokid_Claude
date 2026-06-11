#!/usr/bin/env bash
#
# make-demo.sh — pull a HUD screen recording off the glasses and turn it into
# a README-ready GIF.
#
# Record on the device first, e.g.:
#   adb shell screenrecord --time-limit 20 --size 480x640 /sdcard/demo.mp4
# Then run this script. It pulls the mp4, trims/crops it, and writes a GIF to
# docs/media/demo.gif by default.
#
# Usage:
#   scripts/make-demo.sh [-s START] [-t DURATION] [-f FPS] [-w WIDTH]
#                        [--no-crop] [-i INPUT_MP4] [-o OUTPUT_GIF]
#
# Options:
#   -s START      trim start, seconds (default: 0)
#   -t DURATION   trim length, seconds (default: whole clip)
#   -f FPS        GIF frame rate (default: 12)
#   -w WIDTH      GIF width in px, height auto (default: 360)
#   --no-crop     keep the full 480x640 frame (default crops to the 480x480
#                 content area, dropping the black 80px safe zones)
#   -i INPUT_MP4  use a local mp4 instead of pulling from the device
#   -o OUTPUT_GIF output path (default: docs/media/demo.gif)
#
# Examples:
#   scripts/make-demo.sh                 # pull, full clip -> docs/media/demo.gif
#   scripts/make-demo.sh -s 1 -t 11      # use the 1s..12s window
#   scripts/make-demo.sh -i /tmp/x.mp4 -w 320 -f 10
set -euo pipefail

START=0
DURATION=""
FPS=12
WIDTH=360
CROP="crop=480:480:0:80"
INPUT=""
DEVICE_PATH="/sdcard/demo.mp4"

# Resolve repo root and default output relative to it.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT="$ROOT/docs/media/demo.gif"

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
command -v "$ADB" >/dev/null 2>&1 || ADB="adb"

while [ $# -gt 0 ]; do
  case "$1" in
    -s) START="$2"; shift 2 ;;
    -t) DURATION="$2"; shift 2 ;;
    -f) FPS="$2"; shift 2 ;;
    -w) WIDTH="$2"; shift 2 ;;
    --no-crop) CROP=""; shift ;;
    -i) INPUT="$2"; shift 2 ;;
    -o) OUTPUT="$2"; shift 2 ;;
    -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

command -v ffmpeg >/dev/null 2>&1 || { echo "ffmpeg not found — brew install ffmpeg" >&2; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# 1) Get the source mp4 (pull from device unless a local file was given).
SRC="$TMP/src.mp4"
if [ -n "$INPUT" ]; then
  cp "$INPUT" "$SRC"
else
  echo "Pulling $DEVICE_PATH from device…"
  "$ADB" pull "$DEVICE_PATH" "$SRC" >/dev/null
fi

# 2) Build the filter chain: trim window + optional crop.
TRIM=(-ss "$START")
[ -n "$DURATION" ] && TRIM+=(-t "$DURATION")

VF="fps=$FPS"
[ -n "$CROP" ] && VF="$CROP,$VF"
VF="$VF,scale=$WIDTH:-1:flags=lanczos"

# 3) Two-pass palette for clean colors, then emit the GIF.
echo "Encoding GIF (fps=$FPS, width=$WIDTH${CROP:+, cropped})…"
ffmpeg -y "${TRIM[@]}" -i "$SRC" -vf "$VF,palettegen=stats_mode=diff" "$TMP/pal.png" -loglevel error
mkdir -p "$(dirname "$OUTPUT")"
ffmpeg -y "${TRIM[@]}" -i "$SRC" -i "$TMP/pal.png" \
  -lavfi "$VF [x]; [x][1:v] paletteuse=dither=bayer:bayer_scale=3" \
  "$OUTPUT" -loglevel error

SIZE="$(du -h "$OUTPUT" | cut -f1)"
echo "Wrote $OUTPUT ($SIZE)."
[ "${SIZE%M}" != "$SIZE" ] && awk "BEGIN{exit !(${SIZE%M}+0 > 4)}" 2>/dev/null \
  && echo "Tip: >4MB — rerun with a smaller -w (e.g. 320) or -f 10 to shrink."
echo "Done. Reference it in README.md as: ![demo](docs/media/demo.gif)"
