package com.medicare.ai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertUser(u: UserEntity)

    @Query("SELECT * FROM profiles WHERE uid=:uid") fun profile(uid: String): Flow<HealthProfile?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProfile(p: HealthProfile)
    @Query("DELETE FROM profiles WHERE uid=:uid") suspend fun deleteProfile(uid: String)

    @Query("SELECT * FROM medicines WHERE uid=:uid ORDER BY name") fun meds(uid: String): Flow<List<Medicine>>
    @Query("SELECT * FROM medicines WHERE uid=:uid") suspend fun medsNow(uid: String): List<Medicine>
    @Query("SELECT * FROM medicines") suspend fun allMeds(): List<Medicine>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMed(m: Medicine): Long
    @Query("DELETE FROM medicines WHERE id=:id") suspend fun deleteMed(id: Long)

    @Query("SELECT * FROM dose_logs WHERE uid=:uid AND day=:day") fun logsOn(uid: String, day: Long): Flow<List<DoseLog>>
    @Query("SELECT * FROM dose_logs WHERE uid=:uid ORDER BY day DESC, time DESC LIMIT 300") fun logs(uid: String): Flow<List<DoseLog>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLog(l: DoseLog)

    @Query("SELECT * FROM appointments WHERE uid=:uid ORDER BY whenMillis") fun appts(uid: String): Flow<List<Appointment>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAppt(a: Appointment)
    @Query("DELETE FROM appointments WHERE id=:id") suspend fun deleteAppt(id: Long)

    @Query("SELECT * FROM wellness WHERE uid=:uid AND day>=:from") fun since(uid: String, from: Long): Flow<List<WellnessEntry>>
    @Insert suspend fun insertWellness(e: WellnessEntry)
    @Query("DELETE FROM wellness WHERE id=:id") suspend fun deleteWellness(id: Long)
    @Query("DELETE FROM wellness WHERE uid=:uid AND type='STEPS' AND source='DEVICE' AND day=:day")
    suspend fun clearDeviceSteps(uid: String, day: Long)

    @Query("SELECT * FROM chat_messages WHERE uid=:uid ORDER BY ts") suspend fun chatNow(uid: String): List<ChatMessage>
    @Insert suspend fun insertChat(m: ChatMessage)
    @Query("DELETE FROM chat_messages WHERE uid=:uid") suspend fun clearChat(uid: String)

    // Privacy: wipe everything for a user
    @Query("DELETE FROM medicines WHERE uid=:uid") suspend fun wipeMeds(uid: String)
    @Query("DELETE FROM dose_logs WHERE uid=:uid") suspend fun wipeLogs(uid: String)
    @Query("DELETE FROM appointments WHERE uid=:uid") suspend fun wipeAppts(uid: String)
    @Query("DELETE FROM wellness WHERE uid=:uid") suspend fun wipeWellness(uid: String)
}

@Database(
    entities = [UserEntity::class, HealthProfile::class, Medicine::class, DoseLog::class,
        Appointment::class, WellnessEntry::class, ChatMessage::class],
    version = 1, exportSchema = false
)
abstract class AppDb : RoomDatabase() { abstract fun dao(): AppDao }