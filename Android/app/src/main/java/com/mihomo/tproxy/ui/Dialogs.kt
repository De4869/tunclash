package com.mihomo.tproxy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mihomo.tproxy.AppViewModel

@Composable
fun AddProfileDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        containerColor = Palette.bg2,
        title = { Text("添加订阅 / 节点", color = Palette.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
        text = {
            Column {
                Text(
                    "支持：Clash 订阅链接、vmess/vless/trojan/ss/hysteria2 分享链接（可多行粘贴）",
                    color = Palette.textFaint, fontSize = 11.5.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("https://... 或粘贴节点链接", color = Palette.textFaint, fontSize = 12.sp) },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Palette.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                        cursorColor = Palette.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (loading) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(14.dp), color = Palette.primary, strokeWidth = 1.5.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("正在解析…", color = Palette.textDim, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (loading) {
                Text("请稍候", color = Palette.textFaint, modifier = Modifier.padding(12.dp))
            } else {
                Text(
                    "添加",
                    color = if (text.isBlank()) Palette.textFaint else Palette.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = text.isNotBlank()) {
                        loading = true
                        vm.addProfile(text) { ok, _ ->
                            loading = false
                            if (ok) onDismiss()
                        }
                    }.padding(12.dp)
                )
            }
        },
        dismissButton = {
            Text("取消", color = Palette.textDim, modifier = Modifier.clickable(enabled = !loading) { onDismiss() }.padding(12.dp))
        }
    )
}

@Composable
fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.bg2,
        title = { Text(title, color = Palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    val isSel = value == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Palette.primary.copy(alpha = 0.14f) else Color.Transparent)
                            .clickableNoRipple { onSelect(value); onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = if (isSel) Palette.text else Palette.textDim, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                        if (isSel) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Palette.primary))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Text("关闭", color = Palette.textDim, modifier = Modifier.clickableNoRipple { onDismiss() }.padding(12.dp))
        }
    )
}
@Composable
fun AdSourcesDialog(settings: com.mihomo.tproxy.model.ProxySettings, vm: com.mihomo.tproxy.AppViewModel, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var behavior by remember { mutableStateOf("classical") }
    var format by remember { mutableStateOf("text") }
    val presets = listOf(
        Triple("ACL4SSR · BanAD", "https://gh-proxy.com/https://raw.githubusercontent.com/ACL4SSR/ACL4SSR/master/Clash/BanAD.list", "classical"),
        Triple("ACL4SSR · 程序广告", "https://gh-proxy.com/https://raw.githubusercontent.com/ACL4SSR/ACL4SSR/master/Clash/BanProgramAD.list", "classical"),
        Triple("Clash 域名规则(YAML)", "", "domain")
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.bg2,
        title = { Text("在线广告规则", color = Palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text("mihomo 会定期拉取这些规则并 REJECT 命中域名", color = Palette.textFaint, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.take(2).forEach { (label, u, b) ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Palette.primary.copy(alpha = 0.14f))
                                .clickableNoRipple { url = u; behavior = b; format = "text" }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) { Text(label, color = Palette.primary, fontSize = 10.5.sp) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("规则地址（http/https）", color = Palette.textFaint, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Palette.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                        cursorColor = Palette.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("classical" to "规则文本", "domain" to "域名列表", "ipcidr" to "IP 段").forEach { (v, label) ->
                        val sel = behavior == v
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) Palette.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                                .clickableNoRipple { behavior = v; format = if (v == "domain" || v == "ipcidr") "yaml" else "text" }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) { Text(label, color = if (sel) Palette.text else Palette.textDim, fontSize = 10.5.sp) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "全部更新",
                        color = Palette.primary,
                        fontSize = 11.sp,
                        modifier = Modifier.clickableNoRipple { vm.updateAdRules() }.padding(6.dp)
                    )
                }
                if (settings.adSources.isEmpty()) {
                    Text("还没有添加规则源", color = Palette.textFaint, fontSize = 11.sp)
                } else {
                    settings.adSources.forEachIndexed { i, s ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, color = Palette.text, fontSize = 12.sp)
                                Text(s.url, color = Palette.textFaint, fontSize = 9.5.sp, maxLines = 1)
                                Text(
                                    (if (s.count >= 0) s.count.toString() + " 条" else "规则数未知") +
                                        " · " + (if (s.updatedAt <= 0) "未更新" else
                                        java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                                            .format(java.util.Date(s.updatedAt))),
                                    color = Palette.textFaint,
                                    fontSize = 9.5.sp
                                )
                            }
                            Text(
                                "删除",
                                color = Palette.danger,
                                fontSize = 11.sp,
                                modifier = Modifier.clickableNoRipple { vm.removeAdSource(i) }.padding(6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Text(
                "添加",
                color = Palette.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(enabled = url.isNotBlank()) {
                    vm.addAdSource(url, behavior, format)
                    url = ""
                }.padding(10.dp)
            )
        },
        dismissButton = {
            Text("关闭", color = Palette.textDim, modifier = Modifier.clickableNoRipple { onDismiss() }.padding(10.dp))
        }
    )
}

@Composable
fun MergeFilterDialog(current: String, onDismiss: () -> Unit, onApply: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.bg2,
        title = { Text("聚合节点过滤", color = Palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text("整合时排除名字里包含这些关键词的节点，多个关键词用逗号分隔", color = Palette.textFaint, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("例如：剩余流量,官网,试用", color = Palette.textFaint, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Palette.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = Palette.cardBorder,
                        focusedContainerColor = Palette.card,
                        unfocusedContainerColor = Palette.card,
                        cursorColor = Palette.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Text(
                "保存",
                color = Palette.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickableNoRipple { onApply(text.trim()) }.padding(8.dp)
            )
        },
        dismissButton = {
            Text("取消", color = Palette.textDim, modifier = Modifier.clickableNoRipple { onDismiss() }.padding(8.dp))
        }
    )
}
