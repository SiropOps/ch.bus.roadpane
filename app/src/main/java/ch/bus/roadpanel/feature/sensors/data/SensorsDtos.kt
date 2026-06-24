package ch.bus.roadpanel.feature.sensors.data

import com.google.gson.annotations.SerializedName

data class SensorsResponseDto(
    val timestamp: String,
    @SerializedName("sensor_count") val sensorCount: Int,
    @SerializedName("expected_sensor_count") val expectedSensorCount: Int,
    @SerializedName("missing_sensors") val missingSensors: List<String> = emptyList(),
    val sensors: Map<String, SensorDto> = emptyMap(),
)

data class SensorDto(
    val timestamp: String?,
    val name: String?,
    val address: String?,
    val protocol: String?,
    val rssi: Int?,
    val model: String?,
    val temperature: Double?,
    val battery: Int?,
    val humidity: Double?,
    val pressure: Int?,
    @SerializedName("data_format") val dataFormat: Int?,
    @SerializedName("acceleration_x") val accelerationX: Int?,
    @SerializedName("acceleration_y") val accelerationY: Int?,
    @SerializedName("acceleration_z") val accelerationZ: Int?,
    @SerializedName("battery_voltage") val batteryVoltage: Double?,
    @SerializedName("button_pressed") val buttonPressed: Boolean?,
    @SerializedName("tx_power") val txPower: Int?,
    @SerializedName("movement_counter") val movementCounter: Int?,
    @SerializedName("measurement_sequence") val measurementSequence: Int?,
    @SerializedName("sensor_id") val sensorId: String?,
    @SerializedName("received_at") val receivedAt: String?,
)

data class SensorHistoryResponseDto(
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("reading_count") val readingCount: Int = 0,
    @SerializedName("sensor_id") val sensorId: String? = null,
    val readings: List<SensorDto> = emptyList(),
)
