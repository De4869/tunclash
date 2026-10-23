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
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
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
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(state.startedAt) {
        while (true) { tick = System.currentTimeMillis(); delay(1000) }
    }

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
                    letterSpacing = 3.sp
                )
                Text("透明代理 · TPROXY", color = Palette.textFaint, fontSize = 10.5.sp, letterSpacing = 1.2.sp)
            }
            StatusPill(state)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Settings, null, tint = Palette.textDim, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PowerOrb(on = state.connected, busy = state.busy || state.installing != null, diameter = 168.dp) {
                vm.toggleProxy()
            }
        }

        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val title = when {
                !state.rootOk -> "等待 Root 授权"
                state.installing != null -> state.installing!!
                !state.coreRunning -> "未启动"
                state.paused -> "代理已暂停"
                else -> "正在接管全部流量"
            }
            Text(title, color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    !state.coreRunning -> "点击开关启动 mihomo 内核"
                    state.paused -> "内核运行中 · 流量直连"
                    else -> "运行时长 " + fmtUptime(state.startedAt) + " · " + state.version
                },
                color = Palette.textFaint,
                fontSize = 11.5.sp
            )
        }

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
                            .clickable { vm.retryRoot() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("重新授权", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("↑ 上传", color = Palette.textFaint, fontSize = 10.sp)
                    Text(
                        fmtSpeed(state.upSpeed),
                        color = Palette.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text("累计 " + fmtBytes(state.upTotal), color = Palette.textFaint, fontSize = 9.5.sp)
                }
                Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.08f)))
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("↓ 下载", color = Palette.textFaint, fontSize = 10.sp)
                    Text(
                        fmtSpeed(state.downSpeed),
                        color = Palette.ok,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text("累计 " + fmtBytes(state.downTotal), color = Palette.textFaint, fontSize = 9.5.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

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
        GlassCard(Modifier.fillMaxWidth().clickable { onOpenNodes() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("当前节点", color = Palette.textFaint, fontSize = 10.5.sp)
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
                    Text(prettyName(profileName) + " · " + state.nodes.size + " 个节点", color = Palette.textFaint, fontSize = 10.5.sp)
                }
                if (curNode != null) DelayPill(curNode.delay)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = Palette.textFaint, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip("规则", state.mode == "rule" && !state.paused, Modifier.weight(1f)) { vm.setMode("rule") }
            ModeChip("全局", state.mode == "global" && !state.paused, Modifier.weight(1f)) { vm.setMode("global") }
            ModeChip("直连", state.mode == "direct", Modifier.weight(1f)) { vm.setMode("direct") }
        }

        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconChip(Icons.Rounded.Speed, "测速", enabled = state.coreRunning) { vm.speedTestAll() }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconChip(Icons.Rounded.Public, "检测", enabled = state.coreRunning) { onOpenTest() }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconChip(Icons.Rounded.Bolt, "最优", enabled = state.coreRunning) { vm.autoSelect() }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconChip(Icons.Rounded.Refresh, "重载", enabled = state.rootOk) { vm.applySettings(state.settings) }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconChip(Icons.Rounded.Terminal, "日志") { onOpenLogs() }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconChip(Icons.Rounded.DeleteSweep, "停止", enabled = state.coreRunning, tint = Palette.danger) { vm.stopCore() }
            }
        }

        Spacer(Modifier.height(90.dp))
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
            .background(if (selected) Palette.primary.copy(alpha = 0.16f) else Palette.card)
            .border(
                1.dp,
                if (selected) Palette.primary.copy(alpha = 0.45f) else Palette.cardBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Palette.primary else Palette.textDim,
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
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}