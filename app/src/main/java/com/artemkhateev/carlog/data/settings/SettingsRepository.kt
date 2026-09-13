package com.artemkhateev.carlog.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Currency
import java.util.Locale

enum class DistanceUnit { Kilometer, Mile }

enum class VolumeUnit { Liter, UsGallon, ImperialGallon }

/** Как показывать расход: литры на 100 км или километры на литр (мили на галлон). */
enum class ConsumptionFormat { VolumePer100, DistancePerVolume }

enum class ThemeMode { System, Dark, Light }

/**
 * Единицы — только подписи: значения хранятся так, как их ввели, и при смене единиц не пересчитываются.
 */
data class AppSettings(
    val selectedVehicleId: Long? = null,
    val currencyCode: String = "EUR",
    val distanceUnit: DistanceUnit = DistanceUnit.Kilometer,
    val volumeUnit: VolumeUnit = VolumeUnit.Liter,
    val consumptionFormat: ConsumptionFormat = ConsumptionFormat.VolumePer100,
    val themeMode: ThemeMode = ThemeMode.System,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val selectedVehicle = longPreferencesKey("selected_vehicle")
        val currency = stringPreferencesKey("currency")
        val distanceUnit = stringPreferencesKey("distance_unit")
        val volumeUnit = stringPreferencesKey("volume_unit")
        val consumptionFormat = stringPreferencesKey("consumption_format")
        val themeMode = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            selectedVehicleId = prefs[Keys.selectedVehicle],
            currencyCode = prefs[Keys.currency] ?: defaultCurrencyCode(),
            distanceUnit = prefs[Keys.distanceUnit].toEnum(DistanceUnit.Kilometer),
            volumeUnit = prefs[Keys.volumeUnit].toEnum(VolumeUnit.Liter),
            consumptionFormat = prefs[Keys.consumptionFormat].toEnum(ConsumptionFormat.VolumePer100),
            themeMode = prefs[Keys.themeMode].toEnum(ThemeMode.System),
        )
    }

    suspend fun selectVehicle(id: Long) = set(Keys.selectedVehicle, id)

    suspend fun setCurrency(code: String) = set(Keys.currency, code)

    suspend fun setDistanceUnit(unit: DistanceUnit) = set(Keys.distanceUnit, unit.name)

    suspend fun setVolumeUnit(unit: VolumeUnit) = set(Keys.volumeUnit, unit.name)

    suspend fun setConsumptionFormat(format: ConsumptionFormat) = set(Keys.consumptionFormat, format.name)

    suspend fun setThemeMode(mode: ThemeMode) = set(Keys.themeMode, mode.name)

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    companion object {
        /** Валюта страны телефона; у языка без страны её нет — тогда евро. */
        fun defaultCurrencyCode(locale: Locale = Locale.getDefault()): String =
            runCatching { Currency.getInstance(locale).currencyCode }.getOrNull() ?: "EUR"
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
