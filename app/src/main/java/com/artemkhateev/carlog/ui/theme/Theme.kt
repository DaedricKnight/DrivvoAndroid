package com.artemkhateev.carlog.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.artemkhateev.carlog.ui.format.Formats

private val LocalCarLogColors = staticCompositionLocalOf { DarkCarLogColors }
private val LocalCarLogTypography = staticCompositionLocalOf { DefaultCarLogTypography }
private val LocalFormats = staticCompositionLocalOf<Formats> { error("Formats задаются в CarLogTheme") }

object CarLogTheme {
    val colors: CarLogColors
        @Composable @ReadOnlyComposable get() = LocalCarLogColors.current

    val typography: CarLogTypography
        @Composable @ReadOnlyComposable get() = LocalCarLogTypography.current

    /** Валюта и единицы из настроек. */
    val formats: Formats
        @Composable @ReadOnlyComposable get() = LocalFormats.current
}

@Composable
fun CarLogTheme(dark: Boolean, formats: Formats, content: @Composable () -> Unit) {
    val colors = if (dark) DarkCarLogColors else LightCarLogColors
    CompositionLocalProvider(
        LocalCarLogColors provides colors,
        LocalCarLogTypography provides DefaultCarLogTypography,
        LocalFormats provides formats,
    ) {
        // Material 3 — ради полей ввода, диалогов и шторок; цвета их поверхностей подогнаны под свои.
        MaterialTheme(colorScheme = materialColors(colors), content = content)
    }
}

/** Поля, переключатели и кнопки внутри красятся в цвет типа записи — как формы у референса. */
@Composable
fun AccentScope(accent: Color, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = accent), content = content)
}

private fun materialColors(colors: CarLogColors): ColorScheme {
    val base = if (colors.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = colors.brand,
        onPrimary = Color.White,
        secondaryContainer = colors.navIndicator,
        onSecondaryContainer = colors.textPrimary,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.card,
        onSurface = colors.textPrimary,
        onSurfaceVariant = colors.textSecondary,
        surfaceContainerLowest = colors.card,
        surfaceContainerLow = colors.card,
        surfaceContainer = colors.card,
        surfaceContainerHigh = colors.card,
        surfaceContainerHighest = colors.card,
        outline = colors.outline,
        outlineVariant = colors.divider,
        error = colors.danger,
        scrim = colors.scrim,
    )
}
