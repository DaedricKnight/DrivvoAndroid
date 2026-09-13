package com.artemkhateev.carlog.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class CarLogColors(
    val isDark: Boolean,
    /** Фон экранов. */
    val background: Color,
    /** Фон под карточками отчётов — чуть темнее, чтобы карточки отделялись. */
    val backgroundDim: Color,
    val card: Color,
    val outline: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val icon: Color,
    /** Цвет шапок основных экранов и главной кнопки. */
    val brand: Color,
    /** Бренд на фоне экрана: подписи выбранной вкладки. */
    val brandText: Color,
    val navIndicator: Color,
    val scrim: Color,
    val warning: Color,
    val danger: Color,
)

val DarkCarLogColors = CarLogColors(
    isDark = true,
    background = Color(0xFF1E282D),
    backgroundDim = Color(0xFF192125),
    card = Color(0xFF232E33),
    outline = Color(0xFF3D4A50),
    divider = Color(0xFF2F3B41),
    textPrimary = Color(0xFFECEFF1),
    textSecondary = Color(0xFFA3AEB4),
    icon = Color(0xFF93A0A6),
    brand = Color(0xFF00939F),
    brandText = Color(0xFF1FAAB7),
    navIndicator = Color(0xFF0C3A40),
    scrim = Color(0xA6000000),
    warning = Color(0xFFFFD54F),
    danger = Color(0xFFEF5350),
)

val LightCarLogColors = CarLogColors(
    isDark = false,
    background = Color(0xFFF2F5F6),
    backgroundDim = Color(0xFFE8EDEF),
    card = Color(0xFFFFFFFF),
    outline = Color(0xFFC5CFD3),
    divider = Color(0xFFDDE3E6),
    textPrimary = Color(0xFF1C2529),
    textSecondary = Color(0xFF5E6B71),
    icon = Color(0xFF6E7B81),
    brand = Color(0xFF00939F),
    brandText = Color(0xFF007A85),
    navIndicator = Color(0xFFCBEAED),
    scrim = Color(0x80000000),
    warning = Color(0xFFB07D00),
    danger = Color(0xFFD32F2F),
)

/** Цвета типов записей — одинаковые в обеих темах: по ним записи узнаются с одного взгляда. */
object EntryColors {
    val Refueling = Color(0xFFFF9800)
    val Expense = Color(0xFFE64A19)
    val Income = Color(0xFF388E3C)
    val Service = Color(0xFF8D6E63)
    val Route = Color(0xFF607D8B)
    val Reading = Color(0xFFC2185B)
    val Reminder = Color(0xFF673AB7)
}

/** Цвета долей в кольцевых диаграммах по видам расходов и сервиса. */
val ChartColors = listOf(
    Color(0xFFFF9800),
    Color(0xFF26A69A),
    Color(0xFFE64A19),
    Color(0xFF5C6BC0),
    Color(0xFF8D6E63),
    Color(0xFFC2185B),
    Color(0xFF7CB342),
    Color(0xFF29B6F6),
    Color(0xFFFFCA28),
    Color(0xFF78909C),
)

/** Цвета аватаров машин; у машины хранится индекс. */
val VehicleColors = listOf(
    Color(0xFF00939F),
    Color(0xFF455A64),
    Color(0xFFE64A19),
    Color(0xFF388E3C),
    Color(0xFF8D6E63),
    Color(0xFF1E88E5),
    Color(0xFFC2185B),
    Color(0xFF673AB7),
    Color(0xFFFF9800),
    Color(0xFF212121),
)
