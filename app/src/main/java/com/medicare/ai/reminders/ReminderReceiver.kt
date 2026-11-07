package com.medicare.ai.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medicare.ai.MainActivity
import com.medicare.ai.MediCareApp

class ReminderReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(ctx: Context, i: Intent) {
        val name = i.getStringExtra("name") ?: return
        val time = i.getStringExtra("time") ?: return
        val id = i.getLongExtra("id", 0)
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, MediCareApp.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Medicine reminder")
            .setContentText("Time for $name ($time). Follow your doctor's prescription.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)   // hide name on lock screen
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(open).build()
        val nm = NotificationManagerCompat.from(ctx)
        if (nm.areNotificationsEnabled()) nm.notify((id * 10000 + time.replace(":", "").toInt()).toInt(), n)
        // arm the next day's alarm
        ReminderScheduler.scheduleOne(ctx, id, name, i.getLongExtra("start", 0), i.getLongExtra("end", 0), time)
    }
}