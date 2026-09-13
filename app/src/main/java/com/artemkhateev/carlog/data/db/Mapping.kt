package com.artemkhateev.carlog.data.db

import com.artemkhateev.carlog.data.model.CatalogItem
import com.artemkhateev.carlog.data.model.CostItem
import com.artemkhateev.carlog.data.model.Expense
import com.artemkhateev.carlog.data.model.Fuel
import com.artemkhateev.carlog.data.model.Income
import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.Place
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Reminder
import com.artemkhateev.carlog.data.model.Route
import com.artemkhateev.carlog.data.model.Service
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.data.model.Volume
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Записи ведутся с точностью до минуты; секунды в базу не пишем, чтобы строки сравнивались как даты. */
private val DB_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

fun LocalDateTime.toDbString(): String = format(DB_DATE_TIME)

fun String.toDbDateTime(): LocalDateTime = LocalDateTime.parse(this, DB_DATE_TIME)

fun VehicleEntity.toModel() = Vehicle(
    id = id,
    name = name,
    make = make,
    model = model,
    year = year,
    plate = plate,
    tankCapacity = tankCapacityMilli?.let(::Volume),
    fuelId = fuelId,
    colorIndex = colorIndex,
    active = active,
    notes = notes,
)

fun Vehicle.toEntity() = VehicleEntity(
    id = id,
    name = name,
    make = make,
    model = model,
    year = year,
    plate = plate,
    tankCapacityMilli = tankCapacity?.milli,
    fuelId = fuelId,
    colorIndex = colorIndex,
    active = active,
    notes = notes,
)

fun FuelEntity.toModel() = Fuel(id = id, name = name, category = category)

fun Fuel.toEntity() = FuelEntity(id = id, name = name, category = category)

fun PlaceEntity.toModel() = Place(id = id, name = name, kind = kind, address = address)

fun Place.toEntity() = PlaceEntity(id = id, name = name, kind = kind, address = address)

fun CatalogItemEntity.toModel() = CatalogItem(id = id, kind = kind, name = name)

fun CatalogItem.toEntity() = CatalogItemEntity(id = id, kind = kind, name = name)

fun RefuelingEntity.toModel() = Refueling(
    id = id,
    vehicleId = vehicleId,
    dateTime = dateTime.toDbDateTime(),
    odometer = odometer,
    fuelId = fuelId,
    unitPrice = UnitPrice(unitPriceMilli),
    totalCost = Money(totalCostMinor),
    volume = Volume(volumeMilli),
    fullTank = fullTank,
    missedPrevious = missedPrevious,
    placeId = placeId,
    driverId = driverId,
    paymentMethodId = paymentMethodId,
    reasonId = reasonId,
    notes = notes,
)

fun Refueling.toEntity() = RefuelingEntity(
    id = id,
    vehicleId = vehicleId,
    dateTime = dateTime.toDbString(),
    odometer = odometer,
    fuelId = fuelId,
    unitPriceMilli = unitPrice.milli,
    totalCostMinor = totalCost.minor,
    volumeMilli = volume.milli,
    fullTank = fullTank,
    missedPrevious = missedPrevious,
    placeId = placeId,
    driverId = driverId,
    paymentMethodId = paymentMethodId,
    reasonId = reasonId,
    notes = notes,
)

fun ExpenseWithItems.toModel() = Expense(
    id = expense.id,
    vehicleId = expense.vehicleId,
    dateTime = expense.dateTime.toDbDateTime(),
    odometer = expense.odometer,
    title = expense.title,
    items = items.sortedBy { it.id }.map { CostItem(typeId = it.typeId, amount = Money(it.amountMinor)) },
    discount = Money(expense.discountMinor),
    placeId = expense.placeId,
    driverId = expense.driverId,
    paymentMethodId = expense.paymentMethodId,
    reasonId = expense.reasonId,
    notes = expense.notes,
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    vehicleId = vehicleId,
    dateTime = dateTime.toDbString(),
    odometer = odometer,
    title = title,
    discountMinor = discount.minor,
    placeId = placeId,
    driverId = driverId,
    paymentMethodId = paymentMethodId,
    reasonId = reasonId,
    notes = notes,
)

fun Expense.itemEntities() = items.map { ExpenseItemEntity(expenseId = id, typeId = it.typeId, amountMinor = it.amount.minor) }

fun ServiceWithItems.toModel() = Service(
    id = service.id,
    vehicleId = service.vehicleId,
    dateTime = service.dateTime.toDbDateTime(),
    odometer = service.odometer,
    title = service.title,
    items = items.sortedBy { it.id }.map { CostItem(typeId = it.typeId, amount = Money(it.amountMinor)) },
    discount = Money(service.discountMinor),
    placeId = service.placeId,
    driverId = service.driverId,
    paymentMethodId = service.paymentMethodId,
    reasonId = service.reasonId,
    notes = service.notes,
)

fun Service.toEntity() = ServiceEntity(
    id = id,
    vehicleId = vehicleId,
    dateTime = dateTime.toDbString(),
    odometer = odometer,
    title = title,
    discountMinor = discount.minor,
    placeId = placeId,
    driverId = driverId,
    paymentMethodId = paymentMethodId,
    reasonId = reasonId,
    notes = notes,
)

fun Service.itemEntities() = items.map { ServiceItemEntity(serviceId = id, typeId = it.typeId, amountMinor = it.amount.minor) }

fun IncomeEntity.toModel() = Income(
    id = id,
    vehicleId = vehicleId,
    dateTime = dateTime.toDbDateTime(),
    odometer = odometer,
    title = title,
    typeId = typeId,
    amount = Money(amountMinor),
    driverId = driverId,
    reasonId = reasonId,
    notes = notes,
)

fun Income.toEntity() = IncomeEntity(
    id = id,
    vehicleId = vehicleId,
    dateTime = dateTime.toDbString(),
    odometer = odometer,
    title = title,
    typeId = typeId,
    amountMinor = amount.minor,
    driverId = driverId,
    reasonId = reasonId,
    notes = notes,
)

fun RouteEntity.toModel() = Route(
    id = id,
    vehicleId = vehicleId,
    origin = origin,
    start = startDateTime.toDbDateTime(),
    startOdometer = startOdometer,
    destination = destination,
    end = endDateTime.toDbDateTime(),
    endOdometer = endOdometer,
    kind = kind,
    rate = rateMilli?.let(::UnitPrice),
    freightValue = freightValueMinor?.let(::Money),
    driverId = driverId,
    reasonId = reasonId,
    notes = notes,
)

fun Route.toEntity() = RouteEntity(
    id = id,
    vehicleId = vehicleId,
    origin = origin,
    startDateTime = start.toDbString(),
    startOdometer = startOdometer,
    destination = destination,
    endDateTime = end.toDbString(),
    endOdometer = endOdometer,
    kind = kind,
    rateMilli = rate?.milli,
    freightValueMinor = freightValue?.minor,
    driverId = driverId,
    reasonId = reasonId,
    notes = notes,
)

fun ReadingEntity.toModel() = Reading(
    id = id,
    vehicleId = vehicleId,
    dateTime = dateTime.toDbDateTime(),
    odometer = odometer,
    driverId = driverId,
    notes = notes,
)

fun Reading.toEntity() = ReadingEntity(
    id = id,
    vehicleId = vehicleId,
    dateTime = dateTime.toDbString(),
    odometer = odometer,
    driverId = driverId,
    notes = notes,
)

fun ReminderEntity.toModel() = Reminder(
    id = id,
    vehicleId = vehicleId,
    kind = kind,
    typeId = typeId,
    title = title,
    dueOdometer = dueOdometer,
    dueDate = dueDate?.let(LocalDate::parse),
    repeatDistance = repeatDistance,
    repeatMonths = repeatMonths,
    notes = notes,
    notifiedKey = notifiedKey,
)

fun Reminder.toEntity() = ReminderEntity(
    id = id,
    vehicleId = vehicleId,
    kind = kind,
    typeId = typeId,
    title = title,
    dueOdometer = dueOdometer,
    dueDate = dueDate?.toString(),
    repeatDistance = repeatDistance,
    repeatMonths = repeatMonths,
    notes = notes,
    notifiedKey = notifiedKey,
)
