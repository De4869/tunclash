# 2026-11-15 修复记录：VoLTE/IMS 与 Google Play 下载

## 概述

本次修复解决两个相互独立、但都表现为“网络正常但功能不可用”的问题：

1. 部分运营商（IMS PDN 为 IPv6-only，例如中国移动/中国电信 5G SA）在代理运行期间，
   会出现 **5G 信号和移动数据正常，但打不了电话、收不到短信，来电提示“暂时无法接通”**；
   重启手机后暂时恢复，过一段时间或切换网络后再次复发。
2. Google Play 下载/更新一直停在 **“等待中”/0%**，但 Play 商店本身可以打开、账号正常。

根因分别是：**系统层全局封禁 IPv6**，以及 **Play 下载 CDN 被规则强制 DIRECT**。

---

## 根因分析

### 1. 电话/短信问题：全局 IPv6 封禁打断了 IMS

旧版在 `ipv6: false` 时执行：

```sh
ip6tables -I OUTPUT 2 -j REJECT
```

这是系统级、所有进程生效的 IPv6 出站拒绝。

但国内移动/电信的 IMS PDN 是 **IPv6-only**：

- 语音（VoLTE）和短信（SMS over IMS）都依赖 IMS 注册；
- 移动数据走另一条默认 PDN（IPv4），所以表现为“能上网，但不能打电话/收短信”；
- 开机时 IMS 往往先于代理启动完成注册（`service.sh` 有 20 秒延迟），所以重启后能暂时恢复；
- 之后只要 IMS 需要重新注册（切网、信号变化、飞行模式、IMS 刷新），IPv6 被拒绝，
  注册失败，电话/短信一起失效。

这不是“信号差”，也不是 TikTok/LSPosed 模块导致的。

### 2. Play 下载问题：CDN 域名走了 DIRECT

Play 下载流程：

```text
services.googleapis.cn/download/by-token/...  (经代理)
  -> 302 redirect
  -> rr4---sn-....xn--ngstr-lra8j.com          (Play CDN)
```

旧规则把 `xn--ngstr-lra8j.com` 设为 `DIRECT`。该 CDN 直连在当前网络下无法传输，
Play 的 `CronetDownloader` 收到重定向后 `bytes_downloaded=0`，因此一直显示“等待中”。

同类 Play CDN 域名还有：

- `gvt1.com`
- `gvt2.com`
- `dl.google.com`

### 3. 为什么只把 `IP-CIDR6` 规则置顶还不够

mihomo 默认开启：

```yaml
sniffer:
  override-destination: true
```

Play 的 IPv6 连接会被 sniffer 识别成域名（例如 `rr4---....xn--ngstr-lra8j.com`），
规则引擎优先匹配域名规则（`GEOSITE`/`DOMAIN-SUFFIX`），`IP-CIDR6` 规则可能轮不到。
因此最终采用 **TUN 出口限定拒绝**，不依赖域名/IP 规则顺序。

---

## 修复内容

### 1. IPv6：TUN 双栈 + 只拒绝 TUN 出口

配置生成：

- `ipv6: true`（顶层，保持内核双栈）
- `tun.ipv6: true`（TUN 始终有 IPv6 地址和路由）
- `dns.ipv6` 跟随用户设置：关闭时不返回 AAAA
- 用户关闭 IPv6 时，规则中加入 `IP-CIDR6,::/0,REJECT`

`ctl.sh`：

- 不再写任何全局 `ip6tables REJECT`；
- 当配置包含 `IP-CIDR6,::/0,REJECT` 时，仅添加：

```sh
ip6tables -I OUTPUT 1 -o mihomo0 -j REJECT --reject-with icmp6-port-unreachable
```

- 这条规则只影响 **发往代理 TUN（mihomo0）** 的 IPv6；
- IMS/VoLTE 使用 `rmnet_dataX`，永远不会命中 `mihomo0`，因此电话/短信不受影响；
- `cleanup_ipv6()` 在启动/停止/清理时移除旧版本的全局规则、选择性规则、
  Play/GMS 规则和当前 TUN 规则，升级用户无需手动清理。

### 2. Play CDN：内置规则跟随代理组

以下域名改为跟随当前选中的代理组，不再 DIRECT：

```yaml
- DOMAIN-SUFFIX,xn--ngstr-lra8j.com,<代理组>
- DOMAIN-SUFFIX,gvt1.com,<代理组>
- DOMAIN-SUFFIX,gvt2.com,<代理组>
- DOMAIN,dl.google.com,<代理组>
```

这些是 **内置规则**（`ConfigComposer.playCdnRules()`），不来自 ACL4SSR/GEOSITE/订阅，并且在
“从节点生成配置”和“provider/订阅原样配置”两条路径里都放在：

- 用户 per-app 规则之后；
- 广告规则集（`RULE-SET,ad0`）和 `GEOSITE` 之前；

因此后续“规则自动更新”不会覆盖它们，也不会把它们误判成广告而 REJECT。

### 3. 业务分流组（新增）

使用 mihomo 内置 `GEOSITE`/`GEOIP`，不引入远程规则库：

| 组 | 规则 | 默认 |
|---|---|---|
| `🤖 AI` | `GEOSITE/GEOIP,openai`、`GEOSITE,anthropic`、Gemini/Claude 域名 | `🚀 节点选择` |
| `📺 流媒体` | `GEOSITE/GEOIP,netflix`、`disney`、`youtube`、`spotify`、Prime Video 域名 | `🚀 节点选择` |
| `✈️ Telegram` | `GEOSITE/GEOIP,telegram` | `🚀 节点选择` |
| `🎮 游戏` | `GEOSITE,steam`、`epicgames` | `DIRECT` |
| `🍎 Apple` | `GEOSITE,apple` | `DIRECT` |
| `Ⓜ️ Microsoft` | `GEOSITE,microsoft` | `DIRECT` |

所有业务组都是 `select`，切换立即生效；组内引用 `♻️ 自动选择` / `🔯 故障转移` /
地区组，底层 `url-test`/`fallback` 改为 `interval: 180`、`lazy: false`，网络切换后恢复更快。

内置 Play CDN 四条规则仍然排在最前面，业务规则不会影响 Play 下载。

### 4. 文档/UI

- `SettingsScreen.kt`：IPv6 关闭说明改为“应用 IPv6 在核心里拒绝，不影响电话”；
- `README.md`：补充 IMS 兼容性、TUN 双栈、Play CDN 说明；
- `CHANGELOG.md`：记录本次修复。

---

## 文件改动

| 文件 | 说明 |
|---|---|
| `Android/app/src/main/java/com/mihomo/tproxy/core/ConfigComposer.kt` | TUN 始终双栈；IPv6 关闭时加 `IP-CIDR6`；Play CDN 内置规则置于广告规则前并跟随代理组；新增 AI/流媒体/Telegram/游戏/Apple/Microsoft 业务组与规则 |
| `Android/app/src/main/assets/ctl.sh` | 与模块 `ctl.sh` 同步：清理旧规则、TUN 限定 IPv6 拒绝 |
| `Android/app/src/main/java/com/mihomo/tproxy/ui/SettingsScreen.kt` | IPv6 设置说明 |
| `module/ctl.sh` | `TUN=mihomo0`；`ipv6_disabled()`；`block_tun_ipv6()`；清理旧版全局/Play/选择性规则 |
| `module/gen.sh` | 顶层/TUN `ipv6: true`；`IP-CIDR6`；Play CDN 走代理；业务分组与规则 |
| `_tools/playfix.sh` | 诊断脚本里的 Play CDN 也改为跟随代理组，避免手动修复时写回 DIRECT |
| `README.md` | 行为说明、兼容性、Play CDN |
| `CHANGELOG.md` | 变更记录 |
| `docs/release-notes-2026-11-15.md` | 本文件 |

---

## 验证记录

设备：

- Xiaomi 15（24129PN74C）
- Android 17 / HyperOS 4.0.4
- KernelSU + Zygisk Next + LSPosed 2.2.1
- 双卡：中国移动 + 中国电信，均 5G SA

### 电话/短信

- 代理运行、TUN 双栈、IPv6 关闭（规则 `IP-CIDR6,::/0,REJECT`）；
- 开关飞行模式后约 46 秒，`isImsRegistered=true`；
- 两张卡均 `mVoiceRegState=IN_SERVICE`、`mDataRegState=IN_SERVICE`；
- 系统层 `ip6tables -S OUTPUT` 无全局 REJECT；
- TUN 限定规则 `-o mihomo0` 生效，但 IMS 不受影响。

### 分流组配置

- 用 `mihomo -t` 验证了包含 6 个业务组、`select` 组嵌套、`url-test`/`fallback` 和
  `GEOSITE`/`GEOIP` 规则的配置，验证通过；
- 设备 geosite 支持：`openai`、`anthropic`、`netflix`、`disney`、`youtube`、`telegram`、
  `spotify`、`apple`、`microsoft`、`steam`、`epicgames`、`twitter`、`tiktok`、`google` 等；
  `claude` 缺失时用 `anthropic`，`gemini` 用手动域名补；
- geoip 的 `openai`、`netflix`、`disney`、`telegram` 分类均验证可用；
- Play CDN 规则仍排在业务规则之前，未受分流组改动影响。

### Play 下载

- 修复前：`CronetDownloader: onRedirectReceived`，`bytes_downloaded=0`，一直“等待中”；
- 将 `xn--ngstr-lra8j.com` 改为代理后：
  - `Bitget Wallet` 更新到 9.69.0，`lastUpdateTime=2026-11-15 23:23:43`；
  - `ChatGPT` 更新到 1.2026.307，`lastUpdateTime=2026-11-15 23:25:04`；
  - Play 下载目录出现真实 MB 级分片文件；
  - 核心日志显示 CDN 连接匹配 `DOMAIN-SUFFIX(xn--ngstr-lra8j.com)` 并使用代理组；
- 随后将 `gvt1.com`/`gvt2.com`/`dl.google.com` 一并纳入内置代理规则；
- `mihomo -t` 配置校验通过。

### 回归验证

- `sh -n module/ctl.sh`、`sh -n module/gen.sh` 语法检查通过；
- `ip6tables -S OUTPUT` 只剩系统/OEM 链，无应用层全局 IPv6 规则；
- TUN 上有 IPv6 地址 `fdfe:dcba:9876::1/126` 和 IPv6 路由。

---

## 升级说明

- 模块用户更新后，首次启动会执行 `cleanup_ipv6()` 自动清理旧版写入的全局 IPv6 规则；
- App 用户需要使用新 APK（`ConfigComposer` 已更新）才会生成新配置；
- 仅更新模块、不更新 App 的用户：模块的 `ctl.sh` 会清理旧规则，但 App 生成配置时仍可能写回旧逻辑，建议同时更新；
- 本次不改变订阅/节点格式，不需要重新导入订阅。

---

## 构建/发布

```bash
cd Android
./gradlew :app:assembleRelease
```

模块 zip 按仓库现有 CI/打包流程重新生成。发布前确认：

- `Android/app/src/main/assets/ctl.sh` 与 `module/ctl.sh` 内容一致；
- `module/gen.sh` 与 `ConfigComposer.kt` 的规则一致；
- 新 APK 的 `ConfigComposer` 会生成：
  - `ipv6: true`
  - `tun.ipv6: true`
  - `dns.ipv6: <用户设置>`
  - 用户关闭 IPv6 时 `IP-CIDR6,::/0,REJECT`
  - Play CDN 四条代理规则

---

## 提交信息建议

```text
fix: keep VoLTE/IMS working and fix Google Play downloads

- stop blocking IPv6 globally; keep TUN dual-stack and reject IPv6 only
  on the mihomo0 egress when the user disables IPv6
- remove the legacy global ip6tables REJECT that broke IMS/VoLTE on
  IPv6-only carriers (calls/SMS failed while mobile data kept working)
- clean legacy IPv6 rules on start/stop/cleanup
- route Play download CDNs (xn--ngstr-lra8j.com, gvt1.com, gvt2.com,
  dl.google.com) through the selected proxy group instead of DIRECT
- put Play CDN rules before ad rule sets/GEOSITE so rule updates cannot
  override or reject them
- add built-in business groups (AI, streaming, Telegram, game, Apple,
  Microsoft) using bundled GEOSITE/GEOIP; 180s non-lazy health checks
- update settings text, README, CHANGELOG and release notes
```

## 发布说明建议（用户可见）

```markdown
### 修复

- 修复部分运营商（IMS 为 IPv6-only，如移动/电信 5G SA）在代理运行时
  无法打电话、收不到短信、来电提示“暂时无法接通”的问题。
  IPv6 不再在系统层全局封禁，改为只拒绝发往代理 TUN 的 IPv6，
  电话/短信链路不受影响。

- 修复 Google Play 下载/更新一直停在“等待中”或 0% 的问题。
  Play 下载 CDN（xn--ngstr-lra8j.com、gvt1.com、gvt2.com、dl.google.com）
  改为跟随当前代理节点；之前这些域名走 DIRECT，直连拉不动下载。
  这些是内置规则，后续规则自动更新不会覆盖。

### 改进

- 新增业务分流组：AI、流媒体、Telegram、游戏、Apple、Microsoft，
  可以分别选择节点/地区组，切换立即生效。
- 基于内置 geosite/geoip，不依赖远程规则库，不会因为外部规则更新影响下载；
  Play CDN 规则始终排在最前。
- 节点健康检查间隔调整为 180 秒并关闭 lazy，切换网络后恢复更快。
```
