package com.medicare.ai.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medicare.ai.data.Medicine
import java.time.*

object ReminderScheduler {
    /** Pure function (unit-tested). Returns next trigger in epoch millis, or null if past end date. */
    fun nextTrigger(startDay: Long, endDay: Long, time: String, now: LocalDateTime = LocalDateTime.now()): Long? {
        val t = LocalTime.parse(time)
        var dt = LocalDateTime.of(now.toLocalDate(), t)
        if (!dt.isAfter(now.plusSeconds(30))) dt = dt.plusDays(1)
        val start = LocalDate.ofEpochDay(startDay)
        if (dt.toLocalDate().isBefore(start)) dt = LocalDateTime.of(start, t)
        if (dt.toLocalDate().isAfter(LocalDate.ofEpochDay(endDay))) return null
        return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun code(id: Long, t: String) = (id * 10000 + t.replace(":", "").toInt()).toInt()

    fun schedule(ctx: Context, m: Medicine) =
        m.times.split(",").forEach { scheduleOne(ctx, m.id, m.name, m.startDay, m.endDay, it) }

    fun scheduleOne(ctx: Context, id: Long, name: String, start: Long, end: Long, time: String) {
        val at = nextTrigger(start, end, time) ?: return
        val am = ctx.getSystemService(AlarmManager::class.java)
        val i = Intent(ctx, ReminderReceiver::class.java)
            .putExtra("id", id).putExtra("name", name).putExtra("start", start).putExtra("end", end).putExtra("time", time)
        val pi = PendingIntent.getBroadcast(ctx, code(id, time), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
    }

    fun cancel(ctx: Context, m: Medicine) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        m.times.split(",").forEach {
            PendingIntent.getBroadcast(ctx, code(m.id, it), Intent(ctx, ReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { pi -> am.cancel(pi); pi.cancel() }
        }
    }
}