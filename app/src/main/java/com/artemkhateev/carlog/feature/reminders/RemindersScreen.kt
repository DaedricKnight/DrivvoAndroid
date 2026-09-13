package com.artemkhateev.carlog.feature.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.CurrentVehicle
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.Reminder
import com.artemkhateev.carlog.data.model.ReminderKind
import com.artemkhateev.carlog.domain.DueState
import com.artemkhateev.carlog.domain.DueStatus
import com.artemkhateev.carlog.domain.displayTitle
import com.artemkhateev.carlog.domain.status
import com.artemkhateev.carlog.ui.components.EmptyState
import com.artemkhateev.carlog.ui.components.TypeBadge
import com.artemkhateev.carlog.ui.components.accent
import com.artemkhateev.carlog.ui.components.dueText
import com.artemkhateev.carlog.ui.components.icon
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ReminderRowUi(val reminder: Reminder, val title: String, val status: DueStatus)

class RemindersViewModel(
    currentVehicle: CurrentVehicle,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** Просроченные сверху, дальше — по близости срока. */
    val rows: StateFlow<List<ReminderRowUi>?> = currentVehicle.data
        .filterNotNull()
        .map { data ->
            data.reminders
                .map { ReminderRowUi(it, it.displayTitle(data.catalogs), it.status(data.currentOdometer, today())) }
                .sortedWith(
                    compareBy(
                        { it.status.state },
                        { it.status.remainingDays ?: Long.MAX_VALUE },
                        { it.status.remainingDistance ?: Long.MAX_VALUE },
                    ),
                )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun RemindersScreen(navigator: AppNavigator) {
    val viewModel: RemindersViewModel = viewModel { RemindersViewModel(AppGraph.currentVehicle) }
    val rows = viewModel.rows.collectAsStateWithLifecycle().value ?: return
    if (rows.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Alarm,
            text = stringResource(R.string.reminders_empty),
            hint = stringResource(R.string.reminders_empty_hint),
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(rows, key = { it.reminder.id }) { row ->
            ReminderRow(row) { navigator.openReminder(row.reminder.id) }
        }
    }
}

@Composable
private fun ReminderRow(row: ReminderRowUi, onClick: () -> Unit) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    val formats = CarLogTheme.formats
    val reminder = row.reminder
    val type = if (reminder.kind == ReminderKind.Service) EntryType.Service else EntryType.Expense
    val dueLine = listOfNotNull(
        reminder.dueOdometer?.let { formats.distance(it) },
        reminder.dueDate?.let { formats.fullDate(it) },
        reminder.repeatDistance?.let { stringResource(R.string.reminder_every_distance, formats.distance(it)) },
        reminder.repeatMonths?.let { pluralStringResource(R.plurals.reminder_every_months, it, it) },
    ).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) {
            TypeBadge(type.icon, type.accent, size = 44.dp)
        }
        Column(Modifier.weight(1f)) {
            Column(Modifier.padding(top = 14.dp, bottom = 14.dp, end = 16.dp)) {
                Text(
                    text = row.title.ifBlank { stringResource(R.string.entry_reminder) },
                    style = typography.listTitle,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(dueLine, style = typography.listSubtitle, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    val tint = when (row.status.state) {
                        DueState.Overdue -> colors.danger
                        DueState.Soon -> colors.warning
                        DueState.Later -> colors.textSecondary
                    }
                    Icon(Icons.Filled.Alarm, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                    Text(dueText(row.status), style = typography.listSubtitle, modifier = Modifier.padding(start = 6.dp))
                }
            }
            HorizontalDivider(Modifier.padding(end = 16.dp), color = colors.divider)
        }
    }
}
