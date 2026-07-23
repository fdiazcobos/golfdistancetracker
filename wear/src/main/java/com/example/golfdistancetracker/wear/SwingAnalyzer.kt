package com.example.golfdistancetracker.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class SwingEvent(
    val type: EventType,
    val timestamp: Long,
    val tempoRatio: Double? = null,
    val peakG: Float? = null
)

enum class EventType {
    BACKSWING_START,
    TOP_OF_SWING,
    IMPACT
}

class SwingAnalyzer(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _events = MutableSharedFlow<SwingEvent>()
    val events = _events.asSharedFlow()

    private var backswingStartTime = 0L
    private var transitionTime = 0L
    private var isBackswingInProgress = false
    private var isDownswingInProgress = false

    // Detection thresholds
    private val IMPACT_THRESHOLD = 50f // Linear acceleration m/s^2
    private val GYRO_BACKSWING_THRESHOLD = 3.0f // rad/s

    fun start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val gForce = Math.sqrt(
                    (event.values[0] * event.values[0] + 
                     event.values[1] * event.values[1] + 
                     event.values[2] * event.values[2]).toDouble()
                ).toFloat()

                if (gForce > IMPACT_THRESHOLD && isDownswingInProgress) {
                    val impactTime = System.currentTimeMillis()
                    val backswingDuration = transitionTime - backswingStartTime
                    val downswingDuration = impactTime - transitionTime
                    
                    val ratio = if (downswingDuration > 0) backswingDuration.toDouble() / downswingDuration else 0.0
                    
                    _events.tryEmit(SwingEvent(EventType.IMPACT, impactTime, ratio, gForce))
                    reset()
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val angularVel = event.values[2] // Z axis usually for wrist rotation in golf

                if (!isBackswingInProgress && Math.abs(angularVel) > GYRO_BACKSWING_THRESHOLD) {
                    backswingStartTime = System.currentTimeMillis()
                    isBackswingInProgress = true
                    _events.tryEmit(SwingEvent(EventType.BACKSWING_START, backswingStartTime))
                } else if (isBackswingInProgress && !isDownswingInProgress) {
                    // Detect direction change at the top
                    if (angularVel * -1 > GYRO_BACKSWING_THRESHOLD) { // Reverse direction
                        transitionTime = System.currentTimeMillis()
                        isDownswingInProgress = true
                        _events.tryEmit(SwingEvent(EventType.TOP_OF_SWING, transitionTime))
                    }
                }
            }
        }
    }

    private fun reset() {
        isBackswingInProgress = false
        isDownswingInProgress = false
        backswingStartTime = 0L
        transitionTime = 0L
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
