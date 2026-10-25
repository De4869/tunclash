#!/system/bin/sh
MODDIR="${0%/*}"
# Play downloads break when IPv6 bypasses the IPv4-only tunnel
for i in $(ls /sys/class/net 2>/dev/null); do
  case "$i" in lo|mihomo0|tun*|docker*|veth*) continue ;; esac
  f="/proc/sys/net/ipv6/conf/$i/disable_ipv6"
  [ -w "$f" ] && echo 1 > "$f" 2>/dev/null
done
sleep 20
if [ -f /data/adb/mihomo_tproxy/auto_start ]; then
  sh /data/adb/mihomo_tproxy/ctl.sh start
fi
