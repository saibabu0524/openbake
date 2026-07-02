#!/usr/bin/env zsh
# ─────────────────────────────────────────────────────────────────────────────
# run_all.sh — Start the unified server (API + embedded web app), MySQL, ngrok,
# and Android for OpenBake.
#
# The old backend/ (FastAPI) + web/ (separately-served Next.js) split has been
# retired in favor of server/ — a single Spring Boot jar that serves both the
# REST API and the built web app from the same origin. See server/README or
# the migration notes for background.
#
# Usage:
#   ./run_all.sh              # Full stack (MySQL + server + ngrok; Android info printed)
#   ./run_all.sh --android    # Also build & install Android APK on emulator/device
#   ./run_all.sh --help
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Colours ───────────────────────────────────────────────────────────────────
RED="\033[0;31m"; GREEN="\033[0;32m"; YELLOW="\033[1;33m"
CYAN="\033[0;36m"; BOLD="\033[1m"; RESET="\033[0m"

log()  { echo "${BOLD}[$(date +%H:%M:%S)]${RESET} $*"; }
ok()   { echo "${GREEN}[✔]${RESET} $*"; }
warn() { echo "${YELLOW}[!]${RESET} $*"; }
err()  { echo "${RED}[✘]${RESET} $*" >&2; }
sep()  { echo "${CYAN}────────────────────────────────────────────────────${RESET}"; }

# ── Paths ─────────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/server"
ANDROID_DIR="$SCRIPT_DIR/android"
ANDROID_GRADLE="$ANDROID_DIR/app/build.gradle.kts"

SERVER_PORT=8080
MYSQL_HOST_PORT=3307
MYSQL_CONTAINER=openbake-mysql-dev
NGROK_API="http://localhost:4040/api/tunnels"

# Saved PIDs for cleanup
declare -a PIDS=()

# ── Help ──────────────────────────────────────────────────────────────────────
if [[ "${1:-}" == "--help" ]]; then
  echo ""
  echo "${BOLD}Usage:${RESET}"
  echo "  ./run_all.sh              # MySQL + server (API + web) + ngrok"
  echo "  ./run_all.sh --android    # + build & install Android APK"
  echo "  ./run_all.sh --help"
  echo ""
  exit 0
fi

BUILD_ANDROID=false
[[ "${1:-}" == "--android" ]] && BUILD_ANDROID=true

# ── Cleanup on exit ───────────────────────────────────────────────────────────
cleanup() {
  echo ""
  warn "Shutting down…"
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
  pkill -f "ngrok http $SERVER_PORT" 2>/dev/null || true
  ok "Done. (MySQL container '$MYSQL_CONTAINER' left running — stop it with: docker stop $MYSQL_CONTAINER)"
}
trap cleanup EXIT INT TERM

# ── Prerequisite checks ───────────────────────────────────────────────────────
sep
log "Checking prerequisites…"

check_cmd() {
  if ! command -v "$1" &>/dev/null; then
    err "$1 not found. Install it and re-run."
    exit 1
  fi
  ok "$1 found"
}

check_cmd java
check_cmd mvn
check_cmd python3

if ! command -v docker &>/dev/null; then
  warn "docker not found — you'll need a MySQL instance running yourself."
  warn "Set DATABASE_URL/DATABASE_USERNAME/DATABASE_PASSWORD env vars before re-running, or install Docker."
  DOCKER_AVAILABLE=false
else
  ok "docker found"
  DOCKER_AVAILABLE=true
fi

# ngrok is optional — warn only
if ! command -v ngrok &>/dev/null; then
  warn "ngrok not found — Android emulator URL (10.0.2.2) will be used instead."
  NGROK_AVAILABLE=false
else
  ok "ngrok found"
  NGROK_AVAILABLE=true
fi

# ── 1. MySQL ──────────────────────────────────────────────────────────────────
if $DOCKER_AVAILABLE; then
  sep
  log "Starting MySQL (container: $MYSQL_CONTAINER, port $MYSQL_HOST_PORT)…"

  if docker ps -a --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
    docker start "$MYSQL_CONTAINER" >/dev/null
    ok "Reused existing container."
  else
    docker run -d --name "$MYSQL_CONTAINER" \
      -e MYSQL_ROOT_PASSWORD=root \
      -e MYSQL_DATABASE=openbake \
      -p "${MYSQL_HOST_PORT}:3306" \
      -v openbake-mysql-dev-data:/var/lib/mysql \
      mysql:8.0 --default-authentication-plugin=mysql_native_password >/dev/null
    ok "Created new container."
  fi

  log "Waiting for MySQL to become ready…"
  MYSQL_READY=false
  for i in {1..30}; do
    if docker exec "$MYSQL_CONTAINER" mysqladmin ping -h 127.0.0.1 -uroot -proot --silent >/dev/null 2>&1; then
      MYSQL_READY=true
      break
    fi
    sleep 2
  done
  $MYSQL_READY && ok "MySQL is up." || { err "MySQL did not become ready in time."; exit 1; }

  export DATABASE_URL="${DATABASE_URL:-jdbc:mysql://localhost:${MYSQL_HOST_PORT}/openbake?useSSL=false&allowPublicKeyRetrieval=true}"
  export DATABASE_USERNAME="${DATABASE_USERNAME:-root}"
  export DATABASE_PASSWORD="${DATABASE_PASSWORD:-root}"
fi

# ── 2. Server (API + embedded web app) ─────────────────────────────────────────
sep
log "Starting the Spring Boot server on port $SERVER_PORT (this also builds web/ — first run takes longer)…"

(
  cd "$SERVER_DIR"
  SERVER_PORT="$SERVER_PORT" mvn -q spring-boot:run \
    >> /tmp/openbake_server.log 2>&1
) &
PIDS+=($!)

log "Waiting for the server to become ready…"
READY=false
for i in {1..90}; do
  if curl -sf "http://localhost:$SERVER_PORT/health" > /dev/null 2>&1; then
    READY=true
    break
  fi
  sleep 2
done

if $READY; then
  ok "Server is up: http://localhost:$SERVER_PORT"
else
  err "Server did not start in time. Check /tmp/openbake_server.log"
  exit 1
fi

# ── 3. ngrok ──────────────────────────────────────────────────────────────────
NGROK_URL=""

if $NGROK_AVAILABLE; then
  sep
  log "Starting ngrok tunnel…"

  ngrok http "$SERVER_PORT" --log=stdout > /tmp/openbake_ngrok.log 2>&1 &
  PIDS+=($!)

  # Poll ngrok API for the public URL (up to 15 s)
  for i in {1..15}; do
    NGROK_URL=$(
      curl -sf "$NGROK_API" 2>/dev/null |
      python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    tunnels = data.get('tunnels', [])
    https = [t['public_url'] for t in tunnels if t['proto'] == 'https']
    print(https[0] if https else '', end='')
except Exception:
    print('', end='')
" 2>/dev/null
    ) || NGROK_URL=""
    [[ -n "$NGROK_URL" ]] && break
    sleep 1
  done

  if [[ -n "$NGROK_URL" ]]; then
    ok "ngrok tunnel: $NGROK_URL"
  else
    warn "Could not obtain ngrok URL. Inspect /tmp/openbake_ngrok.log"
    warn "Falling back to emulator address for Android."
    NGROK_URL=""
  fi
fi

# ── 4. Android build config ───────────────────────────────────────────────────
sep
log "Updating Android BASE_URL…"

if [[ -n "$NGROK_URL" ]]; then
  ANDROID_API_URL="${NGROK_URL}/api/"
else
  # Physical device on the same network: use machine IP
  LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "10.0.2.2")
  ANDROID_API_URL="http://${LOCAL_IP}:${SERVER_PORT}/api/"
  warn "ngrok not available. Using local network IP: $ANDROID_API_URL"
  warn "Ensure your Android device/emulator can reach this address."
fi

# Replace buildConfigField BASE_URL in app/build.gradle.kts
# Uses a Python one-liner for safe in-place replacement (avoids sed escaping issues)
python3 - "$ANDROID_GRADLE" "$ANDROID_API_URL" <<'PYEOF'
import sys, re

filepath, new_url = sys.argv[1], sys.argv[2]
with open(filepath, 'r') as f:
    content = f.read()

# Match: buildConfigField("String", "BASE_URL", <any value until closing paren>)
# Handles Kotlin-escaped values like "\"http://...\""
pattern = r'(buildConfigField\("String",\s*"BASE_URL",\s*)([^)]+)(\))'

def make_replacement(m):
    # Write:  buildConfigField("String", "BASE_URL", "\"<url>\"")
    # In Python f-string: "\\" produces one backslash in the output
    return m.group(1) + '"' + '\\"' + new_url + '\\"' + '"' + m.group(3)

new_content, count = re.subn(pattern, make_replacement, content)

if count == 0:
    print(f"WARNING: BASE_URL field not found in {filepath}", file=sys.stderr)
else:
    with open(filepath, 'w') as f:
        f.write(new_content)
    print(f"Updated BASE_URL -> {new_url}")
PYEOF

ok "Android BASE_URL set to: $ANDROID_API_URL"

# ── 5. Android build & install (optional) ────────────────────────────────────
if $BUILD_ANDROID; then
  sep
  log "Building Android debug APK…"

  if [[ ! -f "$ANDROID_DIR/gradlew" ]]; then
    err "gradlew not found at $ANDROID_DIR/gradlew"
    exit 1
  fi

  # Determine Gradle targets based on whether a device is connected
  GRADLE_TARGETS="assembleDebug"
  DEVICE_CONNECTED=false

  if ! command -v adb &>/dev/null; then
    warn "adb not found — APK will be built but not installed. Install Android SDK platform-tools and add to PATH."
  else
    DEVICE_COUNT=$(adb devices 2>/dev/null | tail -n +2 | grep -c "device$" || true)
    if [[ "$DEVICE_COUNT" -lt 1 ]]; then
      warn "No Android device/emulator attached. Starting first available emulator…"

      # Try to start an emulator if avdmanager is available
      if command -v emulator &>/dev/null; then
        FIRST_AVD=$(emulator -list-avds 2>/dev/null | head -n 1)
        if [[ -n "$FIRST_AVD" ]]; then
          log "Launching AVD: $FIRST_AVD"
          emulator -avd "$FIRST_AVD" -no-snapshot-load > /tmp/openbake_emulator.log 2>&1 &
          PIDS+=($!)

          log "Waiting for Android emulator to boot (up to 120 s)…"
          adb wait-for-device
          # Wait for boot animation to finish
          BOOT=""
          for i in {1..60}; do
            BOOT=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') || BOOT=""
            [[ "$BOOT" == "1" ]] && break
            sleep 2
          done
          if [[ "$BOOT" == "1" ]]; then
            ok "Emulator booted."
            GRADLE_TARGETS="assembleDebug installDebug"
            DEVICE_CONNECTED=true
          else
            warn "Emulator may not be fully booted. Installing APK will be skipped."
          fi
        else
          warn "No AVDs found. Create one in Android Studio and re-run with --android."
        fi
      else
        warn "'emulator' command not in PATH. Start an emulator/device manually and re-run."
      fi
    else
      GRADLE_TARGETS="assembleDebug installDebug"
      DEVICE_CONNECTED=true
    fi
  fi

  log "Running: ./gradlew $GRADLE_TARGETS"
  GRADLE_RC=0
  (
    cd "$ANDROID_DIR"
    # shellcheck disable=SC2086
    ./gradlew $GRADLE_TARGETS 2>&1 | tee /tmp/openbake_gradle.log
  ) || GRADLE_RC=$?

  if [[ $GRADLE_RC -eq 0 ]]; then
    ok "Android APK built${DEVICE_CONNECTED:+ and installed} successfully."
    if $DEVICE_CONNECTED; then
      adb shell am start -n "com.saibabui.openbake/.MainActivity" 2>/dev/null || true
      ok "OpenBake launched on device."
    else
      APK_PATH=$(find "$ANDROID_DIR/app/build/outputs/apk/debug" -name "*.apk" 2>/dev/null | head -n 1 || true)
      [[ -n "$APK_PATH" ]] && ok "APK ready at: $APK_PATH"
    fi
  else
    err "Gradle build failed (exit $GRADLE_RC). See /tmp/openbake_gradle.log"
  fi
fi

# ── Summary ───────────────────────────────────────────────────────────────────
sep
echo ""
echo "${BOLD}${GREEN}  ✅  Stack is running  ${RESET}"
echo ""
echo "  ${BOLD}Server (API + Web)${RESET}  →  http://localhost:${SERVER_PORT}"
echo "  ${BOLD}API Docs${RESET}            →  http://localhost:${SERVER_PORT}/docs"
echo "  ${BOLD}MySQL${RESET}               →  localhost:${MYSQL_HOST_PORT} (container: ${MYSQL_CONTAINER})"
if [[ -n "$NGROK_URL" ]]; then
echo "  ${BOLD}ngrok${RESET}               →  ${NGROK_URL}"
echo "  ${BOLD}ngrok UI${RESET}            →  http://localhost:4040"
fi
echo "  ${BOLD}Android${RESET}             →  BASE_URL = ${ANDROID_API_URL}"
echo ""
echo "  Logs:"
echo "    Server   →  /tmp/openbake_server.log"
[[ $NGROK_AVAILABLE == true ]] && echo "    ngrok    →  /tmp/openbake_ngrok.log"
$BUILD_ANDROID && echo "    Gradle   →  /tmp/openbake_gradle.log" || true
echo ""
echo "  ${BOLD}Press Ctrl+C to stop the server + ngrok.${RESET} (MySQL container keeps running — see 'docker stop ${MYSQL_CONTAINER}')"
sep
echo ""

# Keep script alive so trap runs on Ctrl+C
wait
