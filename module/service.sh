#!/system/bin/sh
MODDIR="${0%/*}"
sleep 20
if [ -f /data/adb/mihomo_tproxy/auto_start ]; then
  sh /data/adb/mihomo_tproxy/ctl.sh start
fi
