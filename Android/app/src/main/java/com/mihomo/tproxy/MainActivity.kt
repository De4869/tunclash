package com.mihomo.tproxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mihomo.tproxy.ui.AddProfileDialog
import com.mihomo.tproxy.ui.AppBackground
import com.mihomo.tproxy.ui.AppsScreen
import com.mihomo.tproxy.ui.GlassCard
import com.mihomo.tproxy.ui.HomeScreen
import com.mihomo.tproxy.ui.LogsScreen
import com.mihomo.tproxy.ui.MihomoTheme
import com.mihomo.tproxy.ui.NodesScreen
import com.mihomo.tproxy.ui.Palette
import com.mihomo.tproxy.ui.SettingsScreen
import com.mihomo.tproxy.ui.SubsScreen
import com.mihomo.tproxy.ui.TestScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            MihomoTheme {
                val vm: AppViewModel = viewModel()
                AppRoot(vm, intent?.getStringExtra("screen") ?: "")
            }
        }
    }
}

@Composable
fun AppRoot(vm: AppViewModel, initialScreen: String = "") {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showLogs by remember { mutableStateOf(false) }
    var showApps by remember { mutableStateOf(false) }
    var showTest by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var firstRunHandled by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    // theme: follow system by default, switchable in settings
    val systemDark = isSystemInDarkTheme()
    val dark = when (state.settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    if (Palette.dark != dark) Palette.apply(dark)
    val activity = LocalContext.current as? ComponentActivity
    LaunchedEffect(dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
    }

    LaunchedEffect(initialScreen) {
        when (initialScreen) {
            "test" -> showTest = true
            "nodes" -> tab = 1
            "subs" -> tab = 2
            "settings" -> tab = 3
            "logs" -> showLogs = true
            "apps" -> showApps = true
        }
    }

    LaunchedEffect(state.ready, state.profiles.size) {
        if (state.ready && state.rootOk && !firstRunHandled && state.profiles.isEmpty()) {
            firstRunHandled = true
            showAdd = true
        }
    }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            vm.consumeToast()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                when {
                    showApps -> AppsScreen(state, vm) { showApps = false }
                    showTest -> TestScreen(state, vm) { showTest = false }
                    showLogs -> LogsScreen(state, vm) { showLogs = false }
                    else -> Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            when (tab) {
                                0 -> HomeScreen(
                                    state, vm,
                                    onOpenNodes = { tab = 1 },
                                    onOpenSettings = { tab = 3 },
                                    onOpenLogs = { showLogs = true },
                                    onOpenTest = { showTest = true }
                                )
                                1 -> NodesScreen(state, vm)
                                2 -> SubsScreen(state, vm) { showAdd = true }
                                else -> SettingsScreen(
                                    state, vm,
                                    onOpenApps = { showApps = true },
                                    onOpenLogs = { showLogs = true }
                                )
                            }
                        }
                        BottomBar(tab) { tab = it }
                    }
                }
            }
        }
        if (state.installing != null) InstallingOverlay(state.installing!!)
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp)
        ) { data ->
            Box(
                Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.sheet)
                    .border(1.dp, Palette.cardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) { Text(data.visuals.message, color = Palette.text, fontSize = 12.5.sp) }
        }
        if (showAdd) AddProfileDialog(vm) { showAdd = false }
    }
}

@Composable
fun BottomBar(tab: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.navBg)
            .border(1.dp, Palette.cardBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabItem(0, tab, "首页", Icons.Rounded.Home) { onSelect(it) }
        TabItem(1, tab, "节点", Icons.Rounded.Dns) { onSelect(it) }
        TabItem(2, tab, "订阅", Icons.Rounded.CloudDownload) { onSelect(it) }
        TabItem(3, tab, "设置", Icons.Rounded.Settings) { onSelect(it) }
    }
}

@Composable
private fun RowScope.TabItem(
    index: Int,
    current: Int,
    label: String,
    icon: ImageVector,
    onSelect: (Int) -> Unit
) {
    val selected = index == current
    Row(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(Palette.primary.copy(alpha = 0.9f), Color(0xFF3F6FE8)))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable { onSelect(index) }
            .padding(horizontal = if (selected) 16.dp else 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = if (selected) Color.White else Palette.textFaint,
            modifier = Modifier.size(20.dp)
        )
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun InstallingOverlay(text: String) {
    Box(
        Modifier.fillMaxSize().background(Color(0xFF05070D).copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(Modifier.width(250.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = Palette.primary, strokeWidth = 2.5.dp, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(14.dp))
                Text("正在初始化内核", color = Palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(text, color = Palette.textDim, fontSize = 11.sp)
            }
        }
    }
}