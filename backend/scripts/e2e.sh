#!/bin/bash
# E2E contract check: replays, byte for byte, what the mod sends
# (dev-link flow, %config save/load, /me) and what the site sends
# (register/login/confirm) against a live gateway, and asserts every shape the
# Java client reads back (device_code/user_code/expires_in, 202-pending,
# lvd_ tokens, share_code round-trip, 404 + error body, avatar ?v= bust).
#
# Usage: bash backend/scripts/dev-stack.sh && bash backend/scripts/e2e.sh
#
# Note: gateway rate limits live in memory, so re-running dev-stack.sh resets
# them - otherwise the 3/hour register limit will fail the run.
set -uo pipefail
GW=http://127.0.0.1:8080
JAR=$(mktemp)
trap 'rm -f "$JAR"' EXIT
pass=0; fail=0
ck() { # ck "<name>" "<expected>" "<actual>"
  if [ "$2" = "$3" ]; then pass=$((pass+1)); printf 'ok   %s\n' "$1"
  else fail=$((fail+1)); printf 'FAIL %s\n     expected: %s\n     actual:   %s\n' "$1" "$2" "$3"; fi
}
jqv() { jq -r "$1 // null"; }

# --- mod: POST /device/code -------------------------------------------------
resp=$(curl -s -w '\n%{http_code}' -X POST "$GW/device/code" \
  -H 'Content-Type: application/json' -H 'Accept: application/json' -d '{}')
code=$(tail -1 <<<"$resp"); body=$(sed '$d' <<<"$resp")
ck "device/code status (2xx)" "2xx" "$([[ "$code" =~ ^2..$ ]] && echo 2xx || echo "$code")"
DEVICE_CODE=$(jqv .device_code <<<"$body")
USER_CODE=$(jqv .user_code <<<"$body")
EXPIRES=$(jqv .expires_in <<<"$body")
ck "device_code non-empty" "true" "$([ -n "$DEVICE_CODE" ] && [ "$DEVICE_CODE" != null ] && echo true || echo false)"
ck "user_code non-empty"  "true" "$([ -n "$USER_CODE" ] && [ "$USER_CODE" != null ] && echo true || echo false)"
ck "expires_in numeric"   "true" "$([[ "$EXPIRES" =~ ^[0-9]+$ ]] && echo true || echo false)"

# --- mod: first poll must be 202 (ApiResult.isPending) ----------------------
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/device/token" \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d "{\"device_code\":\"$DEVICE_CODE\"}")
ck "device/token pending -> 202" "202" "$code"

# --- site: register + login + confirm the user code -------------------------
EMAIL="e2e-$(date +%s%N)@example.com"
code=$(curl -s -o /dev/null -w '%{http_code}' -c "$JAR" -X POST "$GW/auth/register" \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"correct-horse-battery-staple\",\"nick\":\"E2E\"}")
ck "auth/register status" "201" "$code"

resp=$(curl -s -w '\n%{http_code}' -c "$JAR" -b "$JAR" -X POST "$GW/auth/login" \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"correct-horse-battery-staple\"}")
code=$(tail -1 <<<"$resp"); body=$(sed '$d' <<<"$resp")
ck "auth/login status" "200" "$code"
ACCESS=$(jqv .access_token <<<"$body")
ck "login returns access_token" "true" "$([ -n "$ACCESS" ] && [ "$ACCESS" != null ] && echo true || echo false)"

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/device/confirm" \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -H "Authorization: Bearer $ACCESS" -d "{\"user_code\":\"$USER_CODE\"}")
ck "device/confirm status" "200" "$code"

# --- mod: poll until linked -------------------------------------------------
DEVICE_TOKEN=""
for _ in 1 2 3 4 5; do
  resp=$(curl -s -w '\n%{http_code}' -X POST "$GW/device/token" \
    -H 'Content-Type: application/json' -H 'Accept: application/json' \
    -d "{\"device_code\":\"$DEVICE_CODE\"}")
  code=$(tail -1 <<<"$resp"); body=$(sed '$d' <<<"$resp")
  if [ "$code" = "200" ]; then DEVICE_TOKEN=$(jqv .device_token <<<"$body"); break; fi
  sleep 1
done
ck "device/token linked status" "200" "$code"
ck "device_token has lvd_ prefix" "true" "$([[ "$DEVICE_TOKEN" == lvd_* ]] && echo true || echo false)"
ck "device_code is single-use (second collect -> 404)" "404" \
  "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/device/token" \
      -H 'Content-Type: application/json' -H 'Accept: application/json' \
      -d "{\"device_code\":\"$DEVICE_CODE\"}")"

# --- mod: GET /me -----------------------------------------------------------
resp=$(curl -s -w '\n%{http_code}' -H "Authorization: Bearer $DEVICE_TOKEN" -H 'Accept: application/json' "$GW/me")
code=$(tail -1 <<<"$resp"); body=$(sed '$d' <<<"$resp")
ck "GET /me status" "200" "$code"
ck "/me email matches" "$EMAIL" "$(jqv .email <<<"$body")"

# --- mod: %config save 1  ->  PUT /configs/1 --------------------------------
DATA='{"modules":{"Reach":true},"theme":{"accent":"#FF5CC8E7"}}'
resp=$(curl -s -w '\n%{http_code}' -X PUT "$GW/configs/1" \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -H "Authorization: Bearer $DEVICE_TOKEN" \
  -d "{\"name\":\"e2e slot\",\"data\":$DATA}")
code=$(tail -1 <<<"$resp"); body=$(sed '$d' <<<"$resp")
ck "PUT /configs/1 status" "200" "$code"
SHARE_CODE=$(jqv .share_code <<<"$body")
ck "PUT response carries share_code (8 chars, no 0O1IL)" "true" \
  "$([[ "$SHARE_CODE" =~ ^[2-9A-HJ-NP-Z]{8}$ ]] && echo true || echo false)"
ck "PUT echoes the data back" "$DATA" "$(jqv .data <<<"$body" | jq -c .)"

# --- mod: GET /configs (slot list) ------------------------------------------
resp=$(curl -s -w '\n%{http_code}' -H "Authorization: Bearer $DEVICE_TOKEN" -H 'Accept: application/json' "$GW/configs")
code=$(tail -1 <<<"$resp"); body=$(sed '$d' <<<"$resp")
ck "GET /configs status" "200" "$code"
ck "slot 1 present with our name" "e2e slot" "$(jqv '.[] | select(.slot==1) | .name' <<<"$body")"

# --- mod: %config load <code>  ->  GET /configs/shared/{code} (no auth) -----
resp=$(curl -s -w '\n%{http_code}' -H 'Accept: application/json' "$GW/configs/shared/$SHARE_CODE")
code=$(tail -1 <<<"$resp"); body=$(sed '$d' <<<"$resp")
ck "GET /configs/shared/{code} status" "200" "$code"
ck "shared name round-trips" "e2e slot" "$(jqv .name <<<"$body")"
ck "shared data round-trips" "$DATA" "$(jqv .data <<<"$body" | jq -c .)"

# --- mod: unknown code -> 404 (ApiResult.isNotFound -> friendly message) ----
resp=$(curl -s -w '\n%{http_code}' -H 'Accept: application/json' "$GW/configs/shared/QQQQQQQQ")
code=$(tail -1 <<<"$resp"); body=$(sed '$d' <<<"$resp")
ck "unknown share code -> 404" "404" "$code"
ck "404 body carries error field" "true" \
  "$([ "$(jqv .error <<<"$body")" != "null" ] && echo true || echo false)"

# --- easter egg: %config load IDDQD -----------------------------------------
resp=$(curl -s -w '\n%{http_code}' -H 'Accept: application/json' "$GW/configs/shared/IDDQD")
code=$(tail -1 <<<"$resp"); body=$(sed '$d' <<<"$resp")
ck "IDDQD -> 200" "200" "$code"
ck "IDDQD is god mode (only ChinaHat on)" "true" \
  "$(jqv '.data.modules.ChinaHat.enabled // .data.ChinaHat.enabled // "missing"' <<<"$body")"

# --- site: avatar upload + cache-busted serve (through the gateway) --------
PNG=$(mktemp --suffix=.png)
python3 -c "
import struct,zlib,sys
w,h=8,8
raw=b''.join(b'\x00'+b'\xff\x00\x00\xff'*w for _ in range(h))
def chunk(t,d):
    c=t+d
    return struct.pack('>I',len(d))+c+struct.pack('>I',zlib.crc32(c)&0xffffffff)
open('$PNG','wb').write(b'\x89PNG\r\n\x1a\n'
  +chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))
  +chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
"
resp=$(curl -s -w '\n%{http_code}' -X POST "$GW/avatars" \
  -H "Authorization: Bearer $DEVICE_TOKEN" -H 'Accept: application/json' \
  -F "file=@$PNG;type=image/png")
code=$(tail -1 <<<"$resp"); body=$(sed '$d' <<<"$resp")
ck "POST /avatars status" "200" "$code"
AVATAR_URL=$(jqv .avatar_url <<<"$body")
ck "avatar_url points through the gateway" "true" \
  "$([[ "$AVATAR_URL" == http://127.0.0.1:8080/media/avatars/*.png ]] && echo true || echo false)"
ck "avatar served through the gateway" "200" \
  "$(curl -s -o /dev/null -w '%{http_code}' "$AVATAR_URL")"
resp=$(curl -s -H "Authorization: Bearer $DEVICE_TOKEN" -H 'Accept: application/json' "$GW/me")
ck "GET /me avatar_url cache-busted with ?v=" "true" \
  "$(jq -r '.avatar_url // ""' <<<"$resp" | grep -qE '\?v=[0-9]+$' && echo true || echo false)"
rm -f "$PNG"

# --- CORS: the site's origin must be allowed --------------------------------
pref=$(curl -s -o /dev/null -w '%{http_code}' -X OPTIONS "$GW/configs" \
  -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: GET' \
  -H 'Access-Control-Request-Headers: authorization')
ck "CORS preflight for site origin" "200" "$pref"

echo
echo "passed=$pass failed=$fail"
[ "$fail" -eq 0 ]
