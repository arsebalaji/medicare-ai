package com.medicare.ai

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.medicare.ai.data.*
import com.medicare.ai.reminders.ReminderScheduler
import com.medicare.ai.viewmodel.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Manual dependency container (keeps the project beginner-friendly, no Hilt needed). */
class AppContainer(val app: Application) {
    val db = Room.databaseBuilder(app, AppDb::class.java, "medicare.db").build()
    val dao = db.dao()
    val prefs = Prefs(app)
    val auth = AuthRepo(dao)
    val api: ChatApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_URL)
        .client(OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(ChatApi::class.java)

    suspend fun rescheduleAll() { auth.currentUid?.let { u -> dao.medsNow(u).forEach { ReminderScheduler.schedule(app, it) } } }
    suspend fun cancelAll() { dao.allMeds().forEach { ReminderScheduler.cancel(app, it) } }
}

class VMFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthVM::class.java) -> AuthVM(c)
        modelClass.isAssignableFrom(ChatVM::class.java) -> ChatVM(c)
        modelClass.isAssignableFrom(MedVM::class.java) -> MedVM(c)
        modelClass.isAssignableFrom(ApptVM::class.java) -> ApptVM(c)
        modelClass.isAssignableFrom(WellnessVM::class.java) -> WellnessVM(c)
        modelClass.isAssignableFrom(ProfileVM::class.java) -> ProfileVM(c)
        else -> error("Unknown ViewModel $modelClass")
    } as T
}