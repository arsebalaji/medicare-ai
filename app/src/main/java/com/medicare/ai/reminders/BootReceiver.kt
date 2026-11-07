package com.medicare.ai.reminders

import android.content.*
import com.medicare.ai.MediCareApp
import kotlinx.coroutines.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        if (i.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { (ctx.applicationContext as MediCareApp).container.rescheduleAll() } finally { pending.finish() }
        }
    }
}