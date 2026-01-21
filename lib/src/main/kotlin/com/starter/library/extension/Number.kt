package com.starter.library.extension

import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.round

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

fun Float.roundNumber(decimals: Int): Float {
    var multiplier = 1.0F
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

fun Double.roundNumber(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

fun Double.toDecimalFormat(format: String = "##########.##"): String {
    val decimalFormat = DecimalFormat(format)
    decimalFormat.roundingMode = RoundingMode.HALF_UP
    return decimalFormat.format(this)
}

fun IntRange.random(): Int {
    return ThreadLocalRandom.current()
        .nextInt((endInclusive + 1) - start) + start
}
