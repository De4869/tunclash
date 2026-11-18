package com.mihomo.tproxy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mihomo.tproxy.AppViewModel
import com.mihomo.tproxy.UiState
import com.mihomo.tproxy.core.ConfigComposer
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    state: UiState,
    vm: AppViewModel,
    onOpenNodes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenTest: () -> Unit = {}
) {
    var showMore by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "TUNCLASH",
                    color = Palette.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )

            }
            StatusPill(state)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickableNoRipple { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Settings, "打开设置", tint = Palette.textDim, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PowerOrb(on = state.connected, busy = state.busy || state.installing != null, diameter = 120.dp) {
                vm.toggleProxy()
            }
        }

        Spacer(Modifier.height(14.dp))
        HomeStatus(state)

        if (!state.rootOk && state.ready) {
            Spacer(Modifier.height(14.dp))
            GlassCard(Modifier.fillMaxWidth(), highlight = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("需要 Root 权限", color = Palette.danger, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text("本应用通过 KernelSU / Magisk 运行 mihomo 内核，不占用 VPN 入口", color = Palette.textDim, fontSize = 11.sp)
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Palette.primary)
                            .clickableNoRipple { vm.retryRoot() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("重新授权", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Current node card first: it answers "who am I connected through?"
        val group = state.groups.firstOrNull { it.name == ConfigComposer.TARGET }
            ?: state.groups.firstOrNull { it.type == "Selector" && it.name != "GLOBAL" }
            ?: state.groups.firstOrNull()
        var current = group?.now ?: ""
        var guard = 0
        while (guard < 3) {
            val nested = state.groups.firstOrNull { it.name == current }
            if (nested == null || nested.now.isBlank() || nested.now == current) break
            current = nested.now
            guard++
        }
        val curNode = state.nodes.firstOrNull { it.name == current }
        val profileName = state.activeProfile?.name ?: "未选择配置"
        GlassCard(Modifier.fillMaxWidth().clickableNoRipple { onOpenNodes() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("当前节点", color = Palette.textFaint, fontSize = 11.5.sp)
                    Spacer(Modifier.height(4.dp))
                    val flag = if (current.isBlank()) "" else ConfigComposer.flagOf(current)
                    Text(
                        if (current.isBlank()) "未选择" else (if (flag.isBlank()) "" else flag + "  ") + current,
                        color = Palette.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(prettyName(profileName) + " · " + state.nodes.size + " 个节点", color = Palette.textFaint, fontSize = 11.5.sp)
                }
                if (curNode != null) DelayPill(curNode.delay)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.ChevronRight, "切换节点", tint = Palette.textFaint, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("↑ 上传", color = Palette.textFaint, fontSize = 11.sp)
                    Text(
                        fmtSpeed(state.upSpeed),
                        color = Palette.accent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text("累计 " + fmtBytes(state.upTotal), color = Palette.textFaint, fontSize = 10.5.sp)
                }
                Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.08f)))
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("↓ 下载", color = Palette.textFaint, fontSize = 11.sp)
                    Text(
                        fmtSpeed(state.downSpeed),
                        color = Palette.ok,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text("累计 " + fmtBytes(state.downTotal), color = Palette.textFaint, fontSize = 10.5.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip("规则", state.mode == "rule" && !state.paused, Modifier.weight(1f)) { vm.setMode("rule") }
            ModeChip("全局", state.mode == "global" && !state.paused, Modifier.weight(1f)) { vm.setMode("global") }
            ModeChip("直连", state.mode == "direct", Modifier.weight(1f)) { vm.setMode("direct") }
        }

        Spacer(Modifier.height(14.dp))

        // Primary actions: speed test (filled) + platform check + more (sheet)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Primary: speed test
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (state.coreRunning) Palette.primary else Palette.card)
                    .clickableNoRipple(enabled = state.coreRunning) { vm.speedTestAll() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.busy) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Speed, "测速", tint = if (state.coreRunning) Color.White else Palette.textFaint, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "测速",
                        color = if (state.coreRunning) Color.White else Palette.textFaint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // Secondary: platform check
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.card)
                    .border(1.dp, Palette.cardBorder, RoundedCornerShape(14.dp))
                    .clickableNoRipple(enabled = state.coreRunning) { onOpenTest() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Public, "平台检测", tint = if (state.coreRunning) Palette.text else Palette.textFaint, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("检测", color = if (state.coreRunning) Palette.text else Palette.textFaint, fontSize = 13.sp)
                }
            }
            // More
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.card)
                    .border(1.dp, Palette.cardBorder, RoundedCornerShape(14.dp))
                    .clickableNoRipple { showMore = !showMore }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (showMore) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        "更多操作",
                        tint = Palette.text,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("更多", color = Palette.text, fontSize = 13.sp)
                }
            }
        }

        // Expandable "more" section
        if (showMore) {
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column {
                    MoreRow(Icons.Rounded.Refresh, "重载配置", "重新写入配置并热加载内核", enabled = state.rootOk) {
                        vm.applySettings(state.settings)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.cardBorder))
                    MoreRow(Icons.Rounded.Terminal, "内核日志", "查看实时流量日志") {
                        onOpenLogs()
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.cardBorder))
                    MoreRow(Icons.Rounded.DeleteSweep, "停止内核", "停止 mihomo 进程", enabled = state.coreRunning, danger = true) {
                        vm.stopCore()
                    }
                }
            }
        }

        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun MoreRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickableNoRipple(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, title,
            tint = when {
                !enabled -> Palette.textFaint
                danger -> Palette.danger
                else -> Palette.text
            },
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = when {
                    !enabled -> Palette.textFaint
                    danger -> Palette.danger
                    else -> Palette.text
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Palette.textFaint, fontSize = 11.5.sp)
        }
    }
}

@Composable
fun StatusPill(state: UiState) {
    val (text, color) = when {
        !state.rootOk -> "无 ROOT" to Palette.danger
        state.installing != null -> "安装中" to Palette.warn
        !state.coreRunning -> "未运行" to Palette.textDim
        state.paused -> "已暂停" to Palette.warn
        else -> "运行中" to Palette.ok
    }
    Row(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ModeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Palette.primary else Palette.card)
            .border(
                1.dp,
                if (selected) Color.Transparent else Palette.cardBorder,
                RoundedCornerShape(14.dp)
            )
            .clickableNoRipple { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else Palette.textDim,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun DelayPill(delay: Int) {
    val color = when {
        delay <= 0 -> Palette.textFaint
        delay < 200 -> Palette.ok
        delay < 500 -> Palette.warn
        else -> Palette.danger
    }
    Box(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            if (delay <= 0) "—" else delay.toString() + "ms",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

/** Owns the per-second tick so only this block recomposes, not the whole home screen. */
@Composable
private fun HomeStatus(state: UiState) {
    var tick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.startedAt) {
        while (true) { tick = System.currentTimeMillis(); delay(1000) }
    }
    val title = when {
        !state.rootOk -> "等待 Root 授权"
        state.installing != null -> state.installing!!
        !state.coreRunning -> "未启动"
        state.paused -> "代理已暂停"
        else -> "正在接管全部流量"
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        val uptime = if (state.startedAt > 0) {
            val secs = ((tick - state.startedAt) / 1000).coerceAtLeast(0)
            String.format(java.util.Locale.US, "%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60)
        } else {
            "--:--:--"
        }
        Text(
            when {
                !state.coreRunning -> "点击开关启动 mihomo 内核"
                state.paused -> "内核运行中 · 流量直连"
                else -> "运行时长 $uptime · " + state.version
            },
            color = Palette.textFaint,
            fontSize = 12.sp
        )
    }
}