package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.Expense
import com.artemkhateev.carlog.data.model.Income
import com.artemkhateev.carlog.data.model.ItemizedEntry
import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Route
import com.artemkhateev.carlog.data.model.Service
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Volume
import com.artemkhateev.carlog.data.model.sum
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Сумма за период и она же в пересчёте на день и на единицу пути. */
data class MoneyStat(val total: Money, val perDay: Double, val perDistance: Double?) {
    companion object {
        fun of(total: Money, range: DateRange, distance: Long) = MoneyStat(
            total = total,
            perDay = total.toDouble() / range.days,
            perDistance = if (distance > 0) total.toDouble() / distance else null,
        )
    }
}

data class GeneralReport(
    val entryCount: Int,
    val balance: MoneyStat,
    val cost: MoneyStat,
    val income: MoneyStat,
    val distance: Long,
    val dailyDistance: Double,
    /** Расходы по типам записей: заправки, сервис, прочие расходы. */
    val costByType: Map<EntryType, Money>,
)

data class RefuelingReport(
    val count: Int,
    val cost: MoneyStat,
    val volume: Volume,
    val volumePerRefueling: Double?,
    val volumePerDay: Double,
    /** Средняя цена — все деньги на весь объём. */
    val averagePrice: Double?,
    val lowestPrice: UnitPrice?,
    val highestPrice: UnitPrice?,
    /** Отрезки, закончившиеся в периоде. */
    val economy: FuelEconomy?,
    val distanceBetween: Double?,
    val daysBetween: Double?,
    /** Запас хода на полном баке при среднем расходе. */
    val estimatedRange: Double?,
    /** Сколько заправки обойдутся за 30 дней при нынешнем темпе. */
    val monthlyProjection: Double,
    val fullTankCount: Int,
    val economyPoints: List<DatedValue>,
    val pricePoints: List<DatedValue>,
)

data class DatedValue(val date: LocalDate, val value: Double)

/** Сумма по виду расхода, сервиса или дохода; null — доход без вида. */
data class TypeAmount(val typeId: Long?, val amount: Money)

data class ItemizedReport(val count: Int, val cost: MoneyStat, val byType: List<TypeAmount>)

data class IncomeReport(val count: Int, val income: MoneyStat, val byType: List<TypeAmount>)

data class RouteReport(
    val count: Int,
    val value: MoneyStat,
    val distance: Long,
    val distancePerRoute: Double?,
    val dailyDistance: Double,
    val durationMinutes: Long,
    val durationPerRoute: Double?,
)

data class ReadingReport(val count: Int, val distance: Long, val dailyDistance: Double, val daysBetween: Double?)

/** Суммы месяца по типам записей: у дохода — доход, у маршрута — его стоимость, у остальных — расход. */
data class MonthTotals(val month: YearMonth, val byType: Map<EntryType, Money>)

data class Reports(
    val range: DateRange,
    val entryCount: Int,
    val countByType: Map<EntryType, Int>,
    val general: GeneralReport,
    val refueling: RefuelingReport,
    val expense: ItemizedReport,
    val income: IncomeReport,
    val service: ItemizedReport,
    val route: RouteReport,
    val reading: ReadingReport,
    val months: List<MonthTotals>,
)

/** [entries] — все записи машины: пробег и расход считаются с учётом записей до периода. */
fun buildReports(entries: List<Entry>, range: DateRange, tankCapacity: Volume?): Reports {
    val inRange = entries.filter { it.dateTime in range }
    val distance = distanceIn(entries, range)
    val totalCost = inRange.map { it.cost }.sum()
    val incomes = inRange.filterIsInstance<Income>()
    val totalIncome = incomes.map { it.amount }.sum()
    val refuelingReport = refuelingReport(
        all = entries.filterIsInstance<Refueling>(),
        inRange = inRange.filterIsInstance<Refueling>(),
        range = range,
        distance = distance,
        tankCapacity = tankCapacity,
    )
    return Reports(
        range = range,
        entryCount = inRange.size,
        countByType = EntryType.entries.associateWith { type -> inRange.count { it.type == type } },
        general = GeneralReport(
            entryCount = inRange.size,
            balance = MoneyStat.of(totalIncome - totalCost, range, distance),
            cost = MoneyStat.of(totalCost, range, distance),
            income = MoneyStat.of(totalIncome, range, distance),
            distance = distance,
            dailyDistance = distance.toDouble() / range.days,
            costByType = listOf(EntryType.Refueling, EntryType.Service, EntryType.Expense)
                .associateWith { type -> inRange.filter { it.type == type }.map { it.cost }.sum() },
        ),
        refueling = refuelingReport,
        expense = itemizedReport(inRange.filterIsInstance<Expense>(), range, distance),
        income = IncomeReport(
            count = incomes.size,
            income = MoneyStat.of(totalIncome, range, distance),
            byType = incomes.groupBy { it.typeId }
                .map { (typeId, list) -> TypeAmount(typeId, list.map { it.amount }.sum()) }
                .sortedByDescending { it.amount },
        ),
        service = itemizedReport(inRange.filterIsInstance<Service>(), range, distance),
        route = routeReport(inRange.filterIsInstance<Route>(), range),
        reading = readingReport(inRange.filterIsInstance<Reading>(), range),
        months = monthlyTotals(inRange, range),
    )
}

private fun refuelingReport(
    all: List<Refueling>,
    inRange: List<Refueling>,
    range: DateRange,
    distance: Long,
    tankCapacity: Volume?,
): RefuelingReport {
    val cost = MoneyStat.of(inRange.map { it.totalCost }.sum(), range, distance)
    val volume = inRange.map { it.volume }.sum()
    val segments = fuelSegments(all).filter { it.end.dateTime in range }
    val economy = fuelEconomy(segments)
    val intervals = inRange.size - 1
    val byOdometer = inRange.sortedWith(compareBy({ it.odometer }, { it.dateTime }))
    val byDate = inRange.sortedBy { it.dateTime }
    return RefuelingReport(
        count = inRange.size,
        cost = cost,
        volume = volume,
        volumePerRefueling = if (inRange.isEmpty()) null else volume.toDouble() / inRange.size,
        volumePerDay = volume.toDouble() / range.days,
        averagePrice = if (volume.milli > 0) cost.total.toDouble() / volume.toDouble() else null,
        lowestPrice = inRange.minOfOrNull { it.unitPrice },
        highestPrice = inRange.maxOfOrNull { it.unitPrice },
        economy = economy,
        distanceBetween = if (intervals > 0) (byOdometer.last().odometer - byOdometer.first().odometer).toDouble() / intervals else null,
        daysBetween = if (intervals > 0) {
            ChronoUnit.DAYS.between(byDate.first().dateTime.toLocalDate(), byDate.last().dateTime.toLocalDate()).toDouble() / intervals
        } else {
            null
        },
        estimatedRange = if (tankCapacity != null && economy != null && economy.average > 0) {
            tankCapacity.toDouble() / economy.average * 100
        } else {
            null
        },
        monthlyProjection = cost.perDay * 30,
        fullTankCount = inRange.count { it.fullTank },
        economyPoints = segments.sortedBy { it.end.dateTime }.map { DatedValue(it.end.dateTime.toLocalDate(), it.volumePer100) },
        pricePoints = byDate.map { DatedValue(it.dateTime.toLocalDate(), it.unitPrice.toDouble()) },
    )
}

private fun itemizedReport(entries: List<ItemizedEntry>, range: DateRange, distance: Long) = ItemizedReport(
    count = entries.size,
    cost = MoneyStat.of(entries.map { it.total }.sum(), range, distance),
    // Скидка — на запись целиком, поэтому доли видов считаются до скидки.
    byType = entries.flatMap { it.items }
        .groupBy { it.typeId }
        .map { (typeId, items) -> TypeAmount(typeId, items.map { it.amount }.sum()) }
        .sortedByDescending { it.amount },
)

private fun routeReport(routes: List<Route>, range: DateRange): RouteReport {
    val distance = routes.sumOf { it.distance.coerceAtLeast(0) }
    val minutes = routes.sumOf { it.durationMinutes.coerceAtLeast(0) }
    return RouteReport(
        count = routes.size,
        value = MoneyStat.of(routes.map { it.value }.sum(), range, distance),
        distance = distance,
        distancePerRoute = if (routes.isEmpty()) null else distance.toDouble() / routes.size,
        dailyDistance = distance.toDouble() / range.days,
        durationMinutes = minutes,
        durationPerRoute = if (routes.isEmpty()) null else minutes.toDouble() / routes.size,
    )
}

private fun readingReport(readings: List<Reading>, range: DateRange): ReadingReport {
    val byDate = readings.sortedBy { it.dateTime }
    val enough = readings.size >= 2
    val distance = if (enough) readings.maxOf { it.odometer } - readings.minOf { it.odometer } else 0
    return ReadingReport(
        count = readings.size,
        distance = distance,
        dailyDistance = distance.toDouble() / range.days,
        daysBetween = if (enough) {
            ChronoUnit.DAYS.between(byDate.first().dateTime.toLocalDate(), byDate.last().dateTime.toLocalDate()).toDouble() /
                (readings.size - 1)
        } else {
            null
        },
    )
}

private fun monthlyTotals(inRange: List<Entry>, range: DateRange): List<MonthTotals> {
    val byMonth = inRange.groupBy { YearMonth.from(it.dateTime) }
    return range.months.map { month ->
        val monthEntries = byMonth[month].orEmpty()
        MonthTotals(
            month = month,
            byType = EntryType.entries.associateWith { type ->
                monthEntries.filter { it.type == type }.map { chartAmount(it) }.sum()
            },
        )
    }
}

private fun chartAmount(entry: Entry): Money = when (entry) {
    is Income -> entry.amount
    is Route -> entry.value
    else -> entry.cost
}
