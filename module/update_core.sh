#!/system/bin/sh
# Online core updater for standalone (WebUI / terminal) usage.
DIR=/data/adb/mihomo_tproxy
LOG=$DIR/logs/update.log
API="https://gh-proxy.com/https://api.github.com/repos/MetaCubeX/mihomo/releases/latest"

mkdir -p $DIR/logs
: > $LOG
exec >> $LOG 2>&1

echo "checking latest release..."
curl -s -m 30 -o /data/local/tmp/rel.json "$API" || { echo "failed: api"; exit 1; }

TAG=$(sed -n 's/.*"tag_name": *"\([^"]*\)".*/\1/p' /data/local/tmp/rel.json | head -1)
URL=$(grep -o 'https://[^"]*mihomo-android-arm64-v8[^"]*\.gz' /data/local/tmp/rel.json | head -1)
if [ -z "$TAG" ] || [ -z "$URL" ]; then echo "failed: parse release"; exit 1; fi

CUR=$(cat $DIR/.version 2>/dev/null)
CUR=${CUR#v}
echo "latest: $TAG  installed: v$CUR"
if [ "${TAG#v}" = "$CUR" ]; then echo "already latest: $TAG"; exit 0; fi

echo "downloading $URL"
curl -L --max-time 900 -o /data/local/tmp/core.gz "https://gh-proxy.com/$URL" || { echo "failed: download"; exit 1; }
SIZE=$(wc -c < /data/local/tmp/core.gz 2>/dev/null)
echo "downloaded: $SIZE bytes"
if [ "${SIZE:-0}" -lt 5000000 ]; then echo "failed: file too small"; exit 1; fi

gzip -dc /data/local/tmp/core.gz > $DIR/bin/mihomo.new || { echo "failed: gunzip"; exit 1; }
chmod 755 $DIR/bin/mihomo.new
mv -f $DIR/bin/mihomo.new $DIR/bin/mihomo
echo "${TAG#v}" > $DIR/.version
sh $DIR/ctl.sh restart
$DIR/bin/mihomo -v | head -1
rm -f /data/local/tmp/core.gz /data/local/tmp/rel.json
echo "updated to $TAG"
