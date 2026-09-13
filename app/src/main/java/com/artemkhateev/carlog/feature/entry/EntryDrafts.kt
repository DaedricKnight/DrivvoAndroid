package com.artemkhateev.carlog.feature.entry

import com.artemkhateev.carlog.data.model.CostItem
import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.Expense
import com.artemkhateev.carlog.data.model.Income
import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Route
import com.artemkhateev.carlog.data.model.RouteKind
import com.artemkhateev.carlog.data.model.Service
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.data.model.Volume
import com.artemkhateev.carlog.data.model.sum
import com.artemkhateev.carlog.domain.OdometerBounds
import com.artemkhateev.carlog.domain.OdometerViolation
import com.artemkhateev.carlog.domain.lastOdometer
import com.artemkhateev.carlog.ui.format.parseMoney
import com.artemkhateev.carlog.ui.format.parseUnitPrice
import com.artemkhateev.carlog.ui.format.parseVolume
import com.artemkhateev.carlog.ui.format.parseWhole
import com.artemkhateev.carlog.ui.format.sanitizeDecimalInput
import com.artemkhateev.carlog.ui.format.toInputText
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Поля форм, у которых бывает ошибка. */
enum class DraftField { Odometer, StartOdometer, EndOdometer, UnitPrice, Total, Volume, Items, ItemAmount, Discount, Amount, IncomeType, End, FreightValue }

sealed interface FieldError {
    data object Required : FieldError
    data object NotPositive : FieldError
    data class BelowPrevious(val previous: Long) : FieldError
    data class AboveNext(val next: Long) : FieldError
    data object EndBeforeStart : FieldError
    data object DiscountTooBig : FieldError
    data object NoTypes : FieldError
}

/** Границы одометра на момент записи — по остальным записям её машины. */
fun interface BoundsAt {
    fun at(dateTime: LocalDateTime): OdometerBounds
}

/**
 * Форма записи. Числа хранятся текстом — так, как их набирают; в запись превращаются, только когда
 * ошибок нет. Общие поля меняются через with-методы: ими пользуются строки, одинаковые во всех формах.
 */
sealed interface EntryDraft {
    val id: Long
    val vehicleId: Long
    val type: EntryType
    val date: LocalDate
    val time: LocalTime
    val driverId: Long?
    val notes: String

    val dateTime: LocalDateTime get() = date.atTime(time)

    /** Пусто — запись можно сохранить. */
    fun errors(bounds: BoundsAt): Map<DraftField, FieldError>

    fun toEntry(bounds: BoundsAt): Entry?

    fun withVehicle(vehicleId: Long): EntryDraft
    fun withDate(date: LocalDate): EntryDraft
    fun withTime(time: LocalTime): EntryDraft
    fun withDriver(driverId: Long?): EntryDraft
    fun withNotes(notes: String): EntryDraft
}

/** Три поля цены заправки; третье считается из двух, введённых последними. */
enum class PriceField { UnitPrice, Total, Volume }

data class RefuelingDraft(
    override val id: Long = 0,
    override val vehicleId: Long,
    override val date: LocalDate,
    override val time: LocalTime,
    val odometerText: String = "",
    val fuelId: Long? = null,
    val priceText: String = "",
    val totalText: String = "",
    val volumeText: String = "",
    /** Поля цены, введённые руками, от старого к новому — не больше двух. */
    val typedPriceFields: List<PriceField> = emptyList(),
    val fullTank: Boolean = true,
    val missedPrevious: Boolean = false,
    val placeId: Long? = null,
    override val driverId: Long? = null,
    val paymentMethodId: Long? = null,
    val reasonId: Long? = null,
    override val notes: String = "",
) : EntryDraft {
    override val type: EntryType get() = EntryType.Refueling

    val unitPrice: UnitPrice? get() = parseUnitPrice(priceText)?.takeIf { it.milli > 0 }
    val total: Money? get() = parseMoney(totalText)?.takeIf { it.minor > 0 }
    val volume: Volume? get() = parseVolume(volumeText)?.takeIf { it.milli > 0 }

    /** Ввод в поле цены: поле запоминается, а то, которое дольше всех не трогали, пересчитывается. */
    fun withPriceInput(field: PriceField, raw: String): RefuelingDraft {
        val text = sanitizeDecimalInput(raw, maxDecimals = if (field == PriceField.Total) 2 else 3)
        val typed = (typedPriceFields - field + field).takeLast(2)
        val edited = when (field) {
            PriceField.UnitPrice -> copy(priceText = text)
            PriceField.Total -> copy(totalText = text)
            PriceField.Volume -> copy(volumeText = text)
        }
        return edited.copy(typedPriceFields = typed).recalculated()
    }

    private fun recalculated(): RefuelingDraft {
        if (typedPriceFields.size < 2) return this
        val price = unitPrice
        val cost = total
        val liters = volume
        // Считать не из чего — пересчитанное поле очищается, чтобы не остались цифры от прошлого ввода.
        return when (PriceField.entries.first { it !in typedPriceFields }) {
            PriceField.Volume -> copy(volumeText = if (price != null && cost != null) volumeFor(cost, price).toInputText() else "")
            PriceField.Total -> copy(totalText = if (price != null && liters != null) totalFor(price, liters).toInputText() else "")
            PriceField.UnitPrice -> copy(priceText = if (cost != null && liters != null) priceFor(cost, liters).toInputText() else "")
        }
    }

    override fun errors(bounds: BoundsAt): Map<DraftField, FieldError> = buildMap {
        odometerError(odometerText, required = true, bounds.at(dateTime))?.let { put(DraftField.Odometer, it) }
        positiveError(priceText, unitPrice)?.let { put(DraftField.UnitPrice, it) }
        positiveError(totalText, total)?.let { put(DraftField.Total, it) }
        positiveError(volumeText, volume)?.let { put(DraftField.Volume, it) }
    }

    override fun toEntry(bounds: BoundsAt): Refueling? {
        if (errors(bounds).isNotEmpty()) return null
        return Refueling(
            id = id,
            vehicleId = vehicleId,
            dateTime = dateTime,
            odometer = parseWhole(odometerText) ?: return null,
            fuelId = fuelId,
            unitPrice = unitPrice ?: return null,
            totalCost = total ?: return null,
            volume = volume ?: return null,
            fullTank = fullTank,
            missedPrevious = missedPrevious,
            placeId = placeId,
            driverId = driverId,
            paymentMethodId = paymentMethodId,
            reasonId = reasonId,
            notes = notes.trim(),
        )
    }

    override fun withVehicle(vehicleId: Long) = copy(vehicleId = vehicleId)
    override fun withDate(date: LocalDate) = copy(date = date)
    override fun withTime(time: LocalTime) = copy(time = time)
    override fun withDriver(driverId: Long?) = copy(driverId = driverId)
    override fun withNotes(notes: String) = copy(notes = notes)
}

data class ItemDraft(val typeId: Long, val amountText: String = "") {
    val amount: Money? get() = parseMoney(amountText)
}

/** Форма расхода или сервиса — они устроены одинаково. */
data class ItemizedDraft(
    override val type: EntryType,
    override val id: Long = 0,
    override val vehicleId: Long,
    override val date: LocalDate,
    override val time: LocalTime,
    val odometerText: String = "",
    val title: String = "",
    val items: List<ItemDraft> = emptyList(),
    val discountText: String = "",
    val placeId: Long? = null,
    override val driverId: Long? = null,
    val paymentMethodId: Long? = null,
    val reasonId: Long? = null,
    override val notes: String = "",
) : EntryDraft {
    init {
        require(type == EntryType.Expense || type == EntryType.Service) { "ItemizedDraft только для расхода и сервиса: $type" }
    }

    val subtotal: Money get() = items.mapNotNull { it.amount }.sum()
    val discount: Money get() = parseMoney(discountText) ?: Money.ZERO
    val total: Money get() = subtotal - discount

    /** Выбор видов: суммы у оставшихся сохраняются, новые добавляются в конец пустыми. */
    fun withTypes(typeIds: Set<Long>): ItemizedDraft {
        val kept = items.filter { it.typeId in typeIds }
        val added = typeIds.filter { id -> kept.none { it.typeId == id } }.map { ItemDraft(it) }
        return copy(items = kept + added)
    }

    fun withItemAmount(typeId: Long, raw: String) = copy(
        items = items.map { if (it.typeId == typeId) it.copy(amountText = sanitizeDecimalInput(raw, maxDecimals = 2)) else it },
    )

    override fun errors(bounds: BoundsAt): Map<DraftField, FieldError> = buildMap {
        // У сервиса пробег нужен всегда: по нему переносятся напоминания «каждые N км».
        odometerError(odometerText, required = type == EntryType.Service, bounds.at(dateTime))?.let { put(DraftField.Odometer, it) }
        if (items.isEmpty()) put(DraftField.Items, FieldError.NoTypes)
        if (items.any { it.amountText.isBlank() }) put(DraftField.ItemAmount, FieldError.Required)
        if (discount > subtotal) put(DraftField.Discount, FieldError.DiscountTooBig)
    }

    override fun toEntry(bounds: BoundsAt): Entry? {
        if (errors(bounds).isNotEmpty()) return null
        val odometer = parseWhole(odometerText)
        val costItems = items.map { CostItem(it.typeId, it.amount ?: return null) }
        return when (type) {
            EntryType.Service -> Service(
                id = id,
                vehicleId = vehicleId,
                dateTime = dateTime,
                odometer = odometer,
                title = title.trim(),
                items = costItems,
                discount = discount,
                placeId = placeId,
                driverId = driverId,
                paymentMethodId = paymentMethodId,
                reasonId = reasonId,
                notes = notes.trim(),
            )
            else -> Expense(
                id = id,
                vehicleId = vehicleId,
                dateTime = dateTime,
                odometer = odometer,
                title = title.trim(),
                items = costItems,
                discount = discount,
                placeId = placeId,
                driverId = driverId,
                paymentMethodId = paymentMethodId,
                reasonId = reasonId,
                notes = notes.trim(),
            )
        }
    }

    override fun withVehicle(vehicleId: Long) = copy(vehicleId = vehicleId)
    override fun withDate(date: LocalDate) = copy(date = date)
    override fun withTime(time: LocalTime) = copy(time = time)
    override fun withDriver(driverId: Long?) = copy(driverId = driverId)
    override fun withNotes(notes: String) = copy(notes = notes)
}

data class IncomeDraft(
    override val id: Long = 0,
    override val vehicleId: Long,
    override val date: LocalDate,
    override val time: LocalTime,
    val odometerText: String = "",
    val title: String = "",
    val typeId: Long? = null,
    val amountText: String = "",
    override val driverId: Long? = null,
    val reasonId: Long? = null,
    override val notes: String = "",
) : EntryDraft {
    override val type: EntryType get() = EntryType.Income

    val amount: Money? get() = parseMoney(amountText)?.takeIf { it.minor > 0 }

    override fun errors(bounds: BoundsAt): Map<DraftField, FieldError> = buildMap {
        odometerError(odometerText, required = false, bounds.at(dateTime))?.let { put(DraftField.Odometer, it) }
        if (typeId == null) put(DraftField.IncomeType, FieldError.Required)
        positiveError(amountText, amount)?.let { put(DraftField.Amount, it) }
    }

    override fun toEntry(bounds: BoundsAt): Income? {
        if (errors(bounds).isNotEmpty()) return null
        return Income(
            id = id,
            vehicleId = vehicleId,
            dateTime = dateTime,
            odometer = parseWhole(odometerText),
            title = title.trim(),
            typeId = typeId,
            amount = amount ?: return null,
            driverId = driverId,
            reasonId = reasonId,
            notes = notes.trim(),
        )
    }

    override fun withVehicle(vehicleId: Long) = copy(vehicleId = vehicleId)
    override fun withDate(date: LocalDate) = copy(date = date)
    override fun withTime(time: LocalTime) = copy(time = time)
    override fun withDriver(driverId: Long?) = copy(driverId = driverId)
    override fun withNotes(notes: String) = copy(notes = notes)
}

/** Маршрут. [date] и [time] — начало поездки. */
data class RouteDraft(
    override val id: Long = 0,
    override val vehicleId: Long,
    val origin: String = "",
    override val date: LocalDate,
    override val time: LocalTime,
    val startOdometerText: String = "",
    val destination: String = "",
    val endDate: LocalDate,
    val endTime: LocalTime,
    val endOdometerText: String = "",
    val kind: RouteKind = RouteKind.Trip,
    val rateText: String = "",
    val freightText: String = "",
    override val driverId: Long? = null,
    val reasonId: Long? = null,
    override val notes: String = "",
) : EntryDraft {
    override val type: EntryType get() = EntryType.Route

    val end: LocalDateTime get() = endDate.atTime(endTime)
    val rate: UnitPrice? get() = parseUnitPrice(rateText)
    val freightValue: Money? get() = parseMoney(freightText)?.takeIf { it.minor > 0 }

    /** Модель маршрута для подсчёта итога прямо в форме; null — одометры ещё не введены. */
    private fun preview(): Route? {
        val startOdometer = parseWhole(startOdometerText) ?: return null
        val endOdometer = parseWhole(endOdometerText) ?: return null
        return Route(
            id = id,
            vehicleId = vehicleId,
            origin = origin.trim(),
            start = dateTime,
            startOdometer = startOdometer,
            destination = destination.trim(),
            end = end,
            endOdometer = endOdometer,
            kind = kind,
            rate = if (kind == RouteKind.Trip) rate else null,
            freightValue = if (kind == RouteKind.Freight) freightValue else null,
            driverId = driverId,
            reasonId = reasonId,
            notes = notes.trim(),
        )
    }

    val value: Money get() = preview()?.takeIf { it.distance >= 0 }?.value ?: Money.ZERO

    override fun errors(bounds: BoundsAt): Map<DraftField, FieldError> = buildMap {
        odometerError(startOdometerText, required = true, bounds.at(dateTime))?.let { put(DraftField.StartOdometer, it) }
        val endError = odometerError(endOdometerText, required = true, bounds.at(end))
        val startValue = parseWhole(startOdometerText)
        val endValue = parseWhole(endOdometerText)
        when {
            endError != null -> put(DraftField.EndOdometer, endError)
            startValue != null && endValue != null && endValue < startValue -> put(DraftField.EndOdometer, FieldError.EndBeforeStart)
        }
        if (end < dateTime) put(DraftField.End, FieldError.EndBeforeStart)
        if (kind == RouteKind.Freight) positiveError(freightText, freightValue)?.let { put(DraftField.FreightValue, it) }
    }

    override fun toEntry(bounds: BoundsAt): Route? = if (errors(bounds).isEmpty()) preview() else null

    override fun withVehicle(vehicleId: Long) = copy(vehicleId = vehicleId)

    /** Начало сдвигается вместе с концом: длительность поездки сохраняется. */
    override fun withDate(date: LocalDate) = shiftedStart(date.atTime(time))

    override fun withTime(time: LocalTime) = shiftedStart(date.atTime(time))

    fun withEndDate(date: LocalDate) = copy(endDate = date)

    fun withEndTime(time: LocalTime) = copy(endTime = time)

    private fun shiftedStart(start: LocalDateTime): RouteDraft {
        val duration = Duration.between(dateTime, end).takeIf { !it.isNegative } ?: Duration.ZERO
        val newEnd = start.plus(duration)
        return copy(date = start.toLocalDate(), time = start.toLocalTime(), endDate = newEnd.toLocalDate(), endTime = newEnd.toLocalTime())
    }

    override fun withDriver(driverId: Long?) = copy(driverId = driverId)
    override fun withNotes(notes: String) = copy(notes = notes)
}

data class ReadingDraft(
    override val id: Long = 0,
    override val vehicleId: Long,
    override val date: LocalDate,
    override val time: LocalTime,
    val odometerText: String = "",
    override val driverId: Long? = null,
    override val notes: String = "",
) : EntryDraft {
    override val type: EntryType get() = EntryType.Reading

    override fun errors(bounds: BoundsAt): Map<DraftField, FieldError> = buildMap {
        odometerError(odometerText, required = true, bounds.at(dateTime))?.let { put(DraftField.Odometer, it) }
    }

    override fun toEntry(bounds: BoundsAt): Reading? {
        if (errors(bounds).isNotEmpty()) return null
        return Reading(
            id = id,
            vehicleId = vehicleId,
            dateTime = dateTime,
            odometer = parseWhole(odometerText) ?: return null,
            driverId = driverId,
            notes = notes.trim(),
        )
    }

    override fun withVehicle(vehicleId: Long) = copy(vehicleId = vehicleId)
    override fun withDate(date: LocalDate) = copy(date = date)
    override fun withTime(time: LocalTime) = copy(time = time)
    override fun withDriver(driverId: Long?) = copy(driverId = driverId)
    override fun withNotes(notes: String) = copy(notes = notes)
}

/** Форма правки существующей записи. */
fun Entry.toDraft(): EntryDraft = when (this) {
    is Refueling -> RefuelingDraft(
        id = id,
        vehicleId = vehicleId,
        date = dateTime.toLocalDate(),
        time = dateTime.toLocalTime(),
        odometerText = odometer.toString(),
        fuelId = fuelId,
        priceText = unitPrice.toInputText(),
        totalText = totalCost.toInputText(),
        volumeText = volume.toInputText(),
        // При правке суммы пересчитывается цена, при правке цены или литров — сумма.
        typedPriceFields = listOf(PriceField.UnitPrice, PriceField.Volume),
        fullTank = fullTank,
        missedPrevious = missedPrevious,
        placeId = placeId,
        driverId = driverId,
        paymentMethodId = paymentMethodId,
        reasonId = reasonId,
        notes = notes,
    )
    is Expense -> itemizedDraft(this)
    is Service -> itemizedDraft(this)
    is Income -> IncomeDraft(
        id = id,
        vehicleId = vehicleId,
        date = dateTime.toLocalDate(),
        time = dateTime.toLocalTime(),
        odometerText = odometer?.toString().orEmpty(),
        title = title,
        typeId = typeId,
        amountText = amount.toInputText(),
        driverId = driverId,
        reasonId = reasonId,
        notes = notes,
    )
    is Route -> RouteDraft(
        id = id,
        vehicleId = vehicleId,
        origin = origin,
        date = start.toLocalDate(),
        time = start.toLocalTime(),
        startOdometerText = startOdometer.toString(),
        destination = destination,
        endDate = end.toLocalDate(),
        endTime = end.toLocalTime(),
        endOdometerText = endOdometer.toString(),
        kind = kind,
        rateText = rate?.toInputText().orEmpty(),
        freightText = freightValue?.toInputText().orEmpty(),
        driverId = driverId,
        reasonId = reasonId,
        notes = notes,
    )
    is Reading -> ReadingDraft(
        id = id,
        vehicleId = vehicleId,
        date = dateTime.toLocalDate(),
        time = dateTime.toLocalTime(),
        odometerText = odometer.toString(),
        driverId = driverId,
        notes = notes,
    )
}

private fun itemizedDraft(entry: com.artemkhateev.carlog.data.model.ItemizedEntry) = ItemizedDraft(
    type = entry.type,
    id = entry.id,
    vehicleId = entry.vehicleId,
    date = entry.dateTime.toLocalDate(),
    time = entry.dateTime.toLocalTime(),
    odometerText = entry.odometer?.toString().orEmpty(),
    title = entry.title,
    items = entry.items.map { ItemDraft(it.typeId, it.amount.toInputText()) },
    discountText = if (entry.discount == Money.ZERO) "" else entry.discount.toInputText(),
    placeId = entry.placeId,
    driverId = entry.driverId,
    paymentMethodId = entry.paymentMethodId,
    reasonId = entry.reasonId,
    notes = entry.notes,
)

/**
 * Новая запись. Как у референса, форма заправки заполняется по прошлой заправке — топливо, цена, заправка,
 * оплата, — а водитель берётся из последней записи с водителем. [entries] — записи машины, новые сверху.
 */
fun newDraft(type: EntryType, vehicle: Vehicle, entries: List<Entry>, now: LocalDateTime): EntryDraft {
    val start = now.truncatedTo(ChronoUnit.MINUTES)
    val date = start.toLocalDate()
    val time = start.toLocalTime()
    val lastDriver = entries.firstNotNullOfOrNull { it.driverId }
    return when (type) {
        EntryType.Refueling -> {
            val last = entries.filterIsInstance<Refueling>().firstOrNull()
            RefuelingDraft(
                vehicleId = vehicle.id,
                date = date,
                time = time,
                fuelId = last?.fuelId ?: vehicle.fuelId,
                priceText = last?.unitPrice?.toInputText().orEmpty(),
                typedPriceFields = if (last != null) listOf(PriceField.UnitPrice) else emptyList(),
                placeId = last?.placeId,
                driverId = lastDriver,
                paymentMethodId = last?.paymentMethodId,
            )
        }
        EntryType.Expense, EntryType.Service -> ItemizedDraft(type = type, vehicleId = vehicle.id, date = date, time = time, driverId = lastDriver)
        EntryType.Income -> IncomeDraft(vehicleId = vehicle.id, date = date, time = time, driverId = lastDriver)
        EntryType.Route -> {
            val end = start.plusHours(1)
            RouteDraft(
                vehicleId = vehicle.id,
                date = date,
                time = time,
                // Поездка начинается там, где машина сейчас.
                startOdometerText = lastOdometer(entries)?.toString().orEmpty(),
                endDate = end.toLocalDate(),
                endTime = end.toLocalTime(),
                driverId = lastDriver,
            )
        }
        EntryType.Reading -> ReadingDraft(vehicleId = vehicle.id, date = date, time = time, driverId = lastDriver)
    }
}

private fun odometerError(text: String, required: Boolean, bounds: OdometerBounds): FieldError? {
    if (text.isBlank()) return if (required) FieldError.Required else null
    val value = parseWhole(text) ?: return FieldError.Required
    return when (val violation = bounds.violation(value)) {
        is OdometerViolation.BelowPrevious -> FieldError.BelowPrevious(violation.previous)
        is OdometerViolation.AboveNext -> FieldError.AboveNext(violation.next)
        null -> null
    }
}

/** Ввод уже отфильтрован до числа, поэтому непустой текст без значения — это ноль. */
private fun positiveError(text: String, parsed: Any?): FieldError? = when {
    text.isBlank() -> FieldError.Required
    parsed == null -> FieldError.NotPositive
    else -> null
}

/** Деление с округлением половины вверх; числа неотрицательные. */
private fun divideRounded(numerator: Long, denominator: Long): Long = (numerator * 2 + denominator) / (denominator * 2)

/** Центы и тысячные доли валюты за литр → тысячные литра. */
internal fun volumeFor(total: Money, price: UnitPrice) = Volume(divideRounded(total.minor * 10_000, price.milli))

internal fun totalFor(price: UnitPrice, volume: Volume) = Money(divideRounded(price.milli * volume.milli, 10_000))

internal fun priceFor(total: Money, volume: Volume) = UnitPrice(divideRounded(total.minor * 10_000, volume.milli))
