package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Volume
import com.artemkhateev.carlog.data.model.sum

/**
 * Отрезок между двумя полными баками. Всё, что залили после первого бака и до второго включительно, —
 * ровно то топливо, которое сожгли на этом отрезке. Неполные заправки между ними расход не считают,
 * а только добавляют объём.
 */
data class FuelSegment(
    val start: Refueling,
    val end: Refueling,
    /** Заправки, у которых в истории показывается расход отрезка: первый полный бак и неполные после него. */
    val refuelingIds: List<Long>,
    val distance: Long,
    val volume: Volume,
) {
    /** Объём на 100 единиц пути — в тех единицах, в которых ведётся учёт. */
    val volumePer100: Double get() = volume.toDouble() / distance * 100
}

/** Отрезки расхода по заправкам одной машины в порядке пробега. */
fun fuelSegments(refuelings: List<Refueling>): List<FuelSegment> {
    val sorted = refuelings.sortedWith(compareBy({ it.odometer }, { it.dateTime }))
    val segments = mutableListOf<FuelSegment>()
    var start: Refueling? = null
    val afterStart = mutableListOf<Refueling>()
    for (refueling in sorted) {
        val open = start
        if (open == null || refueling.missedPrevious) {
            // Считать не от чего: отрезок начнётся с ближайшего полного бака.
            start = refueling.takeIf { it.fullTank }
            afterStart.clear()
            continue
        }
        afterStart += refueling
        if (!refueling.fullTank) continue
        val distance = refueling.odometer - open.odometer
        // Два полных бака на одном пробеге (долили до верха) отрезка не дают.
        if (distance > 0) {
            segments += FuelSegment(
                start = open,
                end = refueling,
                refuelingIds = listOf(open.id) + afterStart.dropLast(1).map { it.id },
                distance = distance,
                volume = afterStart.map { it.volume }.sum(),
            )
        }
        start = refueling
        afterStart.clear()
    }
    return segments
}

/** Расход у заправки в истории — расход отрезка, на котором сожгли залитое ею топливо. */
fun consumptionByRefuelingId(segments: List<FuelSegment>): Map<Long, Double> = buildMap {
    for (segment in segments) {
        for (id in segment.refuelingIds) put(id, segment.volumePer100)
    }
}

data class FuelEconomy(
    /** Средний расход: весь объём на весь путь, а не среднее отрезков — длинный отрезок весит больше. */
    val average: Double,
    val best: Double,
    val worst: Double,
)

fun fuelEconomy(segments: List<FuelSegment>): FuelEconomy? {
    val distance = segments.sumOf { it.distance }
    if (distance <= 0) return null
    val volume = segments.map { it.volume }.sum()
    return FuelEconomy(
        average = volume.toDouble() / distance * 100,
        best = segments.minOf { it.volumePer100 },
        worst = segments.maxOf { it.volumePer100 },
    )
}
