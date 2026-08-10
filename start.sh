#!/usr/bin/env bash
# ShaderCreator starten (Linux/macOS)
cd "$(dirname "$0")"
if command -v node >/dev/null 2>&1; then
  exec node tools/serve.mjs "${1:-8123}"
elif command -v python3 >/dev/null 2>&1; then
  echo "Node.js nicht gefunden – nutze Python-Server auf http://localhost:${1:-8123}"
  exec python3 -m http.server "${1:-8123}"
else
  echo "Bitte Node.js (https://nodejs.org) oder Python 3 installieren."
  exit 1
fi
