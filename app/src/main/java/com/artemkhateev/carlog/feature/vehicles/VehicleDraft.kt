package com.artemkhateev.carlog.feature.vehicles

import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.data.model.Volume
import com.artemkhateev.carlog.ui.format.parseVolume
import com.artemkhateev.carlog.ui.format.parseWhole
import com.artemkhateev.carlog.ui.format.toInputText

/** Форма машины. Числа хранятся текстом — так, как их набирают. */
data class VehicleDraft(
    val id: Long = 0,
    val name: String = "",
    val make: String = "",
    val model: String = "",
    val yearText: String = "",
    val plate: String = "",
    val tankText: String = "",
    val fuelId: Long? = null,
    val colorIndex: Int = 0,
    val active: Boolean = true,
    val notes: String = "",
    /** Только у новой машины: пробег на сегодня, чтобы первая запись уже знала последний одометр. */
    val odometerText: String = "",
) {
    /** Имя можно не вводить: тогда машина называется маркой и моделью. */
    val displayName: String
        get() = name.trim().ifEmpty { listOf(make.trim(), model.trim()).filter { it.isNotEmpty() }.joinToString(" ") }

    val year: Int? get() = yearText.toIntOrNull()

    val yearValid: Boolean get() = yearText.isBlank() || year?.let { it in MIN_YEAR..MAX_YEAR } == true

    val tankCapacity: Volume? get() = parseVolume(tankText)?.takeIf { it.milli > 0 }

    val tankValid: Boolean get() = tankText.isBlank() || tankCapacity != null

    val initialOdometer: Long? get() = parseWhole(odometerText)

    val isValid: Boolean get() = displayName.isNotEmpty() && yearValid && tankValid

    fun toVehicle(): Vehicle? {
        if (!isValid) return null
        return Vehicle(
            id = id,
            name = displayName,
            make = make.trim(),
            model = model.trim(),
            year = year,
            plate = plate.trim(),
            tankCapacity = tankCapacity,
            fuelId = fuelId,
            colorIndex = colorIndex,
            active = active,
            notes = notes.trim(),
        )
    }

    companion object {
        const val MIN_YEAR = 1886
        const val MAX_YEAR = 2100

        fun from(vehicle: Vehicle) = VehicleDraft(
            id = vehicle.id,
            name = vehicle.name,
            make = vehicle.make,
            model = vehicle.model,
            yearText = vehicle.year?.toString().orEmpty(),
            plate = vehicle.plate,
            tankText = vehicle.tankCapacity?.toInputText().orEmpty(),
            fuelId = vehicle.fuelId,
            colorIndex = vehicle.colorIndex,
            active = vehicle.active,
            notes = vehicle.notes,
        )
    }
}
