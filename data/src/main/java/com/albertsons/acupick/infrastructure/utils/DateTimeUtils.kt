package com.albertsons.acupick.infrastructure.utils

import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor
import java.util.Calendar
import java.util.concurrent.TimeUnit

// https://developer.android.com/reference/java/time/format/DateTimeFormatter#patterns

/**
 * Produces results that look like:
 * * `1:04AM`
 * * `6:31PM`
 */
val HOUR_MINUTE_AM_PM_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Produces results that look like:
 * * `1:04 AM`
 * * `6:31 PM`
 */
val SPACED_HOUR_MINUTE_AM_PM_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Produces results that look like:
 * * `09/22/2020 1:40:00 AM
 * * `08/01/2020 4:30:00 PM
 */
val MONTH_DAY_YEAR_HOUR_MIN_SEC_AM_PM_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a")

/**
 * Produces results that look like:
 * * `1:04`
 * * `6:31`
 */
val HOUR_MINUTE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm")

/**
 * Produces results that look like:
 * * `AM`
 * * `PM`
 */
val AM_PM_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("a")

val ZONED_DATE_TIME_EPOCH: ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.UTC)

val DOB_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyy")

val SERVER_DOB_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

val DL_DOB_FORMATTER_VARIANT_1: DateTimeFormatter = DateTimeFormatter.ofPattern("MMddyyyy")
val DL_DOB_FORMATTER_VARIANT_2: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

/**
 * Outputs useful human readable [Duration] information with output similar to:
 * ```
 * 1 years, 0 days, 1 hours, 0 minutes, 1 seconds
 *
 * 0 years, 0 days, 0 hours, 33 minutes, 20 seconds
 *
 * 0 years, 3 days, 9 hours, 31 minutes, 38 seconds
 * ```
 */
fun Duration.prettyPrint(printEmptyValues: Boolean = false): String {
    /** Don't rely on this anywhere else! Just used for human readable string approximation only! */
    fun Duration.toYearsApproximation(): Long = seconds / 31536000L

    /** Don't rely on this anywhere else! Just used for human readable string approximation only! */
    fun Duration.yearsToDaysApproximation(): Long = toYearsApproximation() * 365L

    val years = toYearsApproximation()
    val days = toDays() - yearsToDaysApproximation()
    val hours = toHours() - TimeUnit.DAYS.toHours(toDays())
    val minutes = toMinutes() - TimeUnit.HOURS.toMinutes(toHours())
    val seconds = seconds - TimeUnit.MINUTES.toSeconds(toMinutes())

    return if (!printEmptyValues && years == 0L) {
        if (days == 0L) {
            if (hours == 0L) {
                if (minutes == 0L) {
                    String.format("%s seconds", seconds)
                } else {
                    String.format("%s minutes, %s seconds", minutes, seconds)
                }
            } else {
                String.format("%s hours, %s minutes, %s seconds", hours, minutes, seconds)
            }
        } else {
            String.format("%s days, %s hours, %s minutes, %s seconds", days, hours, minutes, seconds)
        }
    } else {
        String.format("%s years, %s days, %s hours, %s minutes, %s seconds", years, days, hours, minutes, seconds)
    }
}

/** Syntax sugar to allow safe call operator `?.` chaining formatting on [LocalDateTime]/[LocalDate]/[LocalTime]/[ZonedDateTime]/etc */
fun TemporalAccessor?.formattedWith(formatter: DateTimeFormatter): String? {
    if (this == null) return null
    return formatter.format(this)
}

fun getFormattedWithoutAmPm(zonedDateTime: ZonedDateTime?): String? {
    return zonedDateTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(HOUR_MINUTE_TIME_FORMATTER)
}
fun getFormattedWithAmPm(zonedDateTime: ZonedDateTime?): String? {
    return zonedDateTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(HOUR_MINUTE_AM_PM_TIME_FORMATTER)
}

fun String.toZoneTime(): ZonedDateTime = LocalTime.parse(this, HOUR_MINUTE_AM_PM_TIME_FORMATTER).atDate(LocalDate.now()).atZone(ZoneId.systemDefault())

fun ZonedDateTime.toSpacedHourFormat() = withZoneSameInstant(ZoneId.systemDefault()).formattedWith(SPACED_HOUR_MINUTE_AM_PM_TIME_FORMATTER)

fun String.toZonedDateTimeFromFormattedUtcTime(): ZonedDateTime = LocalDateTime.parse(this, MONTH_DAY_YEAR_HOUR_MIN_SEC_AM_PM_TIME_FORMATTER).atZone(ZoneId.of("UTC"))

fun ZonedDateTime.toSameZoneInstantLocalDate() = withZoneSameInstant(ZoneId.systemDefault()).toLocalDate()

fun String.toDob(): LocalDate? {
    return try {
        LocalDate.parse(this, DOB_FORMATTER)
    } catch (e: DateTimeParseException) {
        null
    }
}

fun LocalDate.toFormattedDob(): String = this.format(DOB_FORMATTER)

fun LocalDate.toServerFormattedDob(): String = this.format(SERVER_DOB_FORMAT)

// For use with the Drivers License barcode scanner where the
// format for date can differ by state
fun String.toLocalDateDob(): LocalDate? {
    return try {
        LocalDate.parse(this, DL_DOB_FORMATTER_VARIANT_1)
    } catch (_: Exception) {
        null
    } ?: try {
        LocalDate.parse(this, DL_DOB_FORMATTER_VARIANT_2)
    } catch (_: Exception) {
        Timber.d("[BarcodeScan] unable to parse $this")
        null
    }
}

fun getAge(dateOfBirth: LocalDate?): Int {
    var age = 0
    try {
        val dobCalendar: Calendar = Calendar.getInstance()
        dateOfBirth?.apply {
            // TODO - The index of the MONTH value was off by 1 for dobCalendar; fix this to be cleaner when we have a chance
            dobCalendar.set(Calendar.MONTH, monthValue - 1)
            dobCalendar.set(Calendar.DATE, dayOfMonth)
            dobCalendar.set(Calendar.YEAR, year)
        }

        val today = Calendar.getInstance()
        if (dobCalendar.get(Calendar.YEAR).toString().length == 4) {
            age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR)
        } else {
            return 0
        }

        if (today.get(Calendar.MONTH) == dobCalendar.get(Calendar.MONTH)) {
            if (today.get(Calendar.DAY_OF_MONTH) < dobCalendar.get(Calendar.DAY_OF_MONTH)) {
                age -= 1
            }
        } else if (today.get(Calendar.MONTH) < dobCalendar.get(Calendar.MONTH)) {
            age -= 1
        }
        return age
    } catch (e: Exception) {
        return age
    }
}
