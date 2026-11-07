package com.medicare.ai

import android.app.*
import com.google.firebase.auth.FirebaseAuth
import com.medicare.ai.reminders.ReminderScheduler
import kotlinx.coroutines.*

class MediCareApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Medicine reminders", NotificationManager.IMPORTANCE_HIGH)
        )
        // Safety net: re-arm alarms for the signed-in user on every launch
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseAuth.getInstance().currentUser?.uid?.let { u ->
                container.dao.medsNow(u).forEach { ReminderScheduler.schedule(this@MediCareApp, it) }
            }
        }
    }
    companion object { const val CHANNEL = "medicine_reminders" }
}