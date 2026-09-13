package com.artemkhateev.carlog.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.backup.DataTransfer
import com.artemkhateev.carlog.data.settings.AppSettings
import com.artemkhateev.carlog.data.settings.ConsumptionFormat
import com.artemkhateev.carlog.data.settings.DistanceUnit
import com.artemkhateev.carlog.data.settings.SettingsRepository
import com.artemkhateev.carlog.data.settings.ThemeMode
import com.artemkhateev.carlog.data.settings.VolumeUnit
import com.artemkhateev.carlog.ui.components.ConfirmDialog
import com.artemkhateev.carlog.ui.components.CurrencySheet
import com.artemkhateev.carlog.ui.components.ListRow
import com.artemkhateev.carlog.ui.components.ListScreenScaffold
import com.artemkhateev.carlog.ui.components.OptionSheet
import com.artemkhateev.carlog.ui.components.SectionLabel
import com.artemkhateev.carlog.ui.format.Formats
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val transfer: DataTransfer,
    /** Данные заменены копией — пусть напоминания проверятся по новым. */
    private val onDataReplaced: () -> Unit = {},
) : ViewModel() {

    val state: StateFlow<AppSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val mutableMessage = MutableStateFlow<Int?>(null)

    /** Строка для сообщения внизу экрана; экран показывает её и сбрасывает. */
    val message: StateFlow<Int?> = mutableMessage.asStateFlow()

    fun setCurrency(code: String) = launchSetting { settings.setCurrency(code) }
    fun setDistanceUnit(unit: DistanceUnit) = launchSetting { settings.setDistanceUnit(unit) }
    fun setVolumeUnit(unit: VolumeUnit) = launchSetting { settings.setVolumeUnit(unit) }
    fun setConsumptionFormat(format: ConsumptionFormat) = launchSetting { settings.setConsumptionFormat(format) }
    fun setThemeMode(mode: ThemeMode) = launchSetting { settings.setThemeMode(mode) }

    fun exportCsv(uri: Uri) = transferring(R.string.settings_csv_exported, R.string.settings_export_failed) { transfer.exportCsv(uri) }

    fun exportBackup(uri: Uri) = transferring(R.string.settings_backup_exported, R.string.settings_export_failed) { transfer.exportBackup(uri) }

    fun importBackup(uri: Uri) = transferring(R.string.settings_backup_imported, R.string.settings_backup_failed) {
        transfer.importBackup(uri)
        onDataReplaced()
    }

    fun messageShown() {
        mutableMessage.value = null
    }

    private fun launchSetting(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun transferring(@StringRes success: Int, @StringRes failure: Int, block: suspend () -> Unit) {
        viewModelScope.launch {
            mutableMessage.value = try {
                block()
                success
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                failure
            }
        }
    }
}

private enum class SettingsSheet { Currency, Distance, Volume, Consumption, Theme }

@Composable
fun SettingsScreen(navigator: AppNavigator) {
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(AppGraph.settings, AppGraph.dataTransfer, onDataReplaced = AppGraph.reminderScheduler::checkNow)
    }
    val settings = viewModel.state.collectAsStateWithLifecycle().value
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(context.getString(it))
            viewModel.messageShown()
        }
    }
    var sheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(viewModel::exportCsv) }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportBackup) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri?.toString() }

    ListScreenScaffold(title = stringResource(R.string.settings_title), onBack = navigator::back, onAdd = null, snackbar = snackbar) {
        if (settings != null) {
            val colors = CarLogTheme.colors
            val locale = Locale.getDefault()
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                SettingRow(
                    icon = Icons.Filled.Payments,
                    title = stringResource(R.string.settings_currency),
                    value = runCatching { Currency.getInstance(settings.currencyCode) }.getOrNull()
                        ?.let { "${it.currencyCode} · ${it.getDisplayName(locale)}" } ?: settings.currencyCode,
                ) { sheet = SettingsSheet.Currency }
                SettingRow(Icons.Filled.Straighten, stringResource(R.string.settings_distance_unit), stringResource(settings.distanceUnit.labelRes)) {
                    sheet = SettingsSheet.Distance
                }
                SettingRow(Icons.Filled.WaterDrop, stringResource(R.string.settings_volume_unit), stringResource(settings.volumeUnit.labelRes)) {
                    sheet = SettingsSheet.Volume
                }
                SettingRow(Icons.Filled.Speed, stringResource(R.string.settings_consumption), consumptionLabel(settings, settings.consumptionFormat)) {
                    sheet = SettingsSheet.Consumption
                }
                SettingRow(Icons.Filled.DarkMode, stringResource(R.string.settings_theme), stringResource(settings.themeMode.labelRes)) {
                    sheet = SettingsSheet.Theme
                }
                Text(
                    text = stringResource(R.string.settings_units_hint),
                    style = CarLogTheme.typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp, top = 8.dp),
                )
                SectionLabel(stringResource(R.string.settings_data), Modifier.padding(start = 72.dp, top = 24.dp, bottom = 4.dp))
                val today = LocalDate.now()
                SettingRow(Icons.Filled.FileDownload, stringResource(R.string.settings_export_csv), null) {
                    csvLauncher.launch("carlog-records-$today.csv")
                }
                SettingRow(Icons.Filled.Backup, stringResource(R.string.settings_backup_export), null) {
                    backupLauncher.launch("carlog-backup-$today.json")
                }
                SettingRow(Icons.Filled.Restore, stringResource(R.string.settings_backup_import), null) {
                    importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                }
            }

            when (sheet) {
                SettingsSheet.Currency -> CurrencySheet(settings.currencyCode, viewModel::setCurrency) { sheet = null }
                SettingsSheet.Distance -> EnumSheet(R.string.settings_distance_unit, DistanceUnit.entries, settings.distanceUnit, { stringResource(it.labelRes) }, viewModel::setDistanceUnit) { sheet = null }
                SettingsSheet.Volume -> EnumSheet(R.string.settings_volume_unit, VolumeUnit.entries, settings.volumeUnit, { stringResource(it.labelRes) }, viewModel::setVolumeUnit) { sheet = null }
                SettingsSheet.Consumption -> {
                    val labels = ConsumptionFormat.entries.associateWith { consumptionLabel(settings, it) }
                    EnumSheet(R.string.settings_consumption, ConsumptionFormat.entries, settings.consumptionFormat, { labels.getValue(it) }, viewModel::setConsumptionFormat) {
                        sheet = null
                    }
                }
                SettingsSheet.Theme -> EnumSheet(R.string.settings_theme, ThemeMode.entries, settings.themeMode, { stringResource(it.labelRes) }, viewModel::setThemeMode) { sheet = null }
                null -> Unit
            }
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.settings_backup_import),
            message = stringResource(R.string.settings_backup_import_message),
            confirmLabel = stringResource(R.string.action_ok),
            onConfirm = { viewModel.importBackup(Uri.parse(uri)) },
            onDismiss = { pendingImport = null },
        )
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, value: String?, onClick: () -> Unit) {
    ListRow(
        title = title,
        subtitle = value,
        onClick = onClick,
        divider = false,
        leading = { Icon(icon, contentDescription = null, tint = CarLogTheme.colors.icon) },
    )
}

/** Шторка выбора значения настройки. [label] вызывается при отрисовке строк, поэтому может читать ресурсы. */
@Composable
private fun <T> EnumSheet(
    @StringRes title: Int,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val labels = options.associateWith { label(it) }
    OptionSheet(
        title = stringResource(title),
        options = options,
        optionLabel = { labels.getValue(it) },
        isSelected = { it == selected },
        onSelect = onSelect,
        onDismiss = onDismiss,
    )
}

private fun consumptionLabel(settings: AppSettings, format: ConsumptionFormat): String =
    Formats.from(settings.copy(consumptionFormat = format)).consumptionLabel

@get:StringRes
private val DistanceUnit.labelRes: Int
    get() = when (this) {
        DistanceUnit.Kilometer -> R.string.unit_kilometers
        DistanceUnit.Mile -> R.string.unit_miles
    }

@get:StringRes
private val VolumeUnit.labelRes: Int
    get() = when (this) {
        VolumeUnit.Liter -> R.string.unit_liters
        VolumeUnit.UsGallon -> R.string.unit_us_gallons
        VolumeUnit.ImperialGallon -> R.string.unit_imperial_gallons
    }

@get:StringRes
private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Dark -> R.string.theme_dark
        ThemeMode.Light -> R.string.theme_light
    }
