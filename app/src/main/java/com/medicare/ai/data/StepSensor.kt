package com.medicare.ai.data

import android.content.Context
import android.hardware.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Cumulative steps since last phone reboot, or null if unavailable / no reading within 4 s. */
suspend fun readStepCounter(ctx: Context): Float? = withTimeoutOrNull(4000) {
    suspendCancellableCoroutine<Float?> { cont ->
        val sm = ctx.getSystemService(SensorManager::class.java)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) { cont.resume(null); return@suspendCancellableCoroutine }
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { sm.unregisterListener(this); if (cont.isActive) cont.resume(e.values[0]) }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        cont.invokeOnCancellation { sm.unregisterListener(l) }
    }
}