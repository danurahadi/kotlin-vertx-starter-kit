@file:Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")

package com.starter.library.extension

import id.yoframework.core.extension.logger.logger
import id.yoframework.web.exception.BadRequestException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 */

private val log = logger("DateTime_Extension")
typealias MonthNumber = Int

fun MonthNumber.mapToRoman(): String {
    val romanValues = arrayOf(
        "I",
        "II",
        "III",
        "IV",
        "V",
        "VI",
        "VII",
        "VIII",
        "IX",
        "X",
        "XI",
        "XII"
    )
    return romanValues[this - 1]
}

fun getDatesBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    val numOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate)

    return IntStream.iterate(0) { i -> i + 1 }
        .limit(numOfDaysBetween + 1)
        .mapToObj { i -> startDate.plusDays(i.toLong()) }
        .collect(Collectors.toList())
}

fun getDateList(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    return startDate.datesUntil(endDate)
        .collect(Collectors.toList())
}

@Throws(DateTimeParseException::class)
fun String.toLocalTime(pattern: String): LocalTime {
    return LocalTime.parse(this, DateTimeFormatter.ofPattern(pattern))
}

fun String.toLocalTime(withMilliseconds: Boolean = false): LocalTime {
    val splittedTime = this.split(":")

    return if (withMilliseconds) {
        if (splittedTime.size != 3) {
            throw BadRequestException("Invalid time format.")
        } else {
            val splittedSeconds = splittedTime[2].split(".")
            if (splittedSeconds.size != 2) throw BadRequestException("Invalid time format.")

            LocalTime.of(
                splittedTime[0].toInt(),
                splittedTime[1].toInt(),
                splittedSeconds[0].toInt(),
                splittedSeconds[1].toInt()
            )
        }
    } else {
        when (splittedTime.size) {
            3 -> {
                LocalTime.of(
                    splittedTime[0].toInt(),
                    splittedTime[1].toInt(),
                    splittedTime[2].toInt()
                )
            }
            2 -> {
                LocalTime.of(
                    splittedTime[0].toInt(),
                    splittedTime[1].toInt()
                )
            }
            else -> throw BadRequestException("Invalid time format.")
        }
    }
}

fun String.toIDFormat(isoFormat: Boolean = true, withTime: Boolean = true): String {

    val datetime = if (isoFormat) this.split("T") else this.split(" ")

    return try {

        val date = datetime[0].split("-")

        val months = arrayOf(
            "Januari",
            "Februari",
            "Maret",
            "April",
            "Mei",
            "Juni",
            "Juli",
            "Agustus",
            "September",
            "Oktober",
            "November",
            "Desember"
        )
        val monthNumber = date[1].toInt()
        val monthName = months[monthNumber - 1]

        val formattedDate = "${date[2]} $monthName ${date[0]}"
        val formattedTime = if (isoFormat
        ) {
            val time = datetime[1].split(":")
            val second = time.last().split(".")
            "${time[0]}:${time[1]}:${second[0]}"
        } else {
            datetime[1]
        }

        if (withTime) "$formattedDate $formattedTime" else formattedDate

    } catch (e: Exception) {
        log.error("Exception when format $this to ID format", e)
        return ""
    }

}

fun String.toENFormat(isoFormat: Boolean = true, withTime: Boolean = true): String {

    val datetime = if (isoFormat) this.split("T") else this.split(" ")

    return try {

        val date = datetime[0].split("-")

        val months = arrayOf(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"
        )
        val monthNumber = date[1].toInt()
        val monthName = months[monthNumber - 1]

        val d = LocalDate.of(
            date[0].toInt(),
            monthNumber,
            date[2].toInt()
        )

        val day = d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

        val formattedDate = "$day, $monthName ${date[2]}, ${date[0]}"
        val formattedTime = if (isoFormat
        ) {
            val time = datetime[1].split(":")
            val second = time.last().split(".")
            "${time[0]}:${time[1]}:${second[0]}"
        } else {
            datetime[1]
        }

        if (withTime) "$formattedDate $formattedTime" else formattedDate

    } catch (e: Exception) {
        log.error("Exception when format $this to EN format", e)
        return ""
    }

}

fun LocalDate.toENFormat(): String {

    return try {

        val year = this.year
        val month = this.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

        val date = this.dayOfMonth
        val day = this.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

        val formattedDate = if (year == LocalDate.now().year) {
            "$day, $month $date"
        } else {
            "$day, $month $date, $year"
        }

        formattedDate
    } catch (e: Exception) {
        log.error("Exception when format $this to EN format", e)
        return ""
    }

}

fun LocalDate.toChatDateFormat(): String {
    return when (this) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> this.toENFormat()
    }
}

fun LocalDateTime.toChatDateTimeFormat(): String {
    return when (this.toLocalDate()) {
        LocalDate.now() -> {
            val timeHour = this.hour
            val timeMinute = this.minute

            val formattedMinute = if (timeMinute < 10) {
                "0$timeMinute"
            } else {
                "$timeMinute"
            }

            "$timeHour:$formattedMinute"
        }
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> "${this.monthValue}/${this.dayOfMonth}/${this.year}"
    }
}
