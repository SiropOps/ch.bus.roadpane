package ch.bus.roadpanel.feature.energy.data

import com.google.gson.annotations.SerializedName

data class VictronHealthDto(
    val status: String,
    @SerializedName("mqtt_connected")
    val mqttConnected: Boolean,
    @SerializedName("last_message_timestamp")
    val lastMessageTimestamp: String?,
)

data class VictronMetricsDto(
    val timestamp: String,
    @SerializedName("battery_charging_current")
    val batteryChargingCurrent: Double,
    @SerializedName("battery_voltage")
    val batteryVoltage: Double,
    @SerializedName("charge_state")
    val chargeState: String,
    @SerializedName("solar_power")
    val solarPower: Double,
    @SerializedName("yield_today")
    val yieldToday: Double,
)

data class VictronWaitingDto(
    val status: String,
)
