package com.artemkhateev.carlog.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.Income
import com.artemkhateev.carlog.data.model.ItemizedEntry
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Route
import com.artemkhateev.carlog.domain.DueStatus
import com.artemkhateev.carlog.domain.SOON_DAYS
import com.artemkhateev.carlog.domain.SOON_DISTANCE
import com.artemkhateev.carlog.ui.theme.CarLogTheme

/**
 * Заголовок записи в ленте и поиске: топливо у заправки, виды у сервиса и расхода, маршрут «откуда → куда».
 * [typeTitle] — название типа, когда больше показать нечего.
 */
fun entryTitle(entry: Entry, catalogs: Catalogs, typeTitle: (EntryType) -> String): String = when (entry) {
    is Refueling -> catalogs.fuel(entry.fuelId)?.name
    is ItemizedEntry -> entry.title.ifBlank { entry.items.mapNotNull { catalogs.item(it.typeId)?.name }.joinToString(", ") }
    is Income -> entry.title.ifBlank { catalogs.item(entry.typeId)?.name.orEmpty() }
    is Route -> listOf(entry.origin, entry.destination).filter { it.isNotBlank() }.joinToString(" → ")
    is Reading -> null
}?.takeIf { it.isNotBlank() } ?: typeTitle(entry.type)

@Composable
fun entryTitle(entry: Entry, catalogs: Catalogs): String {
    val titles = EntryType.entries.associateWith { stringResource(it.titleRes) }
    return entryTitle(entry, catalogs) { titles.getValue(it) }
}

/** Вторая строка записи. У заправки с закрытым отрезком расход выделен — ради него ленту и открывают. */
@Composable
fun entrySubtitle(entry: Entry, consumption: Double?, catalogs: Catalogs): AnnotatedString {
    val formats = CarLogTheme.formats
    val parts = mutableListOf<Pair<String, Boolean>>()
    when (entry) {
        is Refueling -> {
            parts += formats.distance(entry.odometer) to false
            if (consumption != null) {
                parts += formats.consumption(consumption) to true
                parts += formats.volume(entry.volume) to false
            } else {
                parts += formats.volume(entry.volume) to false
                parts += "${formats.unitPrice(entry.unitPrice)}/${formats.volumeLabel}" to false
            }
        }
        is Route -> {
            parts += formats.distance(entry.distance) to false
            parts += durationText(entry.durationMinutes) to false
        }
        is ItemizedEntry -> {
            entry.odometer?.let { parts += formats.distance(it) to false }
            catalogs.place(entry.placeId)?.let { parts += it.name to false }
        }
        is Income, is Reading -> entry.odometer?.let { parts += formats.distance(it) to false }
    }
    return buildAnnotatedString {
        parts.forEachIndexed { index, (text, bold) ->
            if (index > 0) append(" · ")
            if (bold) {
                withStyle(SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) { append(text) }
            } else {
                append(text)
            }
        }
    }
}

@Composable
fun durationText(minutes: Long): String {
    val safe = minutes.coerceAtLeast(0)
    return if (safe >= 60) {
        stringResource(R.string.duration_hours_minutes, safe / 60, safe % 60)
    } else {
        stringResource(R.string.duration_minutes, safe)
    }
}

/** «In 350 km · 5 days ago»: каждая часть своим цветом — просроченное красным, близкое жёлтым. */
@Composable
fun dueText(status: DueStatus): AnnotatedString {
    val colors = CarLogTheme.colors
    val formats = CarLogTheme.formats
    val parts = mutableListOf<Pair<String, Color>>()
    val distance = status.remainingDistance
    if (distance != null) {
        val text = when {
            distance > 0 -> stringResource(R.string.due_in_distance, formats.distance(distance))
            distance == 0L -> stringResource(R.string.due_now)
            else -> stringResource(R.string.due_distance_ago, formats.distance(-distance))
        }
        parts += text to dueColor(overdue = distance <= 0, soon = distance <= SOON_DISTANCE)
    }
    val days = status.remainingDays
    if (days != null) {
        val text = when {
            days > 0 -> pluralStringResource(R.plurals.due_in_days, days.toInt(), days.toInt())
            days == 0L -> stringResource(R.string.due_today)
            else -> pluralStringResource(R.plurals.due_days_ago, (-days).toInt(), (-days).toInt())
        }
        parts += text to dueColor(overdue = days < 0, soon = days <= SOON_DAYS)
    }
    return buildAnnotatedString {
        parts.forEachIndexed { index, (text, color) ->
            if (index > 0) withStyle(SpanStyle(color = colors.textSecondary)) { append(" · ") }
            withStyle(SpanStyle(color = color)) { append(text) }
        }
    }
}

@Composable
private fun dueColor(overdue: Boolean, soon: Boolean): Color {
    val colors = CarLogTheme.colors
    return when {
        overdue -> colors.danger
        soon -> colors.warning
        else -> colors.textSecondary
    }
}
