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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
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
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(26.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.30f),
                    spotColor = Color.Black.copy(alpha = 0.40f)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Palette.glassTop, Palette.glassBottom)))
                .border(1.dp, Palette.glassBorder, RoundedCornerShape(26.dp))
        ) {
            val slot = maxWidth / 4
            val x by animateDpAsState(
                targetValue = slot * tab,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "tabPill"
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = x)
                    .width(slot)
                    .height(46.dp)
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.pillBg)
                    .border(1.dp, Palette.primary.copy(alpha = 0.26f), RoundedCornerShape(16.dp))
            )
            Row(Modifier.fillMaxSize()) {
                TabItem(0, tab, "首页", Icons.Rounded.Home, Modifier.weight(1f)) { onSelect(0) }
                TabItem(1, tab, "节点", Icons.Rounded.Dns, Modifier.weight(1f)) { onSelect(1) }
                TabItem(2, tab, "订阅", Icons.Rounded.CloudDownload, Modifier.weight(1f)) { onSelect(2) }
                TabItem(3, tab, "设置", Icons.Rounded.Settings, Modifier.weight(1f)) { onSelect(3) }
            }
        }
    }
}

@Composable
private fun TabItem(
    index: Int,
    current: Int,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    val selected = index == current
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "tabScale"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.55f,
        animationSpec = tween(180),
        label = "tabAlpha"
    )
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxHeight()
            .clickable(interactionSource = interaction, indication = null) { onSelect() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon, null,
            tint = if (selected) Palette.primary else Palette.textFaint,
            modifier = Modifier
                .size(21.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (selected) Palette.primary else Palette.textFaint,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha }
        )
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