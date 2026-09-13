package com.artemkhateev.carlog.ui.format

import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Volume
import com.artemkhateev.carlog.data.settings.AppSettings
import com.artemkhateev.carlog.data.settings.ConsumptionFormat
import com.artemkhateev.carlog.data.settings.DistanceUnit
import com.artemkhateev.carlog.data.settings.VolumeUnit
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/**
 * Все числа на экранах — через этот класс: валюта и единицы из настроек, разделители — из языка телефона.
 * Одометр и пробег без разделителей тысяч, как на приборной панели.
 */
class Formats(
    val currency: Currency,
    val distanceUnit: DistanceUnit,
    val volumeUnit: VolumeUnit,
    val consumptionFormat: ConsumptionFormat,
    private val locale: Locale = Locale.getDefault(),
) {
    val distanceLabel: String = when (distanceUnit) {
        DistanceUnit.Kilometer -> "km"
        DistanceUnit.Mile -> "mi"
    }

    val volumeLabel: String = when (volumeUnit) {
        VolumeUnit.Liter -> "L"
        VolumeUnit.UsGallon, VolumeUnit.ImperialGallon -> "gal"
    }

    val consumptionLabel: String = when (consumptionFormat) {
        ConsumptionFormat.VolumePer100 -> "$volumeLabel/100$distanceLabel"
        ConsumptionFormat.DistancePerVolume ->
            if (distanceUnit == DistanceUnit.Mile && volumeUnit != VolumeUnit.Liter) "mpg" else "$distanceLabel/$volumeLabel"
    }

    private val symbols = DecimalFormatSymbols.getInstance(locale)
    private val upTo3 = DecimalFormat("0.###", symbols)
    private val upTo1 = DecimalFormat("0.#", symbols)
    private val whole = DecimalFormat("0", symbols)

    private val moneyFormat = NumberFormat.getCurrencyInstance(locale).apply { currency = this@Formats.currency }

    /** Цена единицы — как деньги, но до трёх знаков: литр часто стоит 1.799. */
    private val unitPriceFormat = NumberFormat.getCurrencyInstance(locale).apply {
        currency = this@Formats.currency
        maximumFractionDigits = maxOf(3, this@Formats.currency.defaultFractionDigits)
    }

    private val dayMonthFormat = DateTimeFormatter.ofPattern("dd MMM", locale)
    private val fullDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    private val monthFormat = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", locale)

    fun money(value: Money): String = moneyFormat.format(BigDecimal.valueOf(value.minor, 2))

    fun money(value: Double): String = moneyFormat.format(value)

    fun unitPrice(value: UnitPrice): String = unitPriceFormat.format(BigDecimal.valueOf(value.milli, 3))

    fun unitPrice(value: Double): String = unitPriceFormat.format(value)

    fun distance(value: Long): String = "$value $distanceLabel"

    fun distance(value: Double): String = "${whole.format(value)} $distanceLabel"

    fun volume(value: Volume): String = volume(value.toDouble())

    fun volume(value: Double): String = "${upTo3.format(value)} $volumeLabel"

    /** [volumePer100] — расход в единицах учёта; показывается в формате из настроек. */
    fun consumption(volumePer100: Double): String = "${consumptionNumber(volumePer100)} $consumptionLabel"

    fun consumptionNumber(volumePer100: Double): String = upTo3.format(consumptionValue(volumePer100))

    /** Расход в формате из настроек — для графика. */
    fun consumptionValue(volumePer100: Double): Double = when (consumptionFormat) {
        ConsumptionFormat.VolumePer100 -> volumePer100
        ConsumptionFormat.DistancePerVolume -> 100 / volumePer100
    }

    /** 84.6% — доля с одним знаком после запятой. */
    fun percent(value: Double): String = "${upTo1.format(value)}%"

    /** Число без единиц: дни между заправками, проценты. */
    fun decimal(value: Double): String = upTo3.format(value)

    fun wholeNumber(value: Double): String = whole.format(value)

    /** В формах даты как в Drivvo — ISO: 2025-03-01. */
    fun date(value: LocalDate): String = value.toString()

    fun dayMonth(value: LocalDate): String = dayMonthFormat.format(value)

    fun fullDate(value: LocalDate): String = fullDateFormat.format(value)

    fun monthTitle(value: YearMonth): String = monthFormat.format(value).uppercase(locale)

    fun time(value: LocalTime): String = timeFormat.format(value)

    companion object {
        fun from(settings: AppSettings, locale: Locale = Locale.getDefault()) = Formats(
            currency = runCatching { Currency.getInstance(settings.currencyCode) }.getOrElse { Currency.getInstance("EUR") },
            distanceUnit = settings.distanceUnit,
            volumeUnit = settings.volumeUnit,
            consumptionFormat = settings.consumptionFormat,
            locale = locale,
        )
    }
}
