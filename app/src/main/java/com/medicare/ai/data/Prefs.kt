package com.medicare.ai.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore("medicare_prefs")

class Prefs(private val ctx: Context) {
    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val water = intPreferencesKey("water_goal"); val sleep = floatPreferencesKey("sleep_goal")
        val steps = intPreferencesKey("step_goal"); val saveChat = booleanPreferencesKey("save_chat")
        val theme = intPreferencesKey("theme")
        val baseDay = longPreferencesKey("step_base_day"); val baseVal = floatPreferencesKey("step_base_val")
    }
    private val d: Flow<Preferences> get() = ctx.ds.data
    val onboarded = d.map { it[K.onboarded] ?: false }
    val waterGoal = d.map { it[K.water] ?: 2500 }
    val sleepGoal = d.map { it[K.sleep] ?: 8f }
    val stepGoal = d.map { it[K.steps] ?: 8000 }
    val saveChat = d.map { it[K.saveChat] ?: true }
    val theme: Flow<Int> = d.map { it[K.theme] ?: 0 }          // 0 system, 1 light, 2 dark
    val stepBase = d.map { (it[K.baseDay] ?: -1L) to (it[K.baseVal] ?: 0f) }

    suspend fun setOnboarded() { ctx.ds.edit { it[K.onboarded] = true } }
    suspend fun setWaterGoal(v: Int) { ctx.ds.edit { it[K.water] = v } }
    suspend fun setSleepGoal(v: Float) { ctx.ds.edit { it[K.sleep] = v } }
    suspend fun setStepGoal(v: Int) { ctx.ds.edit { it[K.steps] = v } }
    suspend fun setSaveChat(v: Boolean) { ctx.ds.edit { it[K.saveChat] = v } }
    suspend fun setTheme(v: Int) { ctx.ds.edit { it[K.theme] = v } }
    suspend fun setStepBase(day: Long, v: Float) { ctx.ds.edit { it[K.baseDay] = day; it[K.baseVal] = v } }
}