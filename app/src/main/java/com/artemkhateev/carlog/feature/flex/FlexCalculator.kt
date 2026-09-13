package com.artemkhateev.carlog.feature.flex

import com.artemkhateev.carlog.data.settings.ConsumptionFormat
import kotlin.math.abs

enum class CheaperFuel { First, Second, Same }

data class FlexResult(
    /** Цена второго топлива в процентах от первого. */
    val priceRatioPercent: Double,
    val firstCostPerDistance: Double?,
    val secondCostPerDistance: Double?,
    val cheaper: CheaperFuel,
)

/** Привычное правило для этанола: выгоден, пока стоит меньше 70% бензина — на столько больше его уходит. */
const val BREAK_EVEN_PERCENT = 70.0

/**
 * Какое топливо выгоднее. С расходом обоих — по цене единицы пути, без него — по правилу 70%.
 * Расход вводится в формате из настроек: литры на 100 км или километры на литр.
 */
fun compareFuels(
    firstPrice: Double,
    secondPrice: Double,
    firstEfficiency: Double?,
    secondEfficiency: Double?,
    format: ConsumptionFormat,
): FlexResult? {
    if (firstPrice <= 0 || secondPrice <= 0) return null
    val firstCost = firstEfficiency?.takeIf { it > 0 }?.let { costPerDistance(firstPrice, it, format) }
    val secondCost = secondEfficiency?.takeIf { it > 0 }?.let { costPerDistance(secondPrice, it, format) }
    val ratio = secondPrice / firstPrice * 100
    val cheaper = if (firstCost != null && secondCost != null) {
        cheaperOf(firstCost, secondCost)
    } else {
        cheaperOf(BREAK_EVEN_PERCENT, ratio)
    }
    return FlexResult(ratio, firstCost, secondCost, cheaper)
}

private fun costPerDistance(price: Double, efficiency: Double, format: ConsumptionFormat): Double = when (format) {
    ConsumptionFormat.VolumePer100 -> price * efficiency / 100
    ConsumptionFormat.DistancePerVolume -> price / efficiency
}

private fun cheaperOf(first: Double, second: Double): CheaperFuel = when {
    abs(first - second) < 1e-9 -> CheaperFuel.Same
    second < first -> CheaperFuel.Second
    else -> CheaperFuel.First
}
