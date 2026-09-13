package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.CostItem
import com.artemkhateev.carlog.data.model.Expense
import com.artemkhateev.carlog.data.model.Income
import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Route
import com.artemkhateev.carlog.data.model.Service
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Volume
import java.time.LocalDateTime

// Данные в тестах выдуманные: личные записи сюда не переносить.

internal fun at(month: Int, day: Int, hour: Int = 12): LocalDateTime = LocalDateTime.of(2025, month, day, hour, 0)

/** [milli] — тысячные литра, [priceMilli] — тысячные валюты за литр; стоимость считается из них. */
internal fun refueling(
    id: Long,
    odometer: Long,
    milli: Long,
    full: Boolean = true,
    missedPrevious: Boolean = false,
    dateTime: LocalDateTime = at(3, 1),
    priceMilli: Long = 1_500,
) = Refueling(
    id = id,
    vehicleId = 1,
    dateTime = dateTime,
    odometer = odometer,
    fuelId = 1,
    unitPrice = UnitPrice(priceMilli),
    totalCost = Money(milli * priceMilli / 10_000),
    volume = Volume(milli),
    fullTank = full,
    missedPrevious = missedPrevious,
)

internal fun service(id: Long, odometer: Long?, dateTime: LocalDateTime, vararg items: Pair<Long, Long>, discountCents: Long = 0) = Service(
    id = id,
    vehicleId = 1,
    dateTime = dateTime,
    odometer = odometer,
    items = items.map { (typeId, cents) -> CostItem(typeId, Money(cents)) },
    discount = Money(discountCents),
)

internal fun expense(id: Long, odometer: Long?, dateTime: LocalDateTime, vararg items: Pair<Long, Long>) = Expense(
    id = id,
    vehicleId = 1,
    dateTime = dateTime,
    odometer = odometer,
    items = items.map { (typeId, cents) -> CostItem(typeId, Money(cents)) },
)

internal fun income(id: Long, dateTime: LocalDateTime, cents: Long, typeId: Long? = null) = Income(
    id = id,
    vehicleId = 1,
    dateTime = dateTime,
    odometer = null,
    typeId = typeId,
    amount = Money(cents),
)

internal fun reading(id: Long, odometer: Long, dateTime: LocalDateTime) = Reading(
    id = id,
    vehicleId = 1,
    dateTime = dateTime,
    odometer = odometer,
)

internal fun route(id: Long, from: Long, to: Long, start: LocalDateTime, minutes: Long, rateMilli: Long? = null) = Route(
    id = id,
    vehicleId = 1,
    origin = "A",
    start = start,
    startOdometer = from,
    destination = "B",
    end = start.plusMinutes(minutes),
    endOdometer = to,
    rate = rateMilli?.let(::UnitPrice),
)
