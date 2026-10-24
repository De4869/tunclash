#!/system/bin/sh
# Standalone config generator for the module (no app required).
# Reads $DIR/subs.list (name|url per line) and $DIR/data/subN.yaml provider files.
DIR=/data/adb/mihomo_tproxy
DATA=$DIR/data
LIST=$DIR/subs.list
SECRETFILE=$DIR/secret
CFG=$DATA/config.yaml
TMP=$DATA/.config.tmp

mkdir -p $DATA

SECRET=$(cat $SECRETFILE 2>/dev/null)
if [ -z "$SECRET" ]; then
  SECRET=$(cat /proc/sys/kernel/random/uuid | tr -d '-' | head -c 16)
  echo "$SECRET" > $SECRETFILE
  chmod 600 $SECRETFILE
fi

USED=""
USECSV=""
i=0
if [ -f "$LIST" ]; then
  while IFS='|' read -r name url; do
    [ -z "$name" ] && continue
    f="sub${i}.yaml"
    if [ -f "$DATA/p$i.yaml" ] && grep -q "name:" "$DATA/p$i.yaml"; then
      USED="$USED sub$i"
    elif [ -f "$DATA/$f" ] && grep -q "proxies:" "$DATA/$f"; then
      # a subscription is a full Clash config; a provider must contain only the proxies list
      awk 'BEGIN{p=0} /^proxies:/{p=1; print; next} p && /^[A-Za-z0-9_-]+:/{exit} p{print}' "$DATA/$f" > "$DATA/p$i.yaml"
      if [ "$(grep -c 'name:' "$DATA/p$i.yaml" 2>/dev/null)" -gt 0 ]; then
        USED="$USED sub$i"
      fi
    fi
    i=$((i+1))
  done < "$LIST"
fi


# ---- merge every provider into one list, de-duplicated by type+server+port+credential ----
MERGE="$DATA/all.yaml"
cat > "$DATA/dedupe.awk" <<'AWKEOF'
BEGIN { Q = sprintf("%c", 39) }
function qv(line, key,   i, s, j) {
  i = index(line, key ": ")
  if (!i) return ""
  s = substr(line, i + length(key) + 2)
  if (substr(s, 1, 1) == Q) { s = substr(s, 2); j = index(s, Q); if (j) s = substr(s, 1, j - 1) }
  else { j = index(s, ","); if (j) s = substr(s, 1, j - 1) }
  return s
}
/^[ \t]*- \{/ {
  line = $0; sub(/^[ \t]+/, "", line)
  n = qv(line, "name")
  if (n ~ /剩余流量|套餐|到期|过期|已用流量|重置|官网|订阅|客服|试用/) next
  # mihomo requires unique proxy names inside one provider, so de-duplicate by name
  k = n
  if (k in seen) next
  seen[k] = 1
  print line
}
AWKEOF

{
  echo "proxies:"
  for u in $USED; do
    idx=${u#sub}
    [ -f "$DATA/p$idx.yaml" ] && cat "$DATA/p$idx.yaml"
  done | awk -f "$DATA/dedupe.awk"
} > "$MERGE"
MERGED=$(grep -c 'name:' "$MERGE" 2>/dev/null)

USECSV=$(echo $USED | tr ' ' ',')
{
  echo "mixed-port: 7890"
  echo "allow-lan: false"
  echo "bind-address: 127.0.0.1"
  echo "mode: rule"
  echo "log-level: info"
  echo "ipv6: false"
  echo "unified-delay: true"
  echo "tcp-concurrent: true"
  echo "find-process-mode: strict"
  echo "external-controller: 127.0.0.1:9090"
  echo "secret: \"$SECRET\""
  echo "geodata-mode: false"
  echo "geo-auto-update: false"
  echo "profile:"
  echo "  store-selected: true"
  echo "  store-fake-ip: true"
  echo "sniffer:"
  echo "  enable: true"
  echo "  force-dns-mapping: true"
  echo "  parse-pure-ip: true"
  echo "  override-destination: true"
  echo "  sniff:"
  echo "    TLS:"
  echo "      ports: [443, 8443]"
  echo "    HTTP:"
  echo "      ports: [80, 8080-8880]"
  echo "    QUIC:"
  echo "      ports: [443, 8443]"
  echo "tun:"
  echo "  enable: true"
  echo "  stack: mixed"
  echo "  device: mihomo0"
  echo "  auto-route: true"
  echo "  auto-detect-interface: true"
  echo "  dns-hijack:"
  echo "    - any:53"
  echo "  mtu: 9000"
  echo "dns:"
  echo "  enable: true"
  echo "  listen: 0.0.0.0:1053"
  echo "  ipv6: false"
  echo "  enhanced-mode: fake-ip"
  echo "  fake-ip-range: 198.18.0.1/16"
  echo "  fake-ip-filter:"
  echo "    - \"*.lan\""
  echo "    - \"*.local\""
  echo "    - \"+.msftconnecttest.com\""
  echo "    - \"+.msftncsi.com\""
  echo "  default-nameserver:"
  echo "    - 223.5.5.5"
  echo "    - 119.29.29.29"
  echo "  proxy-server-nameserver:"
  echo "    - 223.5.5.5"
  echo "    - 119.29.29.29"
  echo "  nameserver:"
  echo "    - \"https://dns.alidns.com/dns-query\""
  echo "    - \"https://doh.pub/dns-query\""
  echo "    - 223.5.5.5"
  echo ""
  if [ -n "$USED" ]; then
    echo "proxy-providers:"
    echo "  all:"
    echo "    type: file"
    echo "    path: ./all.yaml"
    echo "    health-check:"
    echo "      enable: true"
    echo "      url: http://www.gstatic.com/generate_204"
    echo "      interval: 300"
    echo "proxy-groups:"
    echo "  - name: 🚀 节点选择"
    echo "    type: select"
    echo "    proxies: [♻️ 自动选择, 🔯 故障转移, DIRECT]"
    echo "    use: [all]"
    echo "  - name: ♻️ 自动选择"
    echo "    type: url-test"
    echo "    url: http://www.gstatic.com/generate_204"
    echo "    interval: 300"
    echo "    tolerance: 50"
    echo "    lazy: false"
    echo "    use: [all]"
    echo "  - name: 🔯 故障转移"
    echo "    type: fallback"
    echo "    url: http://www.gstatic.com/generate_204"
    echo "    interval: 300"
    echo "    use: [all]"
    for r in "🇭🇰 香港:香港|HK|Hong Kong" "🇯🇵 日本:日本|JP|Japan|东京|大阪" "🇸🇬 新加坡:新加坡|SG|Singapore|狮城" "🇺🇸 美国:美国|US|United States|洛杉矶|圣何塞" "🇹🇼 台湾:台湾|TW|Taiwan|台北" "🇰🇷 韩国:韩国|KR|Korea|首尔" "🇬🇧 英国:英国|UK|London|伦敦" "🇩🇪 德国:德国|DE|Germany|法兰克福" "🇫🇷 法国:法国|FR|France|巴黎" "🇳🇱 荷兰:荷兰|NL|Amsterdam|阿姆斯特丹"; do
      n=${r%%:*}
      f=${r#*:}
      echo "  - name: $n"
      echo "    type: url-test"
      echo "    url: http://www.gstatic.com/generate_204"
      echo "    interval: 300"
      echo "    tolerance: 50"
      echo "    use: [all]"
      echo "    filter: \"$f\""
    done
    echo "rules:"
    echo "  - GEOSITE,geolocation-!cn,🚀 节点选择"
    echo "  - GEOSITE,cn,DIRECT"
    echo "  - GEOIP,CN,DIRECT"
    echo "  - MATCH,🚀 节点选择"
  else
    echo "proxies: []"
    echo "proxy-groups: []"
    echo "rules:"
    echo "  - MATCH,DIRECT"
  fi
} > "$TMP"

mv -f "$TMP" "$CFG"
chmod 644 "$CFG"
echo "config written: $CFG (providers:$USED merged:$MERGED nodes)"
