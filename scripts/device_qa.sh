#!/usr/bin/env bash
# Splashy Glide - automatic device QA (debug build).
#
#   scripts/device_qa.sh                 # default sample: one level from every world + tile intros
#   LEVELS="11 15 27" scripts/device_qa.sh
#
# What it does:
#   1. runs the core tests (all 5366 levels verified) and builds + installs the debug APK
#   2. backs up the tester's progress (it is restored at the end, even on failure)
#   3. for each level: opens it (--ei debugLevel N), autoplays the stored solution (--ez autoplay true),
#      waits for the "MazeQA WIN" log, and screenshots the start and the win panel
#   4. checks logcat for crashes / GL errors and collects the fps lines
#   5. writes docs/qa/<timestamp>/report.md
# Requirements: a device in `adb devices`, Android Studio's JBR for Gradle.
set -euo pipefail
cd "$(dirname "$0")/.."

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PKG=com.ngstudio.maze
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
LEVELS="${LEVELS:-1 11 21 41 71 111 251 760 1001 1251 1501 1751 2001 2251 3500 4999}"
TIMEOUT="${TIMEOUT:-90}"
OUT="docs/qa/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

"$ADB" get-state >/dev/null || { echo "No device connected"; exit 1; }

echo "== core tests + debug build"
./gradlew -q :core:test :app:assembleDebug
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null

echo "== backing up progress"
"$ADB" shell am force-stop "$PKG"
"$ADB" exec-out run-as "$PKG" tar cf - shared_prefs files > "$OUT/progress_backup.tar" 2>/dev/null || true
restore() {
  echo "== restoring progress"
  "$ADB" shell am force-stop "$PKG" || true
  if [ -s "$OUT/progress_backup.tar" ]; then
    cat "$OUT/progress_backup.tar" | "$ADB" shell run-as "$PKG" tar xf - || true
  fi
  "$ADB" shell am start -S -n "$PKG/.MainActivity" >/dev/null 2>&1 || true
}
trap restore EXIT

{
  echo "# Device QA $(date '+%Y-%m-%d %H:%M')"
  echo
  echo "Device: $("$ADB" shell getprop ro.product.model | tr -d '\r')  ·  Android $("$ADB" shell getprop ro.build.version.release | tr -d '\r')"
  echo
  echo "| Level | Result | Moves / Par | Stars | Start | Win |"
  echo "|---|---|---|---|---|---|"
} > "$OUT/report.md"

fails=0
for n in $LEVELS; do
  "$ADB" logcat -c
  "$ADB" shell am start -S -n "$PKG/.MainActivity" --ei debugLevel "$n" --ez autoplay true >/dev/null 2>&1
  sleep 2.5
  "$ADB" exec-out screencap -p > "$OUT/L${n}_start.png"
  result="TIMEOUT"; line=""
  for ((t = 0; t < TIMEOUT; t++)); do
    line=$("$ADB" logcat -d -s MazeQA:I | grep "WIN level=$n " | tail -1 || true)
    if [ -n "$line" ]; then result="WIN"; break; fi
    if "$ADB" logcat -d | grep -q "FATAL EXCEPTION"; then result="CRASH"; break; fi
    sleep 1
  done
  sleep 1.6
  "$ADB" exec-out screencap -p > "$OUT/L${n}_win.png"
  moves=$(echo "$line" | sed -n 's/.*moves=\([0-9]*\).*/\1/p'); par=$(echo "$line" | sed -n 's/.*par=\([0-9]*\).*/\1/p')
  stars=$(echo "$line" | sed -n 's/.*stars=\([0-9]*\).*/\1/p')
  echo "| $n | $result | ${moves:-?} / ${par:-?} | ${stars:-?} | ![](L${n}_start.png) | ![](L${n}_win.png) |" >> "$OUT/report.md"
  "$ADB" logcat -d | grep -E "MazeGL" >> "$OUT/gl.log" || true
  if [ "$result" != "WIN" ]; then fails=$((fails + 1)); "$ADB" logcat -d > "$OUT/L${n}_logcat.txt"; fi
  echo "  level $n: $result ${moves:+($moves moves, par $par, $stars stars)}"
done

{
  echo
  echo "## Frame rate"
  echo '```'
  grep -o "fps=[0-9.]*" "$OUT/gl.log" | sort | uniq -c | sort -rn | head -8 || echo "no fps lines"
  echo '```'
  if grep -qiE "compile failed|link failed|GL error" "$OUT/gl.log"; then echo; echo "**GL errors found - see gl.log**"; fails=$((fails + 1)); fi
  echo
  echo "Failures: $fails"
} >> "$OUT/report.md"

echo "== report: $OUT/report.md  (failures: $fails)"
[ "$fails" -eq 0 ]
