package com.artemkhateev.carlog.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.domain.IncomeReport
import com.artemkhateev.carlog.domain.ItemizedReport
import com.artemkhateev.carlog.domain.MoneyStat
import com.artemkhateev.carlog.domain.MonthTotals
import com.artemkhateev.carlog.domain.PeriodPreset
import com.artemkhateev.carlog.domain.ReadingReport
import com.artemkhateev.carlog.domain.RefuelingReport
import com.artemkhateev.carlog.domain.Reports
import com.artemkhateev.carlog.domain.RouteReport
import com.artemkhateev.carlog.domain.TypeAmount
import com.artemkhateev.carlog.ui.components.AppCard
import com.artemkhateev.carlog.ui.components.BarChart
import com.artemkhateev.carlog.ui.components.BarGroup
import com.artemkhateev.carlog.ui.components.BarSegment
import com.artemkhateev.carlog.ui.components.CardHeader
import com.artemkhateev.carlog.ui.components.CardPair
import com.artemkhateev.carlog.ui.components.ChartLegend
import com.artemkhateev.carlog.ui.components.DateRangePickerDialogFor
import com.artemkhateev.carlog.ui.components.DonutChart
import com.artemkhateev.carlog.ui.components.DonutSlice
import com.artemkhateev.carlog.ui.components.LineChart
import com.artemkhateev.carlog.ui.components.LinePoint
import com.artemkhateev.carlog.ui.components.OptionSheet
import com.artemkhateev.carlog.ui.components.PeriodBox
import com.artemkhateev.carlog.ui.components.Stat
import com.artemkhateev.carlog.ui.components.StatCard
import com.artemkhateev.carlog.ui.components.StatRow
import com.artemkhateev.carlog.ui.components.accent
import com.artemkhateev.carlog.ui.components.durationText
import com.artemkhateev.carlog.ui.components.titleRes
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import com.artemkhateev.carlog.ui.theme.ChartColors
import com.artemkhateev.carlog.ui.theme.EntryColors
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/** Нет значения: мало записей, чтобы его посчитать. */
private const val NO_VALUE = "—"

enum class ReportTab(val type: EntryType?) {
    General(null),
    Refueling(EntryType.Refueling),
    Expense(EntryType.Expense),
    Income(EntryType.Income),
    Service(EntryType.Service),
    Route(EntryType.Route),
    Reading(EntryType.Reading),
}

@Composable
fun ReportsScreen() {
    val viewModel: ReportsViewModel = viewModel { ReportsViewModel(AppGraph.currentVehicle) }
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    val tabs = ReportTab.entries
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    var periodSheetOpen by rememberSaveable { mutableStateOf(false) }
    var customRangeOpen by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ReportTabRow(tabs, pagerState.currentPage) { index -> scope.launch { pagerState.animateScrollToPage(index) } }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CarLogTheme.colors.backgroundDim),
            beyondViewportPageCount = 1,
            key = { tabs[it].name },
        ) { page ->
            ReportPage(tabs[page], state, onPeriodClick = { periodSheetOpen = true })
        }
    }
    if (periodSheetOpen) {
        PeriodSheet(
            choice = state.choice,
            onPreset = { viewModel.choose(PeriodChoice.Preset(it)) },
            onCustom = { customRangeOpen = true },
            onDismiss = { periodSheetOpen = false },
        )
    }
    if (customRangeOpen) {
        DateRangePickerDialogFor(
            initial = state.reports?.range,
            onPick = { viewModel.choose(PeriodChoice.Custom(it)) },
            onDismiss = { customRangeOpen = false },
        )
    }
}

@Composable
private fun ReportTab.accent(): Color = type?.accent ?: CarLogTheme.colors.brandText

@Composable
private fun ReportTabRow(tabs: List<ReportTab>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = CarLogTheme.colors
    val listState = rememberLazyListState()
    LaunchedEffect(selected) { listState.animateScrollToItem((selected - 1).coerceAtLeast(0)) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background),
    ) {
        LazyRow(state = listState, contentPadding = PaddingValues(horizontal = 8.dp)) {
            itemsIndexed(tabs) { index, tab ->
                val isSelected = index == selected
                val accent = tab.accent()
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = stringResource(tab.type?.titleRes ?: R.string.report_general).uppercase(),
                        style = CarLogTheme.typography.tab,
                        color = if (isSelected) accent else colors.textSecondary,
                        maxLines = 1,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (isSelected) accent else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = colors.divider)
    }
}

@Composable
private fun ReportPage(tab: ReportTab, state: ReportsUiState, onPeriodClick: () -> Unit) {
    val reports = state.reports
    val accent = tab.accent()
    val periodText = periodText(tab, reports)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "period") { PeriodBox(periodText, accent, onPeriodClick) }
        if (reports != null) {
            when (tab) {
                ReportTab.General -> generalItems(reports)
                ReportTab.Refueling -> refuelingItems(reports.refueling, reports.months, state.hasTankCapacity)
                ReportTab.Expense -> itemizedItems(EntryType.Expense, reports.expense, reports.months, state.catalogs)
                ReportTab.Income -> incomeItems(reports.income, reports.months, state.catalogs)
                ReportTab.Service -> itemizedItems(EntryType.Service, reports.service, reports.months, state.catalogs)
                ReportTab.Route -> routeItems(reports.route)
                ReportTab.Reading -> readingItems(reports.reading)
            }
        }
    }
}

@Composable
private fun periodText(tab: ReportTab, reports: Reports?): String {
    if (reports == null) return stringResource(R.string.report_no_entries)
    val formats = CarLogTheme.formats
    val count = tab.type?.let { reports.countByType.getValue(it) } ?: reports.entryCount
    return pluralStringResource(R.plurals.report_entries, count, count, formats.date(reports.range.start), formats.date(reports.range.end))
}

@Composable
private fun moneyFooter(stat: MoneyStat): List<Stat> {
    val formats = CarLogTheme.formats
    return listOf(
        Stat(stringResource(R.string.report_by_day), formats.money(stat.perDay)),
        Stat(stringResource(R.string.report_by_distance, formats.distanceLabel), stat.perDistance?.let { formats.money(it) } ?: NO_VALUE),
    )
}

@Composable
private fun ChartCard(icon: ImageVector, title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        CardHeader(icon, title, accent)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

/** Помесячные столбцы одного типа записей. */
@Composable
private fun MonthlyBars(months: List<MonthTotals>, type: EntryType) {
    val formats = CarLogTheme.formats
    BarChart(
        groups = months.map { BarGroup(it.month.toString(), listOf(listOf(BarSegment(it.byType.getValue(type).toDouble(), type.accent)))) },
        axisLabel = { formats.wholeNumber(it) },
    )
}

@Composable
private fun TypeDonut(amounts: List<TypeAmount>, catalogs: Catalogs) {
    val formats = CarLogTheme.formats
    val noType = stringResource(R.string.report_no_type)
    DonutChart(
        slices = amounts.mapIndexed { index, amount ->
            DonutSlice(catalogs.item(amount.typeId)?.name ?: noType, amount.amount.toDouble(), ChartColors[index % ChartColors.size])
        },
        valueText = { formats.money(it) },
        percentText = { formats.percent(it) },
    )
}

private fun LazyListScope.generalItems(reports: Reports) {
    val general = reports.general
    item(key = "general-balance") {
        val colors = CarLogTheme.colors
        val formats = CarLogTheme.formats
        val total = stringResource(R.string.report_total)
        CardPair {
            StatCard(
                icon = if (general.balance.total.minor < 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                title = stringResource(R.string.report_balance),
                accent = colors.brandText,
                hero = Stat(total, formats.money(general.balance.total)),
                footer = moneyFooter(general.balance),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.Route,
                title = stringResource(R.string.report_distance),
                accent = EntryColors.Route,
                hero = Stat(total, formats.distance(general.distance)),
                footer = listOf(Stat(stringResource(R.string.report_daily_average), formats.distance(general.dailyDistance))),
                modifier = Modifier.weight(1f),
            )
        }
    }
    item(key = "general-cost") {
        val formats = CarLogTheme.formats
        val total = stringResource(R.string.report_total)
        CardPair {
            StatCard(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                title = stringResource(R.string.report_cost),
                accent = EntryColors.Expense,
                hero = Stat(total, formats.money(general.cost.total)),
                footer = moneyFooter(general.cost),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = stringResource(R.string.report_income),
                accent = EntryColors.Income,
                hero = Stat(total, formats.money(general.income.total)),
                footer = moneyFooter(general.income),
                modifier = Modifier.weight(1f),
            )
        }
    }
    item(key = "general-chart") {
        val formats = CarLogTheme.formats
        val costTypes = listOf(EntryType.Refueling, EntryType.Service, EntryType.Expense)
        val hasIncome = reports.months.any { it.byType.getValue(EntryType.Income).minor > 0 }
        ChartCard(Icons.Filled.BarChart, stringResource(R.string.report_general_chart), CarLogTheme.colors.brandText) {
            BarChart(
                groups = reports.months.map { month ->
                    val costs = costTypes.map { BarSegment(month.byType.getValue(it).toDouble(), it.accent) }
                    val income = listOf(BarSegment(month.byType.getValue(EntryType.Income).toDouble(), EntryType.Income.accent))
                    BarGroup(month.month.toString(), if (hasIncome) listOf(costs, income) else listOf(costs))
                },
                axisLabel = { formats.wholeNumber(it) },
            )
            val types = if (hasIncome) costTypes + EntryType.Income else costTypes
            ChartLegend(types.map { stringResource(it.titleRes) to it.accent })
        }
    }
    if (general.cost.total.minor > 0) {
        item(key = "general-split") {
            val formats = CarLogTheme.formats
            ChartCard(Icons.Filled.DonutLarge, stringResource(R.string.report_cost_split), CarLogTheme.colors.brandText) {
                DonutChart(
                    slices = general.costByType.filterValues { it.minor > 0 }.map { (type, amount) ->
                        DonutSlice(stringResource(type.titleRes), amount.toDouble(), type.accent)
                    },
                    valueText = { formats.money(it) },
                    percentText = { formats.percent(it) },
                )
            }
        }
    }
}

private fun LazyListScope.refuelingItems(report: RefuelingReport, months: List<MonthTotals>, hasTankCapacity: Boolean) {
    val accent = EntryColors.Refueling
    item(key = "refueling-cost") {
        val formats = CarLogTheme.formats
        val total = stringResource(R.string.report_total)
        CardPair {
            StatCard(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                title = stringResource(R.string.report_cost),
                accent = accent,
                hero = Stat(total, formats.money(report.cost.total)),
                footer = moneyFooter(report.cost),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.WaterDrop,
                title = stringResource(R.string.report_volume),
                accent = accent,
                hero = Stat(total, formats.volume(report.volume)),
                footer = listOf(
                    Stat(stringResource(R.string.report_per_refueling), report.volumePerRefueling?.let { formats.volume(it) } ?: NO_VALUE),
                    Stat(stringResource(R.string.report_by_day), formats.volume(report.volumePerDay)),
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
    item(key = "refueling-price") {
        val formats = CarLogTheme.formats
        val economy = report.economy
        CardPair {
            StatCard(
                icon = Icons.Filled.AttachMoney,
                title = stringResource(R.string.report_average_price),
                accent = accent,
                hero = Stat(
                    stringResource(R.string.report_price_per_unit, formats.volumeLabel),
                    report.averagePrice?.let { formats.money(it) } ?: NO_VALUE,
                ),
                footer = listOf(
                    Stat(stringResource(R.string.report_lowest), report.lowestPrice?.let { formats.unitPrice(it) } ?: NO_VALUE),
                    Stat(stringResource(R.string.report_highest), report.highestPrice?.let { formats.unitPrice(it) } ?: NO_VALUE),
                ),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                title = stringResource(R.string.report_fuel_efficiency),
                accent = accent,
                hero = Stat(stringResource(R.string.report_general_average), economy?.let { formats.consumption(it.average) } ?: NO_VALUE),
                footer = listOf(
                    // Единица уже есть в среднем — в пол-карточки она у лучшего и худшего не помещается.
                    Stat(stringResource(R.string.report_best), economy?.let { formats.consumptionNumber(it.best) } ?: NO_VALUE),
                    Stat(stringResource(R.string.report_worst), economy?.let { formats.consumptionNumber(it.worst) } ?: NO_VALUE),
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
    item(key = "refueling-routine") { RoutineCard(report, hasTankCapacity) }
    item(key = "refueling-monthly") {
        ChartCard(Icons.Filled.BarChart, stringResource(R.string.report_monthly_chart), accent) { MonthlyBars(months, EntryType.Refueling) }
    }
    if (report.economyPoints.size >= 2) {
        item(key = "refueling-efficiency") {
            val formats = CarLogTheme.formats
            ChartCard(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.report_efficiency_chart), accent) {
                LineChart(
                    points = report.economyPoints.map { LinePoint(formats.dayMonth(it.date), formats.consumptionValue(it.value)) },
                    color = accent,
                    axisLabel = { formats.decimal(it) },
                )
            }
        }
    }
    if (report.pricePoints.size >= 2) {
        item(key = "refueling-price-chart") {
            val formats = CarLogTheme.formats
            ChartCard(Icons.Filled.PriceChange, stringResource(R.string.report_price_chart), accent) {
                LineChart(
                    points = report.pricePoints.map { LinePoint(formats.dayMonth(it.date), it.value) },
                    color = accent,
                    axisLabel = { formats.decimal(it) },
                )
            }
        }
    }
}

/** Ритм заправок: пробег и дни между ними, запас хода, прогноз на месяц и доля полных баков. */
@Composable
private fun RoutineCard(report: RefuelingReport, hasTankCapacity: Boolean) {
    val colors = CarLogTheme.colors
    val formats = CarLogTheme.formats
    val accent = EntryColors.Refueling
    AppCard(Modifier.fillMaxWidth()) {
        CardHeader(Icons.Filled.Timelapse, stringResource(R.string.report_refueling_routine), accent)
        Spacer(Modifier.height(16.dp))
        StatRow(
            listOf(
                Stat(stringResource(R.string.report_between_refuelings), report.distanceBetween?.let { formats.distance(it) } ?: NO_VALUE),
                Stat(
                    stringResource(R.string.report_frequency),
                    report.daysBetween?.let { stringResource(R.string.report_days_value, formats.decimal(it)) } ?: NO_VALUE,
                ),
            ),
        )
        Spacer(Modifier.height(12.dp))
        StatRow(
            listOf(
                Stat(stringResource(R.string.report_estimated_range), report.estimatedRange?.let { formats.distance(it) } ?: NO_VALUE),
                Stat(stringResource(R.string.report_monthly_projection), formats.money(report.monthlyProjection)),
            ),
        )
        if (!hasTankCapacity) {
            Text(
                text = stringResource(R.string.report_tank_hint),
                style = CarLogTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (report.count > 0) {
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = colors.divider)
            val share = report.fullTankCount.toFloat() / report.count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(0.35f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.divider),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(share)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(accent),
                    )
                }
                Text(
                    text = stringResource(R.string.report_full_tanks, report.fullTankCount, report.count, formats.percent(share * 100.0)),
                    style = CarLogTheme.typography.listSubtitle,
                    color = colors.textPrimary,
                    modifier = Modifier
                        .weight(0.65f)
                        .padding(start = 16.dp),
                )
            }
        }
    }
}

private fun LazyListScope.itemizedItems(type: EntryType, report: ItemizedReport, months: List<MonthTotals>, catalogs: Catalogs) {
    item(key = "$type-cost") {
        StatCard(
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            title = stringResource(R.string.report_cost),
            accent = type.accent,
            hero = Stat(stringResource(R.string.report_total), CarLogTheme.formats.money(report.cost.total)),
            footer = moneyFooter(report.cost),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item(key = "$type-monthly") {
        ChartCard(Icons.Filled.BarChart, stringResource(R.string.report_monthly_chart), type.accent) { MonthlyBars(months, type) }
    }
    if (report.byType.isNotEmpty()) {
        item(key = "$type-types") {
            val title = stringResource(if (type == EntryType.Service) R.string.report_services_chart else R.string.report_expenses_chart)
            ChartCard(Icons.Filled.DonutLarge, title, type.accent) { TypeDonut(report.byType, catalogs) }
        }
    }
}

private fun LazyListScope.incomeItems(report: IncomeReport, months: List<MonthTotals>, catalogs: Catalogs) {
    val accent = EntryColors.Income
    item(key = "income-total") {
        StatCard(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            title = stringResource(R.string.report_income),
            accent = accent,
            hero = Stat(stringResource(R.string.report_total), CarLogTheme.formats.money(report.income.total)),
            footer = moneyFooter(report.income),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item(key = "income-monthly") {
        ChartCard(Icons.Filled.BarChart, stringResource(R.string.report_monthly_income_chart), accent) { MonthlyBars(months, EntryType.Income) }
    }
    if (report.byType.isNotEmpty()) {
        item(key = "income-types") {
            ChartCard(Icons.Filled.DonutLarge, stringResource(R.string.report_incomes_chart), accent) { TypeDonut(report.byType, catalogs) }
        }
    }
}

private fun LazyListScope.routeItems(report: RouteReport) {
    val accent = EntryColors.Route
    item(key = "route-value") {
        val formats = CarLogTheme.formats
        val total = stringResource(R.string.report_total)
        CardPair {
            StatCard(
                icon = Icons.Filled.AttachMoney,
                title = stringResource(R.string.report_value),
                accent = accent,
                hero = Stat(total, formats.money(report.value.total)),
                footer = moneyFooter(report.value),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.Route,
                title = stringResource(R.string.report_distance),
                accent = accent,
                hero = Stat(total, formats.distance(report.distance)),
                footer = listOf(
                    Stat(stringResource(R.string.report_per_route), report.distancePerRoute?.let { formats.distance(it) } ?: NO_VALUE),
                    Stat(stringResource(R.string.report_daily_average), formats.distance(report.dailyDistance)),
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
    item(key = "route-time") {
        StatCard(
            icon = Icons.Filled.Timer,
            title = stringResource(R.string.report_time),
            accent = accent,
            hero = Stat(stringResource(R.string.report_total), durationText(report.durationMinutes)),
            footer = listOf(
                Stat(stringResource(R.string.report_per_route), report.durationPerRoute?.let { durationText(it.roundToLong()) } ?: NO_VALUE),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun LazyListScope.readingItems(report: ReadingReport) {
    item(key = "reading-distance") {
        val formats = CarLogTheme.formats
        StatCard(
            icon = Icons.Filled.Speed,
            title = stringResource(R.string.report_distance),
            accent = EntryColors.Reading,
            hero = Stat(stringResource(R.string.report_total), formats.distance(report.distance)),
            footer = listOf(
                Stat(stringResource(R.string.report_daily_average), formats.distance(report.dailyDistance)),
                Stat(
                    stringResource(R.string.report_days_between),
                    report.daysBetween?.let { stringResource(R.string.report_days_value, formats.decimal(it)) } ?: NO_VALUE,
                ),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private sealed interface PeriodOption {
    data class Preset(val preset: PeriodPreset) : PeriodOption
    data object Custom : PeriodOption
}

@Composable
private fun PeriodSheet(choice: PeriodChoice, onPreset: (PeriodPreset) -> Unit, onCustom: () -> Unit, onDismiss: () -> Unit) {
    val labels = mapOf(
        PeriodPreset.AllTime to stringResource(R.string.period_all_time),
        PeriodPreset.ThisMonth to stringResource(R.string.period_this_month),
        PeriodPreset.LastMonth to stringResource(R.string.period_last_month),
        PeriodPreset.Last3Months to stringResource(R.string.period_last_3_months),
        PeriodPreset.Last6Months to stringResource(R.string.period_last_6_months),
        PeriodPreset.ThisYear to stringResource(R.string.period_this_year),
        PeriodPreset.Last12Months to stringResource(R.string.period_last_12_months),
    )
    val customLabel = stringResource(R.string.period_custom)
    val options: List<PeriodOption> = PeriodPreset.entries.map { PeriodOption.Preset(it) } + PeriodOption.Custom
    OptionSheet(
        title = stringResource(R.string.period_title),
        options = options,
        optionLabel = { option ->
            when (option) {
                is PeriodOption.Preset -> labels.getValue(option.preset)
                PeriodOption.Custom -> customLabel
            }
        },
        isSelected = { option ->
            when (option) {
                is PeriodOption.Preset -> choice == PeriodChoice.Preset(option.preset)
                PeriodOption.Custom -> choice is PeriodChoice.Custom
            }
        },
        onSelect = { option ->
            when (option) {
                is PeriodOption.Preset -> onPreset(option.preset)
                PeriodOption.Custom -> onCustom()
            }
        },
        onDismiss = onDismiss,
    )
}
