package com.mihomo.tproxy.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mihomo.tproxy.AppViewModel
import com.mihomo.tproxy.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(val pkg: String, val label: String, val system: Boolean)

private fun loadApps(ctx: Context): List<AppEntry> = runCatching {
    val pm = ctx.packageManager
    pm.getInstalledApplications(PackageManager.GET_META_DATA)
        .asSequence()
        .filter { it.packageName != ctx.packageName }
        .map { AppEntry(it.packageName, it.loadLabel(pm).toString(), (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0) }
        .distinctBy { it.pkg }
        .sortedBy { it.label }
        .toList()
}.getOrDefault(emptyList())

@Composable
fun AppsScreen(state: UiState, vm: AppViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var query by remember { mutableStateOf("") }
    val s = state.settings
    var selected by remember { mutableStateOf(s.perAppPackages) }

    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { loadApps(ctx) } }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).clickableNoRipple {
                    vm.applySettings(state.settings.copy(perAppPackages = selected))
                    onBack()
                },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.ArrowBack, null, tint = Palette.text, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("分应用代理", color = Palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    (if (s.perAppWhitelist) "白名单：仅选中的应用走代理" else "黑名单：选中的应用直连") + " · 已选 " + selected.size,
                    color = Palette.textFaint, fontSize = 11.sp
                )
            }
            Text(
                "完成",
                color = Palette.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickableNoRipple {
                    vm.applySettings(state.settings.copy(perAppPackages = selected))
                    onBack()
                }.padding(8.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索应用", color = Palette.textFaint, fontSize = 13.sp) },
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

        Spacer(Modifier.height(10.dp))

        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Palette.primary, strokeWidth = 2.dp)
            }
        } else {
            val filtered = list.filter { query.isBlank() || it.label.contains(query, true) || it.pkg.contains(query, true) }
            LazyColumn {
                items(filtered) { app ->
                    val checked = selected.contains(app.pkg)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (checked) Palette.primary.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.025f))
                            .border(1.dp, if (checked) Palette.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                            .clickableNoRipple {
                                selected = if (checked) selected - app.pkg else selected + app.pkg
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(app.label.take(1), color = Palette.textDim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = Palette.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.pkg, color = Palette.textFaint, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (checked) Palette.primary else Color.White.copy(alpha = 0.06f))
                                .border(1.dp, if (checked) Color.Transparent else Color.White.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}
