#!/usr/bin/env bash
# Downloads the mihomo core + geo databases used by the app and the module.
# Usage: bash scripts/fetch-deps.sh [mihomo-version]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${1:-v1.19.32}"
MIRROR="${MIRROR:-https://gh-proxy.com/}"

echo "==> mihomo ${VERSION}"
TMP="$(mktemp -d)"
curl -fL --retry 3 -o "$TMP/mihomo.gz" \
  "${MIRROR}https://github.com/MetaCubeX/mihomo/releases/download/${VERSION}/mihomo-android-arm64-v8-${VERSION}.gz"

mkdir -p "$ROOT/module/bin" "$ROOT/module/geo" "$ROOT/Android/app/src/main/assets"
gunzip -c "$TMP/mihomo.gz" > "$ROOT/module/bin/mihomo"
chmod +x "$ROOT/module/bin/mihomo"
cp "$TMP/mihomo.gz" "$ROOT/Android/app/src/main/assets/mihomo.gz"

echo "==> geo databases"
for f in geoip.metadb geosite.dat; do
  curl -fL --retry 3 -o "$ROOT/module/geo/$f" \
    "${MIRROR}https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/$f"
  cp "$ROOT/module/geo/$f" "$ROOT/Android/app/src/main/assets/$f"
done

rm -rf "$TMP"
echo "done: $(du -h "$ROOT/module/bin/mihomo" | cut -f1) core, geo ok"
