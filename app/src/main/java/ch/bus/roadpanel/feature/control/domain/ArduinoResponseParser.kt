package ch.bus.roadpanel.feature.control.domain

object ArduinoResponseParser {
    fun lastWifiState(raw: String): Boolean? = when (lastValidCode(raw)) {
        '1' -> true
        '0' -> false
        else -> null
    }

    fun lastValidCode(raw: String): Char? = raw.lastOrNull { it == '1' || it == '0' }
}
