package com.artemkhateev.carlog.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Шрифт системный (Roboto), как у референса: своя гарнитура тут ничего не добавляет. */
@Immutable
data class CarLogTypography(
    val appBarTitle: TextStyle,
    val chipTitle: TextStyle,
    val chipSubtitle: TextStyle,
    val tab: TextStyle,
    val cardTitle: TextStyle,
    val statLabel: TextStyle,
    val statHero: TextStyle,
    val statValue: TextStyle,
    val listTitle: TextStyle,
    val listSubtitle: TextStyle,
    val sectionLabel: TextStyle,
    val body: TextStyle,
    val button: TextStyle,
    val caption: TextStyle,
    val navLabel: TextStyle,
    val emptyTitle: TextStyle,
)

private fun style(size: Float, weight: FontWeight = FontWeight.Normal, letterSpacing: Float = 0f) =
    TextStyle(fontSize = size.sp, fontWeight = weight, letterSpacing = letterSpacing.sp)

val DefaultCarLogTypography = CarLogTypography(
    appBarTitle = style(22f),
    chipTitle = style(16f, FontWeight.Medium),
    chipSubtitle = style(13f),
    tab = style(15f, FontWeight.Medium, 0.3f),
    cardTitle = style(16f, FontWeight.Medium),
    statLabel = style(13f),
    statHero = style(26f, FontWeight.Bold),
    statValue = style(15f, FontWeight.Medium),
    listTitle = style(17f, FontWeight.Medium),
    listSubtitle = style(14f),
    sectionLabel = style(12f, FontWeight.Medium, 1.2f),
    body = style(16f),
    button = style(15f, FontWeight.Medium, 1f),
    caption = style(12f),
    navLabel = style(13f, FontWeight.Medium),
    emptyTitle = style(20f),
)
