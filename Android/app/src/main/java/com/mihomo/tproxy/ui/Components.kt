package com.mihomo.tproxy.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.bg0, Palette.bg1, Palette.bg0)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Palette.primary.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.05f),
                    radius = size.width * 1.05f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Palette.accent.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.42f),
                    radius = size.width * 0.9f
                )
            )
        }
        content()
    }
}

/** Clickable without the rectangular ripple (rows/chips inside cards). */
fun Modifier.clickableNoRipple(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    composed {
        val source = remember { MutableInteractionSource() }
        this.clickable(
            enabled = enabled,
            interactionSource = source,
            indication = null,
            onClick = onClick
        )
    }

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Int = 22,
    highlight: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (highlight) Palette.cardTop else Palette.card,
                        Palette.cardBottom
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Palette.cardBorder,
                shape = RoundedCornerShape(corner.dp)
            )
            .padding(16.dp)
    ) { content() }
}

@Composable
fun Pill(
    text: String,
    color: Color = Palette.primary,
    filled: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(CircleShape)
            .background(if (filled) color else color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = if (filled) 0f else 0.35f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = if (filled) Palette.bg1 else color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
        color = Palette.textFaint,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp
    )
}

@Composable
fun PowerOrb(
    on: Boolean,
    busy: Boolean,
    diameter: androidx.compose.ui.unit.Dp = 208.dp,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition()
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart)
    )
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val ringColor by animateColorAsState(
        if (on) Palette.accent else Palette.textFaint,
        animationSpec = tween(600)
    )
    val coreTop by animateColorAsState(
        if (on) Palette.primary else Palette.orbOffTop,
        animationSpec = tween(600)
    )
    val coreBottom by animateColorAsState(
        if (on) Color(0xFF2A4BD8) else Palette.orbOffBottom,
        animationSpec = tween(600)
    )

    Box(
        Modifier
            .size(diameter)
            .clickable(enabled = !busy) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2

            // outer glow
            if (on) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Palette.primary.copy(alpha = 0.30f), Color.Transparent),
                        center = c,
                        radius = r
                    ),
                    radius = r,
                    center = c
                )
            }

            // faint base ring
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = r - 6.dp.toPx(),
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )

            if (on) {
                rotate(spin) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, Palette.accent, Palette.primary, Color.Transparent)
                        ),
                        startAngle = 0f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(c.x - (r - 6.dp.toPx()), c.y - (r - 6.dp.toPx())),
                        size = Size((r - 6.dp.toPx()) * 2, (r - 6.dp.toPx()) * 2),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                rotate(-spin * 0.7f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, Palette.primary.copy(alpha = 0.75f), Color.Transparent)
                        ),
                        startAngle = 180f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(c.x - (r - 14.dp.toPx()), c.y - (r - 14.dp.toPx())),
                        size = Size((r - 14.dp.toPx()) * 2, (r - 14.dp.toPx()) * 2),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // inner core
            val coreR = r * 0.56f
            drawCircle(
                brush = Brush.verticalGradient(listOf(coreTop, coreBottom), startY = c.y - coreR, endY = c.y + coreR),
                radius = coreR * (if (busy) pulse else 1f),
                center = c
            )
            drawCircle(
                color = ringColor.copy(alpha = if (on) 0.55f else 0.18f),
                radius = coreR,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.PowerSettingsNew,
                contentDescription = null,
                tint = if (on) Color.White else Palette.textDim,
                modifier = Modifier.size(if (diameter >= 200.dp) 46.dp else 38.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (busy) "处理中" else if (on) "已连接" else "未连接",
                color = if (on) Color.White.copy(alpha = 0.92f) else Palette.textDim,
                fontSize = if (diameter >= 200.dp) 13.sp else 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun IconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color = Palette.text,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White.copy(alpha = 0.055f))
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (enabled) tint else Palette.textFaint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = if (enabled) Palette.textDim else Palette.textFaint, fontSize = 10.5.sp)
    }
}

@Composable
fun StatCell(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Palette.textFaint, fontSize = 10.5.sp, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
fun SpacerH(w: Int) = Spacer(Modifier.width(w.dp))