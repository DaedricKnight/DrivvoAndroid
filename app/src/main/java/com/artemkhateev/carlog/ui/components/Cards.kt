package com.artemkhateev.carlog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemkhateev.carlog.ui.theme.CarLogTheme

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CarLogTheme.colors.card)
            .padding(16.dp),
        content = content,
    )
}

/** Заголовок карточки отчёта: цветной квадрат со значком и название цветом раздела. */
@Composable
fun CardHeader(icon: ImageVector, title: String, accent: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        TypeBadge(icon, accent, size = 40.dp, iconSize = 22.dp, shape = RoundedCornerShape(10.dp))
        Text(
            text = title,
            style = CarLogTheme.typography.cardTitle,
            color = accent,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

data class Stat(val label: String, val value: String)

/** Карточка отчёта: крупное значение и под чертой — одно-два второстепенных. */
@Composable
fun StatCard(
    icon: ImageVector,
    title: String,
    accent: Color,
    hero: Stat,
    footer: List<Stat>,
    modifier: Modifier = Modifier,
) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    AppCard(modifier) {
        CardHeader(icon, title, accent)
        Spacer(Modifier.height(16.dp))
        Text(hero.label, style = typography.statLabel, color = colors.textSecondary, maxLines = 1)
        // Суммы бывают длинными, а карточка — в пол-экрана: крупный текст ужимается, а не обрезается.
        BasicText(
            text = hero.value,
            style = typography.statHero.copy(color = accent),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = typography.statHero.fontSize),
        )
        if (footer.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = colors.divider)
            StatRow(footer)
        }
    }
}

/** Пары «подпись — значение» в строку: первая прижата влево, последняя вправо. */
@Composable
fun StatRow(stats: List<Stat>, modifier: Modifier = Modifier) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEachIndexed { index, stat ->
            val end = stats.size > 1 && index == stats.lastIndex
            Column(Modifier.weight(1f), horizontalAlignment = if (end) Alignment.End else Alignment.Start) {
                Text(stat.label, style = typography.statLabel, color = colors.textSecondary, maxLines = 1)
                BasicText(
                    text = stat.value,
                    style = typography.statValue.copy(
                        color = colors.textPrimary,
                        textAlign = if (end) TextAlign.End else TextAlign.Start,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = typography.statValue.fontSize),
                )
            }
        }
    }
}

/** Две карточки в ряд. */
@Composable
fun CardPair(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

/** Рамка выбора периода: число записей и даты, справа календарь цветом раздела. */
@Composable
fun PeriodBox(text: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CarLogTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = CarLogTheme.typography.statValue,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun EmptyState(icon: ImageVector, text: String, modifier: Modifier = Modifier, hint: String? = null) {
    val colors = CarLogTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = colors.textPrimary.copy(alpha = 0.72f), modifier = Modifier.size(128.dp))
        Text(
            text = text,
            style = CarLogTheme.typography.emptyTitle,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (hint != null) {
            Text(
                text = hint,
                style = CarLogTheme.typography.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = CarLogTheme.typography.sectionLabel,
        color = CarLogTheme.colors.textSecondary,
        modifier = modifier,
    )
}
