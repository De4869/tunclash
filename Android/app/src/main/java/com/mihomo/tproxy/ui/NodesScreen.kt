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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.mihomo.tproxy.model.NodeInfo

@Composable
fun NodesScreen(state: UiState, vm: AppViewModel) {
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refreshProxies() }
    var query by remember { mutableStateOf("") }
    var groupIndex by remember { mutableIntStateOf(0) }
    var sortByDelay by remember { mutableStateOf(false) }
    var onlyAlive by remember { mutableStateOf(false) }
    val groups = state.groups
    if (groupIndex >= groups.size) groupIndex = 0
    val group = groups.getOrNull(groupIndex)
    val nodeMap = remember(state.nodes) { state.nodes.associateBy { it.name } }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("节点", color = Palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    state.nodes.size.toString() + " 个节点 · " + groups.size + " 个策略组",
                    color = Palette.textFaint, fontSize = 11.5.sp
                )
            }
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Palette.primary)
                    .clickable(enabled = state.coreRunning && !state.busy) { vm.speedTestAll() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.busy) {
                    CircularProgressIndicator(Modifier.size(13.dp), color = Color.White, strokeWidth = 1.5.dp)
                } else {
                    Icon(Icons.Rounded.Speed, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("全部测速", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (groups.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups) { g ->
                    val idx = groups.indexOf(g)
                    val selected = idx == groupIndex
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Palette.primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
                            .border(1.dp, if (selected) Palette.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                            .clickable { groupIndex = idx }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            g.name,
                            color = if (selected) Palette.text else Palette.textDim,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索节点 / 地区", color = Palette.textFaint, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Palette.textFaint, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.primary.copy(alpha = 0.55f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                focusedContainerColor = Color.White.copy(alpha = 0.045f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                cursorColor = Palette.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallChip("按延迟排序", sortByDelay) { sortByDelay = !sortByDelay }
            SmallChip("只看可用", onlyAlive) { onlyAlive = !onlyAlive }
        }

        Spacer(Modifier.height(8.dp))

        val names = group?.nodes ?: emptyList()
        val base = names.asSequence()
            .filter { !ConfigComposer.isJunkNode(it) }
            .filter { query.isBlank() || it.contains(query, true) || ConfigComposer.flagOf(it).contains(query) }
            .filter { !onlyAlive || (nodeMap[it]?.delay ?: 0) > 0 }
            .toList()
        val filtered = if (sortByDelay) {
            base.sortedBy { val d = nodeMap[it]?.delay ?: 0; if (d <= 0) Int.MAX_VALUE else d }
        } else {
            base.sortedBy { if (nodeMap.containsKey(it)) 0 else 1 }
        }
        if (group == null) {
            EmptyHint("还没有节点", "去「订阅」添加订阅地址或粘贴节点链接")
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("策略组：" + group.name + " · " + group.type, color = Palette.textFaint, fontSize = 10.5.sp, modifier = Modifier.weight(1f))
                        Text("当前：" + (group.now.ifBlank { "—" }), color = Palette.accent, fontSize = 10.5.sp, maxLines = 1)
                    }
                }
                items(filtered) { name ->
                    val info = nodeMap[name]
                    val selected = group.now == name
                    NodeRow(
                        name = name,
                        info = info,
                        selected = selected,
                        onClick = { vm.select(group.name, name) }
                    )
                }
                item {
                    Spacer(Modifier.height(110.dp))
                }
            }
        }
    }
}

@Composable
fun NodeRow(name: String, info: NodeInfo?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Palette.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.028f))
            .border(1.dp, if (selected) Palette.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            val flag = ConfigComposer.flagOf(name)
            val glyph = if (flag.isNotBlank()) flag else ConfigComposer.leadingEmoji(name)
            Text(glyph, fontSize = 15.sp, color = Palette.textDim)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                color = if (selected) Palette.text else Palette.text.copy(alpha = 0.92f),
                fontSize = 13.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                (info?.type ?: "GROUP") + (if (info != null && info.udp) " · UDP" else ""),
                color = Palette.textFaint,
                fontSize = 10.sp
            )
        }
        if (info != null) DelayPill(info.delay)
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.Check, null, tint = Palette.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SmallChip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (on) Palette.primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (on) Palette.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.07f),
                RoundedCornerShape(11.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (on) Palette.text else Palette.textDim, fontSize = 11.5.sp)
    }
}

@Composable
fun EmptyHint(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Palette.textDim, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Palette.textFaint, fontSize = 12.sp)
    }
}