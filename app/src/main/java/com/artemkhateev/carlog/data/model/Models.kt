package com.artemkhateev.carlog.data.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Деньги в минимальных единицах валюты — центах, не Double: суммы за годы складываются без погрешности. */
@JvmInline
value class Money(val minor: Long) : Comparable<Money> {
    operator fun plus(other: Money) = Money(minor + other.minor)
    operator fun minus(other: Money) = Money(minor - other.minor)
    operator fun unaryMinus() = Money(-minor)
    override fun compareTo(other: Money) = minor.compareTo(other.minor)

    /** Для средних и графиков: доли цента там уже ничего не меняют. */
    fun toDouble(): Double = minor / 100.0

    companion object {
        val ZERO = Money(0)
    }
}

@JvmName("sumOfMoney")
fun Iterable<Money>.sum(): Money = Money(sumOf { it.minor })

/** Объём в тысячных долях литра (или галлона) — три знака, как на табло колонки. */
@JvmInline
value class Volume(val milli: Long) : Comparable<Volume> {
    operator fun plus(other: Volume) = Volume(milli + other.milli)
    override fun compareTo(other: Volume) = milli.compareTo(other.milli)
    fun toDouble(): Double = milli / 1000.0

    companion object {
        val ZERO = Volume(0)
    }
}

@JvmName("sumOfVolume")
fun Iterable<Volume>.sum(): Volume = Volume(sumOf { it.milli })

/** Цена единицы — литра топлива или километра поездки — в тысячных долях валюты: 1.799 €/L. */
@JvmInline
value class UnitPrice(val milli: Long) : Comparable<UnitPrice> {
    override fun compareTo(other: UnitPrice) = milli.compareTo(other.milli)
    fun toDouble(): Double = milli / 1000.0
}

data class Vehicle(
    val id: Long = 0,
    val name: String,
    val make: String = "",
    val model: String = "",
    val year: Int? = null,
    val plate: String = "",
    /** Объём бака: по нему и среднему расходу считается запас хода. */
    val tankCapacity: Volume? = null,
    /** Топливо, которое подставляется в первую заправку. Дальше — топливо прошлой заправки. */
    val fuelId: Long? = null,
    val colorIndex: Int = 0,
    val active: Boolean = true,
    val notes: String = "",
) {
    /** "Toyota Corolla" — марка и модель, если заданы. */
    val description: String get() = listOf(make, model).filter { it.isNotBlank() }.joinToString(" ")
}

/** Порядок совпадает с вкладками отчётов. */
enum class EntryType { Refueling, Expense, Income, Service, Route, Reading }

sealed interface Entry {
    val id: Long
    val vehicleId: Long

    /** Когда сделана запись; у маршрута — начало поездки. */
    val dateTime: LocalDateTime

    /** Показание одометра; у маршрута — в конце поездки. */
    val odometer: Long?
    val driverId: Long?
    val notes: String
    val type: EntryType

    /** Все показания одометра записи: у маршрута их два. */
    val odometerReadings: List<Long> get() = listOfNotNull(odometer)

    /** Сколько запись стоила. Доход и маршрут в расходы не идут. */
    val cost: Money get() = Money.ZERO
}

/** Новые сверху; в одну минуту — по одометру. */
val EntryNewestFirst: Comparator<Entry> =
    compareByDescending<Entry> { it.dateTime }
        .thenByDescending { it.odometer ?: Long.MIN_VALUE }
        .thenByDescending { it.id }

data class Refueling(
    override val id: Long = 0,
    override val vehicleId: Long,
    override val dateTime: LocalDateTime,
    override val odometer: Long,
    val fuelId: Long?,
    val unitPrice: UnitPrice,
    val totalCost: Money,
    val volume: Volume,
    /** Залит полный бак: только между полными баками расход считается точно. */
    val fullTank: Boolean = true,
    /** Предыдущая заправка не записана: отрезок, который кончается этой заправкой, в расход не идёт. */
    val missedPrevious: Boolean = false,
    val placeId: Long? = null,
    override val driverId: Long? = null,
    val paymentMethodId: Long? = null,
    val reasonId: Long? = null,
    override val notes: String = "",
) : Entry {
    override val type: EntryType get() = EntryType.Refueling
    override val cost: Money get() = totalCost
}

/** Строка расхода или сервиса: вид из справочника и сумма. */
data class CostItem(val typeId: Long, val amount: Money)

/** Расход и сервис устроены одинаково: строки по видам из справочника и скидка. */
sealed interface ItemizedEntry : Entry {
    val title: String
    val items: List<CostItem>
    val discount: Money
    val placeId: Long?
    val paymentMethodId: Long?
    val reasonId: Long?

    val total: Money get() = items.map { it.amount }.sum() - discount
    override val cost: Money get() = total
}

data class Expense(
    override val id: Long = 0,
    override val vehicleId: Long,
    override val dateTime: LocalDateTime,
    override val odometer: Long?,
    override val title: String = "",
    override val items: List<CostItem>,
    override val discount: Money = Money.ZERO,
    override val placeId: Long? = null,
    override val driverId: Long? = null,
    override val paymentMethodId: Long? = null,
    override val reasonId: Long? = null,
    override val notes: String = "",
) : ItemizedEntry {
    override val type: EntryType get() = EntryType.Expense
}

data class Service(
    override val id: Long = 0,
    override val vehicleId: Long,
    override val dateTime: LocalDateTime,
    override val odometer: Long?,
    override val title: String = "",
    override val items: List<CostItem>,
    override val discount: Money = Money.ZERO,
    override val placeId: Long? = null,
    override val driverId: Long? = null,
    override val paymentMethodId: Long? = null,
    override val reasonId: Long? = null,
    override val notes: String = "",
) : ItemizedEntry {
    override val type: EntryType get() = EntryType.Service
}

data class Income(
    override val id: Long = 0,
    override val vehicleId: Long,
    override val dateTime: LocalDateTime,
    override val odometer: Long?,
    val title: String = "",
    val typeId: Long?,
    val amount: Money,
    override val driverId: Long? = null,
    val reasonId: Long? = null,
    override val notes: String = "",
) : Entry {
    override val type: EntryType get() = EntryType.Income
}

enum class RouteKind { Trip, Freight }

data class Route(
    override val id: Long = 0,
    override val vehicleId: Long,
    val origin: String,
    val start: LocalDateTime,
    val startOdometer: Long,
    val destination: String,
    val end: LocalDateTime,
    val endOdometer: Long,
    val kind: RouteKind = RouteKind.Trip,
    /** Поездка: цена километра, необязательна. */
    val rate: UnitPrice? = null,
    /** Груз: сумма за перевозку. */
    val freightValue: Money? = null,
    override val driverId: Long? = null,
    val reasonId: Long? = null,
    override val notes: String = "",
) : Entry {
    override val type: EntryType get() = EntryType.Route
    override val dateTime: LocalDateTime get() = start
    override val odometer: Long get() = endOdometer
    override val odometerReadings: List<Long> get() = listOf(startOdometer, endOdometer)

    val distance: Long get() = endOdometer - startOdometer
    val durationMinutes: Long get() = Duration.between(start, end).toMinutes()

    /** Сколько принесла поездка: километры по цене или сумма за груз. */
    val value: Money
        get() = when (kind) {
            // Километры × тысячные доли валюты → центы с округлением половины вверх.
            RouteKind.Trip -> rate?.let { Money((distance * it.milli + 5).floorDiv(10L)) } ?: Money.ZERO
            RouteKind.Freight -> freightValue ?: Money.ZERO
        }
}

data class Reading(
    override val id: Long = 0,
    override val vehicleId: Long,
    override val dateTime: LocalDateTime,
    override val odometer: Long,
    override val driverId: Long? = null,
    override val notes: String = "",
) : Entry {
    override val type: EntryType get() = EntryType.Reading
}

/** Простые справочники: у элемента есть только имя. */
enum class CatalogKind { ServiceType, ExpenseType, IncomeType, Reason, PaymentMethod, Driver }

data class CatalogItem(val id: Long = 0, val kind: CatalogKind, val name: String)

enum class FuelCategory { Gasoline, Diesel, Ethanol, Lpg, Cng, Electricity, Other }

data class Fuel(val id: Long = 0, val name: String, val category: FuelCategory)

enum class PlaceKind { GasStation, Other }

data class Place(val id: Long = 0, val name: String, val kind: PlaceKind, val address: String = "")

/** Все справочники разом: экранам нужны имена по id. */
data class Catalogs(
    val fuels: List<Fuel> = emptyList(),
    val places: List<Place> = emptyList(),
    val items: List<CatalogItem> = emptyList(),
) {
    private val fuelById by lazy { fuels.associateBy { it.id } }
    private val placeById by lazy { places.associateBy { it.id } }
    private val itemById by lazy { items.associateBy { it.id } }

    fun fuel(id: Long?): Fuel? = id?.let { fuelById[it] }
    fun place(id: Long?): Place? = id?.let { placeById[it] }
    fun item(id: Long?): CatalogItem? = id?.let { itemById[it] }
    fun itemsOf(kind: CatalogKind): List<CatalogItem> = items.filter { it.kind == kind }
}

enum class ReminderKind { Service, Expense }

/**
 * Напоминание о сервисе или расходе: к пробегу, к дате или к тому, что наступит раньше.
 * Повторяющееся после выполнения переносится на [repeatDistance] и [repeatMonths] вперёд.
 */
data class Reminder(
    val id: Long = 0,
    val vehicleId: Long,
    val kind: ReminderKind,
    val typeId: Long?,
    val title: String = "",
    val dueOdometer: Long? = null,
    val dueDate: LocalDate? = null,
    val repeatDistance: Long? = null,
    val repeatMonths: Int? = null,
    val notes: String = "",
    /** Для какого срока уже показано уведомление: о том же сроке второй раз не напоминаем. */
    val notifiedKey: String? = null,
) {
    val repeats: Boolean get() = repeatDistance != null || repeatMonths != null

    /** Ключ текущего срока; меняется, когда напоминание переносят. */
    val dueKey: String get() = "${dueOdometer ?: ""}|${dueDate ?: ""}"
}
