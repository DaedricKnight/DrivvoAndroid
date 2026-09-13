package com.artemkhateev.carlog.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.Income
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.model.ReminderKind
import com.artemkhateev.carlog.data.model.Route
import com.artemkhateev.carlog.domain.DueState
import com.artemkhateev.carlog.domain.HistoryRow
import com.artemkhateev.carlog.ui.components.EmptyState
import com.artemkhateev.carlog.ui.components.SectionLabel
import com.artemkhateev.carlog.ui.components.TypeBadge
import com.artemkhateev.carlog.ui.components.accent
import com.artemkhateev.carlog.ui.components.dueText
import com.artemkhateev.carlog.ui.components.entrySubtitle
import com.artemkhateev.carlog.ui.components.entryTitle
import com.artemkhateev.carlog.ui.components.icon
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import com.artemkhateev.carlog.ui.theme.EntryColors
import java.time.LocalDate

private val TrackWidth = 72.dp

@Composable
fun HistoryScreen(navigator: AppNavigator) {
    val viewModel: HistoryViewModel = viewModel { HistoryViewModel(AppGraph.currentVehicle) }
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    if (state.rows.isEmpty() && state.reminders.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
            text = stringResource(R.string.history_empty),
            hint = stringResource(R.string.history_empty_hint),
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (state.reminders.isNotEmpty()) {
            item(key = "reminders", contentType = "reminders") {
                RemindersStrip(state.reminders) { card ->
                    when (card) {
                        is ReminderCardUi.NextRefueling -> navigator.openEntry(EntryType.Refueling)
                        is ReminderCardUi.Scheduled -> navigator.openReminder(card.reminder.id)
                    }
                }
            }
        }
        items(state.rows, key = { it.key }, contentType = { it.contentType }) { row ->
            when (row) {
                is HistoryRow.Month -> MonthSummary(row)
                is HistoryRow.Item -> TimelineEntry(row, state.catalogs) { navigator.openEntry(row.entry.type, row.entry.id) }
                is HistoryRow.Start -> StartOfRoad(row.date)
            }
        }
    }
}

private val HistoryRow.key: String
    get() = when (this) {
        is HistoryRow.Month -> "month-$month"
        // У разных таблиц id пересекаются — ключ включает тип.
        is HistoryRow.Item -> "entry-${entry.type}-${entry.id}"
        is HistoryRow.Start -> "start"
    }

private val HistoryRow.contentType: Int
    get() = when (this) {
        is HistoryRow.Month -> 0
        is HistoryRow.Item -> 1
        is HistoryRow.Start -> 2
    }

@Composable
private fun RemindersStrip(cards: List<ReminderCardUi>, onOpen: (ReminderCardUi) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel(
            text = stringResource(R.string.history_reminders),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cards) { card -> ReminderCard(card) { onOpen(card) } }
        }
        HorizontalDivider(Modifier.padding(top = 16.dp), color = CarLogTheme.colors.divider)
    }
}

@Composable
private fun ReminderCard(card: ReminderCardUi, onClick: () -> Unit) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    val formats = CarLogTheme.formats
    val visualType = when (card) {
        is ReminderCardUi.NextRefueling -> EntryType.Refueling
        is ReminderCardUi.Scheduled -> if (card.reminder.kind == ReminderKind.Service) EntryType.Service else EntryType.Expense
    }
    val title = when (card) {
        is ReminderCardUi.NextRefueling -> stringResource(R.string.history_next_refueling)
        is ReminderCardUi.Scheduled -> card.title.ifBlank { stringResource(R.string.entry_reminder) }
    }
    val (dueOdometer, dueDate) = when (card) {
        is ReminderCardUi.NextRefueling -> card.forecast.odometer to card.forecast.date
        is ReminderCardUi.Scheduled -> card.reminder.dueOdometer to card.reminder.dueDate
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .width(236.dp)
            .height(IntrinsicSize.Min)
            .clip(shape)
            .border(1.dp, colors.outline, shape)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(visualType.accent),
        )
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(visualType.icon, visualType.accent, size = 30.dp, iconSize = 18.dp)
                Text(
                    text = title,
                    style = typography.statValue,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            val dueLine = listOfNotNull(dueOdometer?.let { formats.distance(it) }, dueDate?.let { formats.dayMonth(it) })
                .joinToString(" · ")
            Text(dueLine, style = typography.listSubtitle, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                val tint = when (card.status.state) {
                    DueState.Overdue -> colors.danger
                    DueState.Soon -> colors.warning
                    DueState.Later -> colors.textSecondary
                }
                Icon(Icons.Filled.Alarm, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Text(
                    text = dueText(card.status),
                    style = typography.listSubtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

private enum class TrackMarker { None, Bar, End }

/** Пунктир ленты слева; у итога месяца на нём планка, у начала пути лента обрывается. */
@Composable
private fun TimelineTrack(marker: TrackMarker = TrackMarker.None, content: @Composable BoxScope.() -> Unit = {}) {
    val trackColor = CarLogTheme.colors.divider
    val dashColor = CarLogTheme.colors.textSecondary.copy(alpha = 0.55f)
    val markerColor = CarLogTheme.colors.outline
    Box(
        modifier = Modifier
            .width(TrackWidth)
            .fillMaxHeight()
            .drawBehind {
                val x = size.width / 2
                val end = if (marker == TrackMarker.End) size.height / 2 else size.height
                drawLine(trackColor, Offset(x, 0f), Offset(x, end), strokeWidth = 8.dp.toPx())
                drawLine(
                    color = dashColor,
                    start = Offset(x, 0f),
                    end = Offset(x, end),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                )
                if (marker != TrackMarker.None) {
                    val width = 26.dp.toPx()
                    val height = 8.dp.toPx()
                    val y = if (marker == TrackMarker.Bar) 36.dp.toPx() else end
                    drawRoundRect(
                        color = markerColor,
                        topLeft = Offset(x - width / 2, y - height / 2),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(height / 2),
                    )
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun MonthSummary(row: HistoryRow.Month) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    val formats = CarLogTheme.formats
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        TimelineTrack(TrackMarker.Bar)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp, bottom = 12.dp, end = 16.dp)
                .clip(shape)
                .background(colors.card)
                .border(1.dp, colors.outline, shape)
                .padding(16.dp),
        ) {
            SectionLabel(formats.monthTitle(row.month))
            val summary = listOfNotNull(
                formats.money(row.cost),
                formats.distance(row.distance),
                row.averageConsumption?.let { formats.consumption(it) },
            ).joinToString(" · ")
            Text(summary, style = typography.body, color = colors.textPrimary, modifier = Modifier.padding(top = 8.dp))
            if (row.tankRange != null) {
                val range = formats.distance(row.tankRange)
                Text(
                    text = withBold(stringResource(R.string.history_tank_range, range), range),
                    style = typography.listSubtitle,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun TimelineEntry(row: HistoryRow.Item, catalogs: Catalogs, onClick: () -> Unit) {
    val entry = row.entry
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    val formats = CarLogTheme.formats
    val amount = when (entry) {
        is Income -> "+" + formats.money(entry.amount)
        is Route -> entry.value.takeIf { it.minor != 0L }?.let { formats.money(it) }
        is Reading -> null
        else -> formats.money(entry.cost)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick),
    ) {
        TimelineTrack {
            TypeBadge(entry.type.icon, entry.type.accent, size = 48.dp)
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.padding(top = 16.dp, end = 16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = entryTitle(entry, catalogs),
                        style = typography.listTitle,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entrySubtitle(entry, row.consumption, catalogs),
                        style = typography.listSubtitle,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                    if (amount != null) {
                        Text(
                            text = amount,
                            style = typography.listTitle,
                            color = if (entry is Income) EntryColors.Income else colors.textPrimary,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = formats.dayMonth(entry.dateTime.toLocalDate()),
                        style = typography.listSubtitle,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            HorizontalDivider(Modifier.padding(top = 16.dp, end = 16.dp), color = colors.divider)
        }
    }
}

@Composable
private fun StartOfRoad(date: LocalDate) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        TimelineTrack(TrackMarker.End)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp, bottom = 12.dp, end = 16.dp)
                .clip(shape)
                .border(1.dp, colors.brand.copy(alpha = 0.7f), shape)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.history_start_of_road).uppercase(),
                style = typography.sectionLabel,
                color = colors.brandText,
            )
            Text(
                text = CarLogTheme.formats.fullDate(date),
                style = typography.body,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Текст, в котором [bold] выделено жирным: подставленное значение в переведённой строке. */
private fun withBold(text: String, bold: String): AnnotatedString = buildAnnotatedString {
    val start = text.indexOf(bold)
    if (start < 0) {
        append(text)
    } else {
        append(text.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
        append(text.substring(start + bold.length))
    }
}
