# 2026-11-18 UI 优化（P0）

只改视觉层，不涉及 `ConfigComposer` / `ctl.sh` / 任何代理、分流、IPv6 逻辑。

## 改动内容

### 1. 对比度与排版（`Theme.kt`）

| Token | 旧值 | 新值 |
|---|---|---|
| 深色 `textDim` | `#8A93A8` | `#A4AEC3` |
| 深色 `textFaint` | `#5A6377` | `#7F8AA3` |
| 浅色 `textDim` | `#5B6577` | `#667085` |
| 浅色 `textFaint` | `#8C95A6` | `#76818F` |
| 深色 `cardBorder` | `0x14FFFFFF` | `0x0FFFFFFF` |
| 浅色 `cardBorder` | `0x14000000` | `0x0F000000` |

字号微调：`titleMedium` 15→16sp，`bodySmall` 12→12.5sp，`labelSmall` 11→11.5sp，
`displayLarge` letterSpacing `-1sp → -0.5sp`。

### 2. 控件（`Components.kt`）

- `GlassCard` 默认圆角 `22dp → 18dp`；
- `IconChip` 图标盒 `40dp → 44dp`、图标 `20 → 21dp`、文案 `10.5 → 11sp`，点击区更大；
- `PowerOrb` 动画只在 `on || busy` 时运行：断开且空闲时不再持续旋转/脉冲，省电且不再有闲置动画。

### 3. 首页（`HomeScreen.kt`）

- 标题 `TUNCLASH` letterSpacing `3sp → 1.2sp`；
- 设置入口 `38dp → 44dp`，加 `contentDescription`；
- 模式切换（规则/全局/直连）选中态改为**实色主色 + 白字**，选中一目了然；
- 速度数字 `15 → 17sp`，标签 `10 → 11sp`；
- 每秒刷新的运行时长收进独立的 `HomeStatus()` 组合项：**只有这段文本每秒重组**，
  首页其余部分（Canvas、卡片、节点卡）不再跟着每秒重绘。

### 4. 日志页（`LogsScreen.kt`）

- 日志行结构化：`时间`（弱化）+ `INFO/WARN/ERROR` 徽标（按级别着色）+ `消息`（等宽 11sp/行高 16sp），
  不再整行红/黄；
- 解析 mihomo 的 `time="..." level=... msg="..."` 格式，解析失败时回退原文；
- 上滑离开底部后右下角出现「回到底部」浮动按钮，点击平滑滚到最新一行；
- 头部图标 `38 → 44dp`，补齐 `contentDescription`（返回 / 复制全部日志 / 清空日志）；
- 空状态改成图标 + 主副文案，区分“暂无日志”和“没有匹配”。

## 文件

| 文件 | 说明 |
|---|---|
| `Android/app/src/main/java/com/mihomo/tproxy/ui/Theme.kt` | 对比度、排版 token |
| `Android/app/src/main/java/com/mihomo/tproxy/ui/Components.kt` | GlassCard / IconChip / PowerOrb |
| `Android/app/src/main/java/com/mihomo/tproxy/ui/HomeScreen.kt` | 标题、模式选中态、HomeStatus 重组优化 |
| `Android/app/src/main/java/com/mihomo/tproxy/ui/LogsScreen.kt` | 结构化日志、回到底部、空状态 |
| `CHANGELOG.md` | v1.3.2 优化记录 |

## 验证

- 本机无 Java/JAVA_HOME，无法跑 Gradle 编译；已做括号/花括号配平检查
  （Theme 8/8、Components 26/26、HomeScreen 61/61、LogsScreen 52/52），
  发布前请在 Android Studio / DeepSeek 客户端构建确认；
- 未修改任何状态流、网络请求、配置生成逻辑，代理行为与 v1.3.1 一致。

## 提交信息建议

```text
ui: readability, home layout, node selection, settings polish

P0:
- raise secondary text contrast in both themes, soften card borders
- smaller card corners (22->18dp) and larger icon/touch targets (44dp)
- mode chips use a solid primary state instead of tinted outline
- logs: structured rows (time + level badge + message), scroll-to-bottom
  fab, clearer empty states; copy/clear buttons get descriptions
- home: per-second uptime moves into its own composable so only the status
  text recomposes; PowerOrb animates only while connected or busy

P1:
- home: node card moves above the stats card (who first, then how fast);
  five equal chips replaced with filled "测速" + outlined "检测" + "更多"
  (expands to reload/logs/stop); PowerOrb shrinks to 120dp
- nodes: selected row uses a 3dp primary accent bar instead of full border
- settings: rows are 56dp with 14sp titles, 11.5sp subtitles, larger
  chevron, and content descriptions
```
