package ch.bus.roadpanel.feature.control.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArduinoResponseParserTest {
    @Test
    fun keepsLastValidWifiState() {
        assertEquals(true, ArduinoResponseParser.lastWifiState("\r\n1)"))
        assertEquals(false, ArduinoResponseParser.lastWifiState("1\n0\r)"))
    }

    @Test
    fun ignoresNoiseWithoutState() {
        assertNull(ArduinoResponseParser.lastWifiState("\r\n )"))
    }
}
