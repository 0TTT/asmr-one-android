package com.asmrone.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 品牌色：深色为主，淡紫蓝强调 + 珊瑚粉进度，契合 ASMR 氛围
private val PrimaryDark = Color(0xFFA9B7E6)      // 播放按钮 / 主强调（淡紫蓝）
private val SecondaryDark = Color(0xFFF2A48E)    // 进度条 / 次强调（珊瑚粉）

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF1B2340),
    secondary = SecondaryDark,
    onSecondary = Color(0xFF2A1A13),
    tertiary = Color(0xFF4DB6AC),
    background = Color(0xFF121010),
    onBackground = Color(0xFFEDE7E0),
    surface = Color(0xFF1D1A17),
    onSurface = Color(0xFFEDE7E0),
    surfaceVariant = Color(0xFF2A2420),
    onSurfaceVariant = Color(0xFF9A8F86),
    surfaceContainer = Color(0xFF1D1A17),
    surfaceContainerHigh = Color(0xFF3A2D24),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5C6BC0),
    secondary = Color(0xFFE91E63),
    tertiary = Color(0xFF00897B),
)

/**
 * darkTheme 为 null 时跟随系统；默认深色（与整体视觉设计一致）。
 */
@Composable
fun AsmrOneTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
