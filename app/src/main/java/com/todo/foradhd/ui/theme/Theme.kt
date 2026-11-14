package com.todo.foradhd.ui.theme

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

val LocalVinaiColors = staticCompositionLocalOf { DarkVinaiColors }

object VinaiTheme {
    val colors: VinaiColors
        @Composable get() = LocalVinaiColors.current
}

@Composable
fun VinaiTheme(
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (isDark) DarkVinaiColors else SmokedSilverGlassColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    val colorScheme = darkColorScheme(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        secondary = colors.secondary,
        onSecondary = colors.onPrimary,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.elevated,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.border,
        error = colors.error,
        onError = colors.background
    )

    CompositionLocalProvider(LocalVinaiColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = vinaiTypography(colors),
            content = content
        )
    }
}

/**
 * Enables native hardware-accelerated cross-window backdrop blur behind Dialogs on Android 12+ (API 31+),
 * paired with a semi-transparent obsidian scrim on all Android versions.
 */
@Composable
fun ApplyDialogBlurBehind(blurRadiusPx: Int = 44) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = blurRadiusPx
                }
            }
            window.setDimAmount(0.42f)
        }
    }
}

/**
 * Pure Black obsidian canvas with a dedicated blurred atmospheric silver-chrome light layer
 * behind the foreground UI so semi-transparent Liquid Glass cards refract soft specular highlights.
 */
@Composable
fun ObsidianAtmosphereBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = VinaiTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Dedicated blurred metallic-silver ambient light layer behind all UI surfaces
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 52.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x22E8EDF5), Color(0x0A9BA4B2), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.12f),
                            radius = size.width * 0.72f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1AC8D0DC), Color.Transparent),
                            center = Offset(size.width * 0.14f, size.height * 0.46f),
                            radius = size.width * 0.58f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1CDCE2EC), Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.76f),
                            radius = size.width * 0.68f
                        )
                    )
                }
        )
        content()
    }
}

/**
 * Liquid Glass container with semi-transparent obsidian base, dedicated blurred frosted silver
 * dispersion backdrop behind crisp foreground content, specular top sheen, and chrome rim border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val colors = VinaiTheme.colors
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val rimBrush = if (borderColor != null) {
        Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))
    } else {
        Brush.linearGradient(
            colors = listOf(
                colors.glassBorderHighlight,
                colors.border,
                colors.glassBorderShadow
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.glassBase)
            .border(1.dp, rimBrush, shape)
    ) {
        // Blurred internal frosted liquid-glass dispersion layer (never blurs foreground text)
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 18.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                .background(
                    Brush.linearGradient(
                        colors = listOf(colors.cardTop, Color.Transparent, colors.cardBottom)
                    )
                )
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(colors.glassSheen, Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                    )
                }
        )
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Reusable semi-transparent Liquid Glass pill/dock container with blurred silver dispersion
 * layer and metallic chrome border.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(999.dp),
    baseTint: Color? = null,
    blurRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = VinaiTheme.colors
    val rimBrush = Brush.linearGradient(
        colors = listOf(colors.glassBorderHighlight, colors.border, colors.glassBorderShadow)
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(baseTint ?: colors.pillBg)
            .border(1.dp, rimBrush, shape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(colors.cardTop, colors.cardBottom)
                    )
                )
        )
        content()
    }
}

/**
 * Brushed Metallic Silver primary CTA with chrome specular highlight and obsidian ink label.
 */
@Composable
fun SilverPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colors = VinaiTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val bgBrush = if (enabled) {
        Brush.linearGradient(
            colors = listOf(
                colors.metallicStart,
                colors.metallicMid,
                colors.metallicEnd,
                colors.metallicMid
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                colors.metallicStart.copy(alpha = 0.35f),
                colors.metallicEnd.copy(alpha = 0.35f)
            )
        )
    }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(brush = bgBrush)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xDDFFFFFF), colors.metallicEnd)
                ),
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun GlassOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val colors = VinaiTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(colors.glassBorderHighlight, colors.glassBorderShadow))
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.subtleOverlay,
            contentColor = colors.textPrimary
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
fun GlassDarkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val colors = VinaiTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.subtleOverlay,
            contentColor = colors.textPrimary
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        content = content
    )
}
