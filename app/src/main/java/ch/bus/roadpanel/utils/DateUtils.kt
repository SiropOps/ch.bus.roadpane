package ch.bus.roadpanel.utils

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val fallbackDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

fun formatRelativeTime(time: String): String {
    val zone = ZoneId.systemDefault()
    val instant = time.toInstantOrNull(zone) ?: return time
    val now = Instant.now()
    val duration = Duration.between(instant, now)

    return when {
        duration.seconds < 60 -> "maintenant"
        duration.toMinutes() < 60 -> "il y a ${duration.toMinutes()} min"
        duration.toHours() < 24 -> "il y a ${duration.toHours()} h"
        duration.toDays() < 7 -> "il y a ${duration.toDays()} j"
        else -> instant.atZone(zone).format(fallbackDateTimeFormatter)
    }
}

private fun String.toInstantOrNull(zone: ZoneId): Instant? {
    val value = trim()
    return parseEpochMillis(value)
        ?: parseInstant(value)
        ?: parseOffsetDateTime(value)
        ?: parseLocalDateTime(value, zone)
        ?: parseLocalTime(value, zone)
}

private fun parseEpochMillis(value: String): Instant? {
    if (!value.all { it.isDigit() }) return null
    return value.toLongOrNull()?.let(Instant::ofEpochMilli)
}

private fun parseInstant(value: String): Instant? = try {
    Instant.parse(value)
} catch (_: DateTimeParseException) {
    null
}

private fun parseOffsetDateTime(value: String): Instant? = try {
    OffsetDateTime.parse(value).toInstant()
} catch (_: DateTimeParseException) {
    null
}

private fun parseLocalDateTime(value: String, zone: ZoneId): Instant? = try {
    LocalDateTime.parse(value).atZone(zone).toInstant()
} catch (_: DateTimeParseException) {
    null
}

private fun parseLocalTime(value: String, zone: ZoneId): Instant? = try {
    val now = LocalDate.now(zone)
    val candidate = LocalTime.parse(value).atDate(now).atZone(zone).toInstant()
    if (candidate.isAfter(Instant.now())) {
        candidate.minus(Duration.ofDays(1))
    } else {
        candidate
    }
} catch (_: DateTimeParseException) {
    null
}
