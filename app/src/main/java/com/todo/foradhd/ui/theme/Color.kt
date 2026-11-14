package com.todo.foradhd.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val ObsidianBg = Color(0xFF000000)
val ObsidianSurface = Color(0xFF08090B)
val ObsidianElevated = Color(0xFF14161B)
val ObsidianInputBg = Color(0x990A0B0F)

val SilverPrimary = Color(0xFFF0F3F7)
val SilverSecondary = Color(0xFFC2C8D2)
val SilverDark = Color(0xFF8C929E)
val TextPrimary = Color(0xFFF7F8FA)
val TextSecondary = Color(0xFFC0C6D0)
val TextMuted = Color(0xFF8A909C)
val TextOnSilver = Color(0xFF090A0D)

val MetallicHighlight = Color(0xFFFAFBFC)
val MetallicMid = Color(0xFFD1D6DF)
val MetallicShadow = Color(0xFF9198A4)

@Immutable
data class VinaiColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val inputBg: Color,
    val glassBase: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val glassSheen: Color,
    val glassBorderHighlight: Color,
    val glassBorderShadow: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val primary: Color,
    val secondary: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentMuted: Color,
    val metallicStart: Color,
    val metallicMid: Color,
    val metallicEnd: Color,
    val ringTrack: Color,
    val error: Color,
    val navBarBg: Color,
    val pillBg: Color,
    val subtleOverlay: Color,
    val modalScrim: Color
)

/**
 * Pure Obsidian Black + Brushed Metallic Silver + Semi-Transparent Liquid Glass theme.
 */
val DarkVinaiColors = VinaiColors(
    isDark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF08090C),
    elevated = Color(0xFF14161B),
    inputBg = Color(0x8C0A0C10),
    glassBase = Color(0x880B0D12),
    cardTop = Color(0x2EF2F5FA),
    cardBottom = Color(0x0C161920),
    glassSheen = Color(0x28FFFFFF),
    glassBorderHighlight = Color(0x78FFFFFF),
    glassBorderShadow = Color(0x289098A6),
    border = Color(0x3CD8DEE9),
    borderStrong = Color(0x82F2F5FA),
    divider = Color(0x20E5E9F0),
    primary = Color(0xFFF0F3F7),
    secondary = Color(0xFFC2C8D2),
    onPrimary = Color(0xFF090A0D),
    textPrimary = Color(0xFFF7F8FA),
    textSecondary = Color(0xFFC0C6D0),
    textMuted = Color(0xFF8A909C),
    accent = Color(0xFFE6EBF2),
    accentMuted = Color(0x3CD6DCE6),
    metallicStart = Color(0xFFFAFBFC),
    metallicMid = Color(0xFFD1D6DF),
    metallicEnd = Color(0xFF9198A4),
    ringTrack = Color(0xFF1D2026),
    error = Color(0xFFE4B4B4),
    navBarBg = Color(0x9E0A0C10),
    pillBg = Color(0xA613161C),
    subtleOverlay = Color(0x16E8ECF4),
    modalScrim = Color(0x94000000)
)

/**
 * High-contrast Smoked Chrome Steel variant (also Black & Metallic Silver Liquid Glass).
 */
val SmokedSilverGlassColors = VinaiColors(
    isDark = true,
    background = Color(0xFF0B0D11),
    surface = Color(0xFF12151B),
    elevated = Color(0xFF1C2029),
    inputBg = Color(0x9E101318),
    glassBase = Color(0x9912151D),
    cardTop = Color(0x3AF2F5FA),
    cardBottom = Color(0x14222732),
    glassSheen = Color(0x30FFFFFF),
    glassBorderHighlight = Color(0x90FFFFFF),
    glassBorderShadow = Color(0x349AA3B2),
    border = Color(0x4AE0E5EE),
    borderStrong = Color(0x94F7F9FC),
    divider = Color(0x28E5E9F0),
    primary = Color(0xFFF5F7FA),
    secondary = Color(0xFFCBD1DB),
    onPrimary = Color(0xFF090A0D),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFCCD2DC),
    textMuted = Color(0xFF969DA9),
    accent = Color(0xFFEEF2F7),
    accentMuted = Color(0x46DCE2EC),
    metallicStart = Color(0xFFFFFFFF),
    metallicMid = Color(0xFFD7DCE4),
    metallicEnd = Color(0xFF9AA2AF),
    ringTrack = Color(0xFF282D36),
    error = Color(0xFFECC2C2),
    navBarBg = Color(0xB314181F),
    pillBg = Color(0xB81B2029),
    subtleOverlay = Color(0x1CEFF3F9),
    modalScrim = Color(0x9E050608)
)
