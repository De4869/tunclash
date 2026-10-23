package com.mihomo.tproxy.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val bg0 = Color(0xFF05070D)
    val bg1 = Color(0xFF0A0E18)
    val bg2 = Color(0xFF0E1424)
    val primary = Color(0xFF5B8CFF)
    val primaryDim = Color(0xFF3355AA)
    val accent = Color(0xFF22D3EE)
    val ok = Color(0xFF34D399)
    val warn = Color(0xFFFBBF24)
    val danger = Color(0xFFFB7185)
    val text = Color(0xFFE9EDF7)
    val textDim = Color(0xFF8A93A8)
    val textFaint = Color(0xFF5A6377)
    val card = Color(0x0DFFFFFF)
    val cardBorder = Color(0x14FFFFFF)
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

private val scheme = darkColorScheme(
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

@Composable
fun MihomoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
