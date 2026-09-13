package com.artemkhateev.carlog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Кусок столбца. */
data class BarSegment(val value: Double, val color: Color)

/** Столбцы одного месяца: каждый — стопка кусков (расходы по типам), рядом может стоять второй (доход). */
data class BarGroup(val label: String, val bars: List<List<BarSegment>>)

data class DonutSlice(val label: String, val value: Double, val color: Color)

data class LinePoint(val label: String, val value: Double)

/** Круглый шаг шкалы: 1, 2, 2.5 или 5 на степень десяти — чтобы подписи оси были ровными. */
internal fun niceStep(rough: Double): Double {
    if (rough <= 0.0 || rough.isNaN()) return 1.0
    val magnitude = 10.0.pow(floor(log10(rough)))
    val fraction = rough / magnitude
    val nice = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 2.5 -> 2.5
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * magnitude
}

@Composable
fun BarChart(groups: List<BarGroup>, axisLabel: (Double) -> String, modifier: Modifier = Modifier) {
    val colors = CarLogTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = CarLogTheme.typography.caption.copy(color = colors.textSecondary)
    val max = groups.flatMap { it.bars }.maxOfOrNull { bar -> bar.sumOf { it.value } } ?: 0.0
    val step = niceStep(max / 4)
    val top = if (max <= 0.0) step * 4 else ceil(max / step) * step
    Canvas(
        modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        val area = drawValueAxis(measurer, labelStyle, colors.divider, low = 0.0, high = top, step = step, axisLabel = axisLabel)
        if (groups.isEmpty()) return@Canvas
        val slot = (size.width - area.left) / groups.size
        val labelEvery = ceil((measurer.measure(groups.first().label, labelStyle).size.width + 8.dp.toPx()) / slot).toInt().coerceAtLeast(1)
        groups.forEachIndexed { index, group ->
            val slotLeft = area.left + slot * index
            val count = group.bars.size.coerceAtLeast(1)
            val gap = 4.dp.toPx()
            val barWidth = min((slot * 0.7f - gap * (count - 1)) / count, 56.dp.toPx()).coerceAtLeast(1f)
            var barLeft = slotLeft + (slot - (barWidth * count + gap * (count - 1))) / 2
            for (segments in group.bars) {
                var bottom = area.bottom
                for (segment in segments) {
                    if (segment.value <= 0.0) continue
                    val height = (segment.value / top * (area.bottom - area.top)).toFloat()
                    drawRect(segment.color, topLeft = Offset(barLeft, bottom - height), size = Size(barWidth, height))
                    bottom -= height
                }
                barLeft += barWidth + gap
            }
            if (index % labelEvery == 0) {
                val layout = measurer.measure(group.label, labelStyle)
                drawText(layout, topLeft = Offset(slotLeft + slot / 2 - layout.size.width / 2f, area.bottom + 4.dp.toPx()))
            }
        }
    }
}

/** Линия по точкам через равные промежутки — расход по отрезкам, цена по заправкам. */
@Composable
fun LineChart(points: List<LinePoint>, color: Color, axisLabel: (Double) -> String, modifier: Modifier = Modifier) {
    val colors = CarLogTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = CarLogTheme.typography.caption.copy(color = colors.textSecondary)
    if (points.isEmpty()) return
    val minValue = points.minOf { it.value }
    val maxValue = points.maxOf { it.value }
    val spread = maxValue - minValue
    val padding = if (spread == 0.0) max(maxValue * 0.1, 1.0) else spread * 0.15
    val step = niceStep((spread + padding * 2) / 4)
    val low = (floor((minValue - padding) / step) * step).coerceAtLeast(0.0)
    val high = ceil((maxValue + padding) / step) * step
    Canvas(
        modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        val area = drawValueAxis(measurer, labelStyle, colors.divider, low = low, high = high, step = step, axisLabel = axisLabel)
        val xStep = if (points.size > 1) (size.width - area.left - 12.dp.toPx()) / (points.size - 1) else 0f
        fun x(index: Int) = area.left + 6.dp.toPx() + xStep * index
        fun y(value: Double) = area.bottom - ((value - low) / (high - low) * (area.bottom - area.top)).toFloat()
        val path = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(x(index), y(point.value)) else path.lineTo(x(index), y(point.value))
        }
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
        points.forEachIndexed { index, point -> drawCircle(color, radius = 3.5.dp.toPx(), center = Offset(x(index), y(point.value))) }
        // Подписи по оси X — только первая и последняя: дат слишком много, чтобы подписать все.
        listOf(0, points.lastIndex).distinct().forEach { index ->
            val layout = measurer.measure(points[index].label, labelStyle)
            val left = (x(index) - layout.size.width / 2f).coerceIn(area.left, size.width - layout.size.width)
            drawText(layout, topLeft = Offset(left, area.bottom + 4.dp.toPx()))
        }
    }
}

/** Кольцо долей и под ним список: цвет, название, сумма, процент. */
@Composable
fun DonutChart(slices: List<DonutSlice>, valueText: (Double) -> String, percentText: (Double) -> String, modifier: Modifier = Modifier) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    val total = slices.sumOf { it.value }
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(180.dp)) {
                val stroke = 30.dp.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arcSize = Size(size.width - stroke, size.height - stroke)
                if (total <= 0.0) {
                    drawArc(colors.divider, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
                    return@Canvas
                }
                var start = -90f
                for (slice in slices) {
                    val sweep = (slice.value / total * 360).toFloat()
                    drawArc(slice.color, start, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
                    start += sweep
                }
            }
            Text(valueText(total), style = typography.statValue, color = colors.textPrimary)
        }
        slices.forEach { slice ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(slice.color),
                )
                Text(
                    text = slice.label,
                    style = typography.listSubtitle,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                )
                Text(valueText(slice.value), style = typography.statValue, color = colors.textPrimary)
                Text(
                    text = if (total > 0) percentText(slice.value / total * 100) else "",
                    style = typography.listSubtitle,
                    color = colors.textSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(60.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartLegend(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    val colors = CarLogTheme.colors
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Text(label, style = CarLogTheme.typography.listSubtitle, color = colors.textPrimary, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

private data class PlotArea(val left: Float, val top: Float, val bottom: Float)

/** Шкала значений слева с пунктирной сеткой; возвращает, где рисовать сами данные. */
private fun DrawScope.drawValueAxis(
    measurer: TextMeasurer,
    style: TextStyle,
    gridColor: Color,
    low: Double,
    high: Double,
    step: Double,
    axisLabel: (Double) -> String,
): PlotArea {
    val ticks = generateSequence(low) { it + step }.takeWhile { it <= high + step / 2 }.toList()
    val layouts = ticks.map { measurer.measure(axisLabel(it), style) }
    val left = (layouts.maxOfOrNull { it.size.width } ?: 0) + 10.dp.toPx()
    val labelHeight = (layouts.firstOrNull()?.size?.height ?: 0).toFloat()
    val top = labelHeight / 2
    val bottom = size.height - labelHeight - 8.dp.toPx()
    ticks.forEachIndexed { index, tick ->
        val y = bottom - ((tick - low) / (high - low) * (bottom - top)).toFloat()
        drawLine(
            color = gridColor,
            start = Offset(left, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
        val layout = layouts[index]
        drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f))
    }
    return PlotArea(left = left, top = top, bottom = bottom)
}
