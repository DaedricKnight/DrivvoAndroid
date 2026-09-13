package com.artemkhateev.carlog.data.export

import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.Income
import com.artemkhateev.carlog.data.model.ItemizedEntry
import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Route
import com.artemkhateev.carlog.data.model.Vehicle
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * Выгрузка записей в CSV для таблиц: одна строка — одна запись, старые сверху.
 * Числа с точкой и без разделителей тысяч, даты ISO — так файл одинаково читается при любом языке.
 */
object CsvExport {

    val header = listOf(
        "vehicle", "type", "date", "time", "odometer", "end_odometer", "title", "types", "fuel", "volume", "unit_price",
        "full_tank", "amount", "discount", "place", "driver", "payment_method", "reason", "origin", "destination", "notes",
    )

    private val time = DateTimeFormatter.ofPattern("HH:mm")

    fun build(vehicles: List<Vehicle>, entriesByVehicle: Map<Long, List<Entry>>, catalogs: Catalogs): String {
        val rows = vehicles.flatMap { vehicle ->
            entriesByVehicle[vehicle.id].orEmpty().sortedBy { it.dateTime }.map { row(vehicle, it, catalogs) }
        }
        return (listOf(header) + rows).joinToString(separator = "\r\n", postfix = "\r\n") { cells -> cells.joinToString(",") { escape(it) } }
    }

    private fun row(vehicle: Vehicle, entry: Entry, catalogs: Catalogs): List<String> {
        val cells = header.associateWith { "" }.toMutableMap()
        cells["vehicle"] = vehicle.name
        cells["type"] = entry.type.name
        cells["date"] = entry.dateTime.toLocalDate().toString()
        cells["time"] = time.format(entry.dateTime)
        cells["odometer"] = entry.odometerReadings.firstOrNull()?.toString().orEmpty()
        cells["driver"] = catalogs.item(entry.driverId)?.name.orEmpty()
        cells["notes"] = entry.notes
        when (entry) {
            is Refueling -> {
                cells["fuel"] = catalogs.fuel(entry.fuelId)?.name.orEmpty()
                cells["volume"] = BigDecimal.valueOf(entry.volume.milli, 3).stripTrailingZeros().toPlainString()
                cells["unit_price"] = BigDecimal.valueOf(entry.unitPrice.milli, 3).stripTrailingZeros().toPlainString()
                cells["full_tank"] = if (entry.fullTank) "yes" else "no"
                cells["amount"] = plain(entry.totalCost)
                cells["place"] = catalogs.place(entry.placeId)?.name.orEmpty()
                cells["payment_method"] = catalogs.item(entry.paymentMethodId)?.name.orEmpty()
                cells["reason"] = catalogs.item(entry.reasonId)?.name.orEmpty()
            }
            is ItemizedEntry -> {
                cells["title"] = entry.title
                cells["types"] = entry.items.joinToString("; ") { item -> "${catalogs.item(item.typeId)?.name.orEmpty()} ${plain(item.amount)}" }
                cells["amount"] = plain(entry.total)
                cells["discount"] = if (entry.discount == Money.ZERO) "" else plain(entry.discount)
                cells["place"] = catalogs.place(entry.placeId)?.name.orEmpty()
                cells["payment_method"] = catalogs.item(entry.paymentMethodId)?.name.orEmpty()
                cells["reason"] = catalogs.item(entry.reasonId)?.name.orEmpty()
            }
            is Income -> {
                cells["title"] = entry.title
                cells["types"] = catalogs.item(entry.typeId)?.name.orEmpty()
                cells["amount"] = plain(entry.amount)
                cells["reason"] = catalogs.item(entry.reasonId)?.name.orEmpty()
            }
            is Route -> {
                cells["end_odometer"] = entry.endOdometer.toString()
                cells["origin"] = entry.origin
                cells["destination"] = entry.destination
                cells["amount"] = if (entry.value == Money.ZERO) "" else plain(entry.value)
                cells["reason"] = catalogs.item(entry.reasonId)?.name.orEmpty()
            }
            is Reading -> Unit
        }
        return header.map { cells.getValue(it) }
    }

    private fun plain(money: Money): String = BigDecimal.valueOf(money.minor, 2).toPlainString()

    /** Кавычки — только там, где без них строка развалится: запятая, кавычка, перевод строки. */
    internal fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
