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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.mihomo.tproxy.model.Profile

@Composable
fun SubsScreen(state: UiState, vm: AppViewModel, onAddClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("订阅", color = Palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("支持订阅链接 · 分享链接 · Clash 配置", color = Palette.textFaint, fontSize = 11.5.sp)
            }
            RoundIcon(Icons.Rounded.Refresh) { vm.updateAll() }
            Spacer(Modifier.width(8.dp))
            RoundIcon(Icons.Rounded.Add, accent = true) { onAddClick() }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn {
            item {
                MergeCard(state, vm)
                Spacer(Modifier.height(10.dp))
            }
            if (state.profiles.isEmpty()) {
                item {
                    EmptyHint("还没有订阅", "点击右上角 + 添加订阅链接或粘贴节点")
                    Spacer(Modifier.height(40.dp))
                }
            }
            items(state.profiles) { p ->
                ProfileCard(p, active = p.id == state.activeId, state = state, vm = vm)
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(110.dp)) }
        }
    }
}

@Composable
fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (accent) Palette.primary else Color.White.copy(alpha = 0.05f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (accent) Color.White else Palette.textDim, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun MergeCard(state: UiState, vm: AppViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Palette.primary.copy(alpha = 0.22f), Palette.accent.copy(alpha = 0.12f))
                )
            )
            .border(1.dp, Palette.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(enabled = state.profiles.count { !it.isMerge } > 0) { vm.mergeAll() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = Palette.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("整合全部节点", color = Palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "自动去重 · 按地区分组 · 生成最优策略组",
                color = Palette.textDim, fontSize = 11.sp
            )
        }
        Text("去整合", color = Palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ProfileCard(p: Profile, active: Boolean, state: UiState, vm: AppViewModel) {
    var confirmDelete by remember { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth(), highlight = active) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            prettyName(p.name),
                            color = Palette.text,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (active) {
                            Spacer(Modifier.width(6.dp))
                            Pill("使用中", Palette.ok)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        kindLabel(p) + " · " + p.nodeCount + " 节点" + if (p.updatedAt > 0) " · " + fmtDate(p.updatedAt / 1000) else "",
                        color = Palette.textFaint, fontSize = 10.5.sp
                    )
                }
                if (!active) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Palette.primary.copy(alpha = 0.16f))
                            .clickable { vm.activate(p.id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("启用", color = Palette.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                if (p.kind == "url") {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).clickable { vm.updateProfile(p.id) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Refresh, null, tint = Palette.textDim, modifier = Modifier.size(15.dp)) }
                    Spacer(Modifier.width(6.dp))
                }
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).clickable { confirmDelete = true },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Delete, null, tint = Palette.danger.copy(alpha = 0.8f), modifier = Modifier.size(15.dp)) }
            }

            if (p.total > 0) {
                Spacer(Modifier.height(12.dp))
                val used = p.upload + p.download
                val ratio = (used.toFloat() / p.total.toFloat()).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(ratio)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    if (ratio > 0.9f) listOf(Palette.danger, Color(0xFFF97316))
                                    else listOf(Palette.primary, Palette.accent)
                                )
                            )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(fmtBytes(used) + " / " + fmtBytes(p.total), color = Palette.textDim, fontSize = 10.5.sp, modifier = Modifier.weight(1f))
                    if (p.expire > 0) Text("到期 " + fmtDate(p.expire), color = Palette.textFaint, fontSize = 10.5.sp)
                }
            }
        }
    }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                Text("删除", color = Palette.danger, modifier = Modifier.clickable { confirmDelete = false; vm.deleteProfile(p.id) }.padding(12.dp))
            },
            dismissButton = { Text("取消", color = Palette.textDim, modifier = Modifier.clickable { confirmDelete = false }.padding(12.dp)) },
            title = { Text("删除配置", color = Palette.text) },
            text = { Text("确定删除「" + p.name + "」吗？", color = Palette.textDim) },
            containerColor = Palette.bg2
        )
    }
}

private fun kindLabel(p: Profile): String = when (p.kind) {
    "url" -> "订阅链接"
    "yaml" -> "Clash 配置"
    "links" -> "分享链接"
    "merge" -> "聚合节点"
    else -> p.kind
}