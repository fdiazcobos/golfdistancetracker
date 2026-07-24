package com.example.golfdistancetracker.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

class SwingAnalyzer(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val _events = MutableSharedFlow<SwingEvent>()
    val events = _events.asSharedFlow()

    private var backswingStartTime = 0L
    private var transitionTime = 0L
    private var isBackswingInProgress = false
    private var isDownswingInProgress = false

    // Detection thresholds
    private val IMPACT_THRESHOLD = 35f // Lowered to 35 for better sensitivity
    private val GYRO_BACKSWING_THRESHOLD = 3.0f // rad/s

    fun start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        reset()
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
                    
                    vibrateImpact()
                    _events.tryEmit(SwingEvent(EventType.IMPACT, impactTime, ratio, gForce))
                    reset()
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val angularVel = event.values[2] 

                if (!isBackswingInProgress && Math.abs(angularVel) > GYRO_BACKSWING_THRESHOLD) {
                    backswingStartTime = System.currentTimeMillis()
                    isBackswingInProgress = true
                    vibrateStart()
                    _events.tryEmit(SwingEvent(EventType.BACKSWING_START, backswingStartTime))
                } else if (isBackswingInProgress && !isDownswingInProgress) {
                    if (angularVel * -1 > GYRO_BACKSWING_THRESHOLD) { 
                        transitionTime = System.currentTimeMillis()
                        isDownswingInProgress = true
                        _events.tryEmit(SwingEvent(EventType.TOP_OF_SWING, transitionTime))
                    }
                }
            }
        }
    }

    private fun vibrateStart() {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateImpact() {
        vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun reset() {
        isBackswingInProgress = false
        isDownswingInProgress = false
        backswingStartTime = 0L
        transitionTime = 0L
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
