package com.mihomo.tproxy.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Single mutable palette shared by every screen. [apply] switches all tokens at once;
 * MainActivity wraps the UI in key(Palette.dark) so the whole tree re-composes on change.
 */
object Palette {
    var dark by mutableStateOf(true)
        private set

    var bg0 = Color(0xFF05070D); private set
    var bg1 = Color(0xFF0A0E18); private set
    var bg2 = Color(0xFF0E1424); private set
    var primary = Color(0xFF5B8CFF); private set
    var primaryDim = Color(0xFF3355AA); private set
    var accent = Color(0xFF22D3EE); private set
    var ok = Color(0xFF34D399); private set
    var warn = Color(0xFFFBBF24); private set
    var danger = Color(0xFFFB7185); private set
    var text = Color(0xFFE9EDF7); private set
    var textDim = Color(0xFF8A93A8); private set
    var textFaint = Color(0xFF5A6377); private set
    var card = Color(0x0DFFFFFF); private set
    var cardBorder = Color(0x14FFFFFF); private set
    var cardTop = Color(0x1AFFFFFF); private set
    var cardBottom = Color(0x0AFFFFFF); private set
    var navBg = Color(0xD90B1020); private set
    var sheet = Color(0xFF1A2136); private set
    var orbOffTop = Color(0xFF232A3A); private set
    var orbOffBottom = Color(0xFF161B28); private set
    var track = Color(0xFF232A3A); private set

    init { dark() }

    fun apply(isDark: Boolean) {
        dark = isDark
        if (isDark) dark() else light()
    }

    private fun dark() {
        bg0 = Color(0xFF05070D); bg1 = Color(0xFF0A0E18); bg2 = Color(0xFF0E1424)
        primary = Color(0xFF5B8CFF); primaryDim = Color(0xFF3355AA)
        accent = Color(0xFF22D3EE); ok = Color(0xFF34D399); warn = Color(0xFFFBBF24); danger = Color(0xFFFB7185)
        text = Color(0xFFE9EDF7); textDim = Color(0xFF8A93A8); textFaint = Color(0xFF5A6377)
        card = Color(0x0DFFFFFF); cardBorder = Color(0x14FFFFFF)
        cardTop = Color(0x1AFFFFFF); cardBottom = Color(0x0AFFFFFF)
        navBg = Color(0xD90B1020); sheet = Color(0xFF1A2136)
        orbOffTop = Color(0xFF232A3A); orbOffBottom = Color(0xFF161B28); track = Color(0xFF232A3A)
    }

    private fun light() {
        bg0 = Color(0xFFF3F5FA); bg1 = Color(0xFFFFFFFF); bg2 = Color(0xFFE7ECF5)
        primary = Color(0xFF3D6BFF); primaryDim = Color(0xFF2747B8)
        accent = Color(0xFF0E9BB5); ok = Color(0xFF0F9D63); warn = Color(0xFFB45309); danger = Color(0xFFDC2626)
        text = Color(0xFF101828); textDim = Color(0xFF5B6577); textFaint = Color(0xFF8C95A6)
        card = Color(0xFFFFFFFF); cardBorder = Color(0x14000000)
        cardTop = Color(0xFFFFFFFF); cardBottom = Color(0xFFF8FAFE)
        navBg = Color(0xF2FFFFFF); sheet = Color(0xFFFFFFFF)
        orbOffTop = Color(0xFFE3E9F4); orbOffBottom = Color(0xFFD2DAE8); track = Color(0xFFD7DEEA)
    }
}

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.5.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp)
)

@Composable
fun MihomoTheme(dark: Boolean = Palette.dark, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        primary = Palette.primary,
        onPrimary = Color.White,
        secondary = Palette.accent,
        background = Palette.bg0,
        onBackground = Palette.text,
        surface = Palette.bg1,
        onSurface = Palette.text,
        surfaceVariant = Palette.bg2,
        onSurfaceVariant = Palette.textDim,
        error = Palette.danger
    ) else lightColorScheme(
        primary = Palette.primary,
        onPrimary = Color.White,
        secondary = Palette.accent,
        background = Palette.bg0,
        onBackground = Palette.text,
        surface = Palette.bg1,
        onSurface = Palette.text,
        surfaceVariant = Palette.bg2,
        onSurfaceVariant = Palette.textDim,
        error = Palette.danger
    )
    MaterialTheme(colorScheme = scheme, typography = AppTypography) {
        key(dark) { content() }
    }
}
