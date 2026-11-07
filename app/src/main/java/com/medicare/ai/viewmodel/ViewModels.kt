package com.medicare.ai.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.medicare.ai.AppContainer
import com.medicare.ai.data.*
import com.medicare.ai.domain.Safety
import com.medicare.ai.reminders.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

abstract class BaseVM(protected val c: AppContainer) : ViewModel() {
    protected val uid = c.auth.uidFlow
    protected fun <T> forUser(empty: T, f: (String) -> Flow<T>): StateFlow<T> =
        uid.flatMapLatest { if (it == null) flowOf(empty) else f(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), empty)
}

// ───────── Auth ─────────
class AuthVM(c: AppContainer) : BaseVM(c) {
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val info = MutableStateFlow<String?>(null)
    val name = c.auth.userFlow.map { it?.displayName.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), c.auth.currentUser?.displayName.orEmpty())

    fun clear() { error.value = null; info.value = null }

    fun submit(signUp: Boolean, name: String, email: String, pw: String, onOk: () -> Unit) {
        clear(); val e = email.trim()
        when {
            signUp && name.isBlank() -> { error.value = "Please enter your name."; return }
            !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> { error.value = "Enter a valid email address."; return }
            pw.length < 6 -> { error.value = "Password must be at least 6 characters."; return }
        }
        viewModelScope.launch {
            loading.value = true
            try {
                if (signUp) c.auth.signUp(name.trim(), e, pw) else c.auth.login(e, pw)
                c.rescheduleAll(); onOk()
            } catch (ex: Exception) { error.value = c.auth.friendly(ex) } finally { loading.value = false }
        }
    }

    fun resetPassword(email: String) {
        clear()
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { error.value = "Enter your email above first."; return }
        viewModelScope.launch {
            try { c.auth.resetPassword(email.trim()); info.value = "Password reset email sent." }
            catch (ex: Exception) { error.value = c.auth.friendly(ex) }
        }
    }
}

// ───────── Chat ─────────
class ChatVM(c: AppContainer) : BaseVM(c) {
    private val _msgs = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _msgs.asStateFlow()
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val emergency = MutableStateFlow(false)

    init { reload() }
    fun reload() = viewModelScope.launch {
        val u = c.auth.currentUid ?: return@launch
        if (c.prefs.saveChat.first()) _msgs.value = c.dao.chatNow(u)
    }
    fun clearError() { error.value = null }

    private suspend fun add(u: String, role: String, text: String) {
        val m = ChatMessage(uid = u, role = role, text = text)
        _msgs.update { it + m }
        if (c.prefs.saveChat.first()) c.dao.insertChat(m)   // privacy: only stored if user allows
    }

    fun send(text: String) {
        val t = text.trim(); if (t.isEmpty() || busy.value) return
        viewModelScope.launch {
            val u = c.auth.currentUid ?: return@launch
            add(u, "user", t); emergency.value = false
            if (Safety.isEmergency(t)) { emergency.value = true; add(u, "assistant", Safety.EMERGENCY_REPLY) }
            else ask(u)
        }
    }
    fun askAnyway() { emergency.value = false; viewModelScope.launch { c.auth.currentUid?.let { ask(it) } } }
    fun dismissEmergency() { emergency.value = false }

    private suspend fun ask(u: String) {
        busy.value = true
        try {
            val token = c.auth.token() ?: throw IllegalStateException("no token")
            val hist = _msgs.value.filter { it.text != Safety.EMERGENCY_REPLY }.takeLast(10).map { ChatMsg(it.role, it.text) }
            val r = c.api.chat("Bearer $token", ChatReq(hist))
            if (r.emergency == true) emergency.value = true
            add(u, "assistant", r.reply ?: "Sorry, I couldn't generate a reply.")
        } catch (e: IOException) { error.value = "Can't reach the assistant. Check your internet connection and try again."
        } catch (e: HttpException) { error.value = if (e.code() == 429) "Too many requests. Please wait a minute." else "The assistant is unavailable right now."
        } catch (e: Exception) { error.value = "Something went wrong. Please try again."
        } finally { busy.value = false }
    }
}

// ───────── Medicines ─────────
data class DoseItem(val med: Medicine, val time: String, val status: String)

class MedVM(c: AppContainer) : BaseVM(c) {
    val meds = forUser(emptyList<Medicine>()) { c.dao.meds(it) }
    val logs = forUser(emptyList<DoseLog>()) { c.dao.logs(it) }
    val today: StateFlow<List<DoseItem>> = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList()) else {
            val day = LocalDate.now().toEpochDay()
            combine(c.dao.meds(u), c.dao.logsOn(u, day)) { ms, ls ->
                ms.filter { day in it.startDay..it.endDay }.flatMap { m ->
                    m.times.split(",").map { t ->
                        DoseItem(m, t, ls.firstOrNull { it.medicineId == m.id && it.time == t }?.status ?: "PENDING")
                    }
                }.sortedBy { it.time }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(m: Medicine, old: Medicine?) = viewModelScope.launch {
        val u = c.auth.currentUid ?: return@launch
        old?.let { ReminderScheduler.cancel(c.app, it) }
        val saved = m.copy(uid = u)
        val id = c.dao.upsertMed(saved)
        ReminderScheduler.schedule(c.app, saved.copy(id = if (saved.id == 0L) id else saved.id))
    }
    fun delete(m: Medicine) = viewModelScope.launch { ReminderScheduler.cancel(c.app, m); c.dao.deleteMed(m.id) }
    fun mark(d: DoseItem, status: String) = viewModelScope.launch {
        val u = c.auth.currentUid ?: return@launch
        c.dao.upsertLog(DoseLog(uid = u, medicineId = d.med.id, medicineName = d.med.name,
            day = LocalDate.now().toEpochDay(), time = d.time, status = status))
    }
}

// ───────── Appointments ─────────
class ApptVM(c: AppContainer) : BaseVM(c) {
    val appts = forUser(emptyList<Appointment>()) { c.dao.appts(it) }
    fun save(a: Appointment) = viewModelScope.launch { c.auth.currentUid?.let { c.dao.upsertAppt(a.copy(uid = it)) } }
    fun delete(a: Appointment) = viewModelScope.launch { c.dao.deleteAppt(a.id) }
}

// ───────── Wellness (water / sleep / steps) ─────────
data class Goals(val water: Int = 2500, val sleep: Float = 8f, val steps: Int = 8000)

class WellnessVM(c: AppContainer) : BaseVM(c) {
    val goals = combine(c.prefs.waterGoal, c.prefs.sleepGoal, c.prefs.stepGoal) { w, s, st -> Goals(w, s, st) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Goals())
    val week = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList()) else c.dao.since(u, LocalDate.now().toEpochDay() - 6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    fun say(m: String) { message.tryEmit(m) }

    fun add(type: String, v: Float) {
        val max = when (type) { "WATER" -> 5000f; "SLEEP" -> 24f; else -> 100000f }
        if (v <= 0f || v > max) { say("Enter a value above 0 and up to ${max.toInt()}."); return }
        viewModelScope.launch {
            val u = c.auth.currentUid ?: return@launch
            c.dao.insertWellness(WellnessEntry(uid = u, type = type, day = LocalDate.now().toEpochDay(), value = v, source = "MANUAL"))
        }
    }
    fun delete(id: Long) = viewModelScope.launch { c.dao.deleteWellness(id) }

    fun syncSteps() = viewModelScope.launch {
        val u = c.auth.currentUid ?: return@launch
        val cur = readStepCounter(c.app) ?: run { say("Step sensor unavailable or no reading yet. Walk a few steps and try again."); return@launch }
        val today = LocalDate.now().toEpochDay()
        val (bd, bv) = c.prefs.stepBase.first()
        val base = if (bd != today || cur < bv) { c.prefs.setStepBase(today, cur); cur } else bv
        val steps = (cur - base).coerceAtLeast(0f)
        c.dao.clearDeviceSteps(u, today)
        c.dao.insertWellness(WellnessEntry(uid = u, type = "STEPS", day = today, value = steps, source = "DEVICE"))
        say(if (bd != today) "Step counting started for today. Sync again later to see steps." else "Synced ${steps.toInt()} steps from your phone.")
    }

    fun setGoal(type: String, v: Float) = viewModelScope.launch {
        when (type) { "WATER" -> c.prefs.setWaterGoal(v.toInt()); "SLEEP" -> c.prefs.setSleepGoal(v); else -> c.prefs.setStepGoal(v.toInt()) }
    }
}

// ───────── Profile + Settings ─────────
class ProfileVM(c: AppContainer) : BaseVM(c) {
    val profile = forUser<HealthProfile?>(null) { c.dao.profile(it) }
    val theme = c.prefs.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val saveChat = c.prefs.saveChat.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun save(p: HealthProfile) = viewModelScope.launch { c.auth.currentUid?.let { c.dao.upsertProfile(p.copy(uid = it)) } }
    fun deleteProfile() = viewModelScope.launch { c.auth.currentUid?.let { c.dao.deleteProfile(it) } }
    fun setTheme(v: Int) = viewModelScope.launch { c.prefs.setTheme(v) }
    fun setSaveChat(v: Boolean) = viewModelScope.launch { c.prefs.setSaveChat(v); if (!v) c.auth.currentUid?.let { c.dao.clearChat(it) } }
    fun clearChat() = viewModelScope.launch { c.auth.currentUid?.let { c.dao.clearChat(it) } }
    fun wipeAll() = viewModelScope.launch {
        val u = c.auth.currentUid ?: return@launch
        c.cancelAll()
        c.db.withTransaction {
            c.dao.wipeMeds(u); c.dao.wipeLogs(u); c.dao.wipeAppts(u); c.dao.wipeWellness(u)
            c.dao.clearChat(u); c.dao.deleteProfile(u)
        }
    }
    fun logout() = viewModelScope.launch { c.cancelAll(); c.auth.logout() }
}