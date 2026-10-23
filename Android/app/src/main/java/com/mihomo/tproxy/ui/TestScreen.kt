package com.mihomo.tproxy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mihomo.tproxy.AppViewModel
import com.mihomo.tproxy.PlatformResult
import com.mihomo.tproxy.UiState

@Composable
fun TestScreen(state: UiState, vm: AppViewModel, onBack: () -> Unit) {
    val results = state.platforms
    val okCount = results.count { it.state == "ok" }
    val ready = results.count { it.state == "ok" || it.state == "fail" }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.ArrowBack, null, tint = Palette.text, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("平台检测", color = Palette.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("通过当前节点访问常用平台，验证分流是否正常", color = Palette.textFaint, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        GlassCard(Modifier.fillMaxWidth(), highlight = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            !state.coreRunning -> "代理未运行"
                            ready == 0 -> "尚未检测"
                            else -> okCount.toString() + " / " + ready + " 个平台可用"
                        },
                        color = if (okCount > 0) Palette.text else Palette.danger,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (state.coreRunning) "经 127.0.0.1:7890 逐个探测，超时 6 秒" else "请先回到首页开启代理",
                        color = Palette.textFaint, fontSize = 10.5.sp
                    )
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (state.coreRunning) Brush.horizontalGradient(listOf(Palette.primary, Color(0xFF3F6FE8))) else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))))
                        .clickable(enabled = state.coreRunning) { vm.testPlatforms() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = if (state.coreRunning) Color.White else Palette.textFaint, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("开始检测", color = if (state.coreRunning) Color.White else Palette.textFaint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (results.isEmpty()) {
            EmptyHint("还没有检测结果", "点上面的「开始检测」，一次测完 10 个常用平台")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { r -> PlatformTile(r) }
                item { Spacer(Modifier.height(90.dp)) }
            }
        }
    }
}

@Composable
private fun PlatformTile(r: PlatformResult) {
    val color = when (r.state) {
        "ok" -> Palette.ok
        "fail" -> Palette.danger
        "testing" -> Palette.warn
        else -> Palette.textFaint
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) { Text(r.emoji, fontSize = 16.sp) }
                Spacer(Modifier.width(9.dp))
                Text(
                    r.name,
                    color = Palette.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                when (r.state) {
                    "testing" -> CircularProgressIndicator(Modifier.size(13.dp), color = Palette.warn, strokeWidth = 1.5.dp)
                    "ok" -> Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.ok))
                    "fail" -> Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.danger))
                    else -> {}
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when (r.state) {
                    "ok" -> "可用 · " + r.ms + " ms"
                    "fail" -> "不可用 / 超时"
                    "testing" -> "检测中…"
                    else -> "未检测"
                },
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
