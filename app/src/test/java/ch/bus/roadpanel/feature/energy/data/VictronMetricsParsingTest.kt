package ch.bus.roadpanel.feature.energy.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VictronMetricsParsingTest {
    @Test
    fun `parses metrics keyed by device name`() {
        val json = """
            {
              "smartsolar_pyleas": {
                "timestamp": "2026-09-07T16:37:00.868703+00:00",
                "name": "smartsolar_pyleas",
                "address": "E1:EA:0C:89:CC:C5",
                "rssi": -56,
                "model_name": "SmartSolar Charger MPPT 100/30",
                "battery_charging_current": 0.8,
                "battery_voltage": 12.82,
                "charge_state": "bulk",
                "charger_error": "no_error",
                "solar_power": 10,
                "yield_today": 480
              },
              "batteryprotec_pyleas": {
                "name": "batteryprotec_pyleas",
                "alarm_reason": "no_alarm",
                "device_state": "active",
                "error_code": "no_error",
                "input_voltage": 12.79,
                "off_reason": "no_reason",
                "output_state": "on",
                "output_voltage": 12.79,
                "warning_reason": "no_alarm"
              },
              "ve_direct_pyleas": {
                "name": "ve_direct_pyleas",
                "model_name": "Phoenix Inverter 12V 500VA 230V",
                "ac_apparent_power": 230,
                "ac_current": 1.0,
                "ac_voltage": 230.02,
                "battery_voltage": 12.65,
                "device_state": "inverting"
              }
            }
        """.trimIndent()

        val type = object : TypeToken<Map<String, VictronDeviceDto>>() {}.type
        val metrics: VictronMetricsDto = Gson().fromJson(json, type)

        assertEquals(3, metrics.size)
        assertEquals(10.0, metrics.getValue("smartsolar_pyleas").solarPower)
        assertEquals("on", metrics.getValue("batteryprotec_pyleas").outputState)
        assertEquals(230.02, metrics.getValue("ve_direct_pyleas").acVoltage)
        assertTrue(metrics.values.all { it.name != null })
    }
}
