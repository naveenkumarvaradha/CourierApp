#!/usr/bin/env bash
# Generates a self-signed TLS certificate for local/dev use.
#
# For a real deployment, replace the generated files with a CA-issued
# certificate (e.g. via Let's Encrypt/certbot) for your actual domain —
# self-signed certs will always show a browser warning.
#
# Usage: ./scripts/generate-self-signed-cert.sh [hostname-or-ip ...]
# Example: ./scripts/generate-self-signed-cert.sh courierapp.example.com 192.168.2.245

set -euo pipefail
export MSYS_NO_PATHCONV=1  # avoid Git-Bash-on-Windows mangling "/CN=..." into a filesystem path
cd "$(dirname "$0")/.."

OUT_DIR="frontend/certs"
mkdir -p "$OUT_DIR"

HOSTS=("$@")
if [ ${#HOSTS[@]} -eq 0 ]; then
  HOSTS=("localhost" "127.0.0.1")
fi

SAN=""
for h in "${HOSTS[@]}"; do
  if [[ "$h" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    SAN="${SAN}IP:${h},"
  else
    SAN="${SAN}DNS:${h},"
  fi
done
SAN="${SAN%,}"

openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
  -keyout "$OUT_DIR/server.key" \
  -out "$OUT_DIR/server.crt" \
  -subj "/CN=${HOSTS[0]}" \
  -addext "subjectAltName=${SAN}"

echo "Generated $OUT_DIR/server.crt and $OUT_DIR/server.key for: ${HOSTS[*]}"
echo "These are gitignored — regenerate on each host, or replace with a real CA cert for production."
