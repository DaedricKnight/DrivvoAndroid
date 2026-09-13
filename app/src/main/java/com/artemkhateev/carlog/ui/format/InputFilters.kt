package com.artemkhateev.carlog.ui.format

import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Volume
import java.math.BigDecimal

private const val MAX_INTEGER_DIGITS = 9

/** Фильтр ввода числа: цифры, один разделитель (всегда точка) и не больше [maxDecimals] знаков после него. */
fun sanitizeDecimalInput(raw: String, maxDecimals: Int): String {
    val result = StringBuilder()
    var separatorSeen = false
    var decimals = 0
    for (ch in raw) {
        when {
            ch in '0'..'9' && !separatorSeen -> if (result.length < MAX_INTEGER_DIGITS) result.append(ch)
            ch in '0'..'9' && decimals < maxDecimals -> {
                result.append(ch)
                decimals++
            }
            (ch == '.' || ch == ',') && !separatorSeen && maxDecimals > 0 -> {
                if (result.isEmpty()) result.append('0')
                result.append('.')
                separatorSeen = true
            }
        }
    }
    return result.toString()
}

/** Фильтр ввода целого: одометр, год. */
fun sanitizeWholeInput(raw: String, maxDigits: Int = MAX_INTEGER_DIGITS): String = raw.filter { it in '0'..'9' }.take(maxDigits)

/** "12", "12.5", "12,50" → число в долях 10^-[scale]. Всё, что не похоже на число, — null. */
private fun parseScaled(text: String, scale: Int): Long? {
    val trimmed = text.trim()
    if (!Regex("""\d{1,$MAX_INTEGER_DIGITS}([.,]\d{0,$scale})?""").matches(trimmed)) return null
    return BigDecimal(trimmed.replace(',', '.')).movePointRight(scale).toLong()
}

fun parseMoney(text: String): Money? = parseScaled(text, 2)?.let(::Money)

fun parseVolume(text: String): Volume? = parseScaled(text, 3)?.let(::Volume)

fun parseUnitPrice(text: String): UnitPrice? = parseScaled(text, 3)?.let(::UnitPrice)

fun parseWhole(text: String): Long? = text.trim().takeIf { Regex("""\d{1,$MAX_INTEGER_DIGITS}""").matches(it) }?.toLong()

/** Значения для поля ввода — без лишних нулей: 1200.00 → "1200", 45.90 → "45.9". */
fun Money.toInputText(): String = BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()

fun Volume.toInputText(): String = BigDecimal.valueOf(milli, 3).stripTrailingZeros().toPlainString()

fun UnitPrice.toInputText(): String = BigDecimal.valueOf(milli, 3).stripTrailingZeros().toPlainString()
