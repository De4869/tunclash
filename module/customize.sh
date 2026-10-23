#!/system/bin/sh
SKIPUNZIP=0
ui_print "****************************************"
ui_print "  Mihomo TProxy 内核 v1.0.0"
ui_print "  mihomo 透明代理 · TUN 全局接管"
ui_print "****************************************"

DIR=/data/adb/mihomo_tproxy
mkdir -p $DIR/bin $DIR/geo $DIR/config $DIR/logs $DIR/run $DIR/data
if [ -x $DIR/bin/mihomo ] && [ -f $DIR/.version ]; then
  ui_print "- 保留已安装内核 $(cat $DIR/.version)"
else
  ui_print "- 安装内核"
  cp -f $MODPATH/bin/mihomo $DIR/bin/mihomo
  chmod 755 $DIR/bin/mihomo
  echo "1.19.14" > $DIR/.version
fi
ui_print "- 安装 geo 数据库"
cp -f $MODPATH/geo/geoip.metadb $DIR/data/geoip.metadb 2>/dev/null
cp -f $MODPATH/geo/geosite.dat $DIR/data/geosite.dat 2>/dev/null
ui_print "- 安装 WebUI 运行时"
cp -f $MODPATH/gen.sh $DIR/gen.sh
chmod 755 $DIR/gen.sh
[ -f $DIR/subs.list ] || : > $DIR/subs.list
cp -f $MODPATH/update_core.sh $DIR/update_core.sh
chmod 755 $DIR/update_core.sh
ui_print "- 安装控制脚本"
cp -f $MODPATH/ctl.sh $DIR/ctl.sh
chmod 755 $DIR/ctl.sh
if [ ! -f $DIR/.version ]; then echo "1.19.14" > $DIR/.version; fi
ui_print "- 完成，重启后由 App 控制启停"
