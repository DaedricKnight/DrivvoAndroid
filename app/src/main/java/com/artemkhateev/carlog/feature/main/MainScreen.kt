package com.artemkhateev.carlog.feature.main

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.VehicleSelection
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.feature.history.HistoryScreen
import com.artemkhateev.carlog.feature.more.MoreScreen
import com.artemkhateev.carlog.feature.reminders.RemindersScreen
import com.artemkhateev.carlog.feature.reports.ReportsScreen
import com.artemkhateev.carlog.ui.components.EmptyState
import com.artemkhateev.carlog.ui.components.ReminderIcon
import com.artemkhateev.carlog.ui.components.SaveButton
import com.artemkhateev.carlog.ui.components.TitleTopBar
import com.artemkhateev.carlog.ui.components.TopBarAction
import com.artemkhateev.carlog.ui.components.TypeBadge
import com.artemkhateev.carlog.ui.components.VehicleAvatar
import com.artemkhateev.carlog.ui.components.VehicleTopBar
import com.artemkhateev.carlog.ui.components.accent
import com.artemkhateev.carlog.ui.components.icon
import com.artemkhateev.carlog.ui.components.titleRes
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import com.artemkhateev.carlog.ui.theme.EntryColors
import kotlinx.coroutines.launch

enum class MainTab { History, Reports, Reminders, More }

/** Высота строки нижней панели: по ней кнопка «+» над затемнением встаёт точно на своё место. */
private val BottomBarHeight = 80.dp
private val AddButtonSize = 56.dp

@Composable
fun MainScreen(navigator: AppNavigator) {
    val currentVehicle = AppGraph.currentVehicle
    val selection = currentVehicle.selection.collectAsStateWithLifecycle().value ?: return
    val data by currentVehicle.data.collectAsStateWithLifecycle()
    val vehicle = selection.selected
    val colors = CarLogTheme.colors
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableStateOf(MainTab.History) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var vehicleSheetOpen by rememberSaveable { mutableStateOf(false) }
    val tabStates = rememberSaveableStateHolder()

    if (vehicle == null) {
        NoVehicles(onAdd = { navigator.openVehicle() })
        return
    }

    BackHandler(enabled = menuOpen) { menuOpen = false }
    BackHandler(enabled = !menuOpen && tab != MainTab.History) { tab = MainTab.History }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            if (tab == MainTab.More) {
                TitleTopBar(stringResource(R.string.more_title), colors.brand, onBack = null)
            } else {
                VehicleTopBar(
                    color = if (tab == MainTab.Reminders) EntryColors.Reminder else colors.brand,
                    vehicle = vehicle,
                    odometer = data?.currentOdometer,
                    onVehicleClick = { vehicleSheetOpen = true },
                ) {
                    if (tab != MainTab.Reminders) {
                        TopBarAction(Icons.Filled.Search, stringResource(R.string.action_search), navigator::openSearch)
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                tabStates.SaveableStateProvider(tab) {
                    when (tab) {
                        MainTab.History -> HistoryScreen(navigator)
                        MainTab.Reports -> ReportsScreen()
                        MainTab.Reminders -> RemindersScreen(navigator)
                        MainTab.More -> MoreScreen(navigator)
                    }
                }
            }
            BottomBar(
                tab = tab,
                onTab = {
                    tab = it
                    menuOpen = false
                },
                menuOpen = menuOpen,
                onAddClick = { menuOpen = !menuOpen },
            )
        }
        if (menuOpen) {
            AddMenuOverlay(
                onDismiss = { menuOpen = false },
                onPick = { action ->
                    menuOpen = false
                    when (action) {
                        AddAction.Reminder -> navigator.openReminder()
                        else -> navigator.openEntry(action.entryType!!)
                    }
                },
            )
        }
    }
    if (vehicleSheetOpen) {
        VehicleSheet(
            selection = selection,
            onSelect = { scope.launch { currentVehicle.select(it.id) } },
            onManage = navigator::openVehicles,
            onDismiss = { vehicleSheetOpen = false },
        )
    }
}

@Composable
private fun NoVehicles(onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(CarLogTheme.colors.background),
    ) {
        TitleTopBar(stringResource(R.string.app_name), CarLogTheme.colors.brand, onBack = null)
        Box(Modifier.weight(1f)) {
            EmptyState(Icons.Filled.DirectionsCar, stringResource(R.string.no_vehicles))
        }
        SaveButton(
            text = stringResource(R.string.add_vehicle),
            accent = CarLogTheme.colors.brand,
            onClick = onAdd,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun BottomBar(tab: MainTab, onTab: (MainTab) -> Unit, menuOpen: Boolean, onAddClick: () -> Unit) {
    val colors = CarLogTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background),
    ) {
        HorizontalDivider(color = colors.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(BottomBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(MainTab.History, tab, Icons.AutoMirrored.Filled.FormatListBulleted, R.string.tab_history, onTab)
            NavItem(MainTab.Reports, tab, Icons.Filled.StackedLineChart, R.string.tab_reports, onTab)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AddButton(open = menuOpen, onClick = onAddClick)
            }
            NavItem(MainTab.Reminders, tab, Icons.Filled.Alarm, R.string.tab_reminders, onTab)
            NavItem(MainTab.More, tab, Icons.Filled.MoreHoriz, R.string.tab_more, onTab)
        }
    }
}

@Composable
private fun RowScope.NavItem(
    item: MainTab,
    current: MainTab,
    icon: ImageVector,
    @StringRes label: Int,
    onTab: (MainTab) -> Unit,
) {
    val colors = CarLogTheme.colors
    val selected = item == current
    val tint = if (selected) colors.brandText else colors.textPrimary
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTab(item) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) colors.navIndicator else Color.Transparent)
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint)
        }
        Text(
            text = stringResource(label),
            style = CarLogTheme.typography.navLabel,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AddButton(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(if (open) 45f else 0f, label = "add-rotation")
    Box(
        modifier = modifier
            .size(AddButtonSize)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(CarLogTheme.colors.brand)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.add_record),
            tint = Color.White,
            modifier = Modifier
                .size(30.dp)
                .rotate(rotation),
        )
    }
}

/** Пункты меню «+» сверху вниз; ближе всего к кнопке — заправка, самая частая запись. */
private enum class AddAction(val entryType: EntryType?) {
    Reminder(null),
    Reading(EntryType.Reading),
    Route(EntryType.Route),
    Service(EntryType.Service),
    Income(EntryType.Income),
    Expense(EntryType.Expense),
    Refueling(EntryType.Refueling),
}

@Composable
private fun AddMenuOverlay(onDismiss: () -> Unit, onPick: (AddAction) -> Unit) {
    val colors = CarLogTheme.colors
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = BottomBarHeight + 4.dp)
                .width(240.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.card)
                .padding(vertical = 10.dp),
        ) {
            AddAction.entries.forEach { action ->
                val type = action.entryType
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(action) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TypeBadge(
                        icon = type?.icon ?: ReminderIcon,
                        color = type?.accent ?: EntryColors.Reminder,
                        size = 44.dp,
                        iconSize = 24.dp,
                    )
                    Text(
                        text = stringResource(type?.titleRes ?: R.string.entry_reminder),
                        style = CarLogTheme.typography.listTitle,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
        // Кнопка поверх затемнения — на том же месте, что и в панели: ею меню и закрывается.
        AddButton(
            open = true,
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = (BottomBarHeight - AddButtonSize) / 2),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleSheet(
    selection: VehicleSelection,
    onSelect: (Vehicle) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.card) {
        Text(
            text = stringResource(R.string.select_vehicle),
            style = typography.cardTitle,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Column(Modifier.navigationBarsPadding()) {
            selection.vehicles.forEach { vehicle ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(vehicle)
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VehicleAvatar(vehicle, size = 40.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                    ) {
                        Text(vehicle.name, style = typography.listTitle, color = colors.textPrimary)
                        val subtitle = listOf(vehicle.description, vehicle.plate).filter { it.isNotBlank() }.joinToString(" · ")
                        if (subtitle.isNotEmpty()) Text(subtitle, style = typography.listSubtitle, color = colors.textSecondary)
                    }
                    if (vehicle.id == selection.selected?.id) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = colors.brandText)
                    }
                }
            }
            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onManage()
                    }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = colors.icon, modifier = Modifier.width(40.dp))
                Text(
                    text = stringResource(R.string.manage_vehicles),
                    style = typography.body,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}
