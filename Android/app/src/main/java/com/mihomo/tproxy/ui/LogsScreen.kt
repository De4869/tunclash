package com.mihomo.tproxy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mihomo.tproxy.AppViewModel
import com.mihomo.tproxy.UiState

private enum class LogLevel(val label: String) { ALL("全部"), WARN("警告"), ERROR("错误") }

@Composable
fun LogsScreen(state: UiState, vm: AppViewModel, onBack: () -> Unit) {
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    var query by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(LogLevel.ALL) }

    val lines = remember(state.logs, query, level) {
        state.logs.filter { line ->
            val byLevel = when (level) {
                LogLevel.ALL -> true
                LogLevel.WARN -> line.contains("warn", true)
                LogLevel.ERROR -> line.contains("error", true) || line.contains("fatal", true)
            }
            byLevel && (query.isBlank() || line.contains(query, true))
        }
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex < 0 || listState.firstVisibleItemIndex >= lastIndex - 3) {
                runCatching { listState.scrollToItem(lines.size - 1) }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Palette.bg0)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).clickableNoRipple { onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.ArrowBack, "返回", tint = Palette.text, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("内核日志", color = Palette.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(lines.size.toString() + " / " + state.logs.size + " 行 · 实时", color = Palette.textFaint, fontSize = 10.5.sp)
            }
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f))
                    .clickableNoRipple { clipboard.setText(AnnotatedString(lines.joinToString("\n"))) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.ContentCopy, "复制", tint = Palette.textDim, modifier = Modifier.size(17.dp)) }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).clickableNoRipple { vm.clearLogs() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.DeleteSweep, "清空", tint = Palette.textDim, modifier = Modifier.size(18.dp)) }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索日志", color = Palette.textFaint, fontSize = 13.sp) },
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        )

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LogLevel.entries.forEach { lv ->
                val on = level == lv
                Box(
                    Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (on) Palette.primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
                        .border(
                            1.dp,
                            if (on) Palette.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.07f),
                            RoundedCornerShape(11.dp)
                        )
                        .clickableNoRipple { level = lv }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(lv.label, color = if (on) Palette.text else Palette.textDim, fontSize = 11.5.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), state = listState) {
            items(lines) { line ->
                Text(
                    line,
                    color = when {
                        line.contains("fatal", true) || line.contains("error", true) -> Palette.danger
                        line.contains("warn", true) -> Palette.warn
                        else -> Palette.textDim
                    },
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 1.5.dp)
                )
            }
            if (lines.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (state.logs.isEmpty()) "暂无日志，启动内核后这里会实时输出" else "没有匹配的日志",
                            color = Palette.textFaint, fontSize = 12.sp
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}
