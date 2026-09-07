package ch.bus.roadpanel.feature.energy.data

import com.google.gson.annotations.SerializedName

data class VictronHealthDto(
    val status: String,
    @SerializedName("mqtt_connected")
    val mqttConnected: Boolean,
    @SerializedName("last_message_timestamp")
    val lastMessageTimestamp: String?,
)

data class VictronDeviceDto(
    val timestamp: String? = null,
    val name: String? = null,
    val address: String? = null,
    val rssi: Int? = null,
    @SerializedName("model_name")
    val modelName: String? = null,
    @SerializedName("battery_charging_current")
    val batteryChargingCurrent: Double? = null,
    @SerializedName("battery_voltage")
    val batteryVoltage: Double? = null,
    @SerializedName("charge_state")
    val chargeState: String? = null,
    @SerializedName("charger_error")
    val chargerError: String? = null,
    @SerializedName("solar_power")
    val solarPower: Double? = null,
    @SerializedName("yield_today")
    val yieldToday: Double? = null,
    @SerializedName("alarm_reason")
    val alarmReason: String? = null,
    @SerializedName("device_state")
    val deviceState: String? = null,
    @SerializedName("error_code")
    val errorCode: String? = null,
    @SerializedName("input_voltage")
    val inputVoltage: Double? = null,
    @SerializedName("off_reason")
    val offReason: String? = null,
    @SerializedName("output_state")
    val outputState: String? = null,
    @SerializedName("output_voltage")
    val outputVoltage: Double? = null,
    @SerializedName("warning_reason")
    val warningReason: String? = null,
    @SerializedName("ac_apparent_power")
    val acApparentPower: Double? = null,
    @SerializedName("ac_current")
    val acCurrent: Double? = null,
    @SerializedName("ac_voltage")
    val acVoltage: Double? = null,
)

typealias VictronMetricsDto = Map<String, VictronDeviceDto>

data class VictronWaitingDto(
    val status: String,
)
