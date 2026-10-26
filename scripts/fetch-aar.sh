#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.2.0}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/app/libs/Wavewalletdk.aar"
URL="https://github.com/lightninglabs/wavelength/releases/download/${VERSION}/Wavewalletdk.aar"

mkdir -p "$(dirname "$DEST")"

if [[ -f "$DEST" ]]; then
  echo "AAR already present: $DEST"
  exit 0
fi

echo "Downloading $URL"
curl -fL --retry 5 --retry-all-errors --retry-delay 5 -o "$DEST" "$URL"
ls -lh "$DEST"
