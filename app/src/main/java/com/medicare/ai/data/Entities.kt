package com.medicare.ai.data

import androidx.room.*

@Entity(tableName = "users")
data class UserEntity(@PrimaryKey val uid: String, val email: String, val name: String)

@Entity(tableName = "profiles")
data class HealthProfile(
    @PrimaryKey val uid: String,
    val age: Int? = null, val heightCm: Int? = null, val weightKg: Float? = null,
    val allergies: String = "", val conditions: String = "",
    val emergencyName: String = "", val emergencyPhone: String = ""
)

/** times = comma-separated "HH:mm"; startDay/endDay = LocalDate epoch days */
@Entity(tableName = "medicines", indices = [Index("uid")])
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val uid: String,
    val name: String, val schedule: String, val times: String, val startDay: Long, val endDay: Long
)

/** status: PENDING | TAKEN | MISSED. Kept as history even after the medicine is deleted. */
@Entity(tableName = "dose_logs", indices = [Index(value = ["medicineId", "day", "time"], unique = true), Index("uid")])
data class DoseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val uid: String,
    val medicineId: Long, val medicineName: String, val day: Long, val time: String, val status: String
)

@Entity(tableName = "appointments", indices = [Index("uid")])
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val uid: String,
    val doctor: String, val place: String, val whenMillis: Long, val notes: String
)

/** type: WATER (ml) | SLEEP (hours) | STEPS ; source: MANUAL | DEVICE */
@Entity(tableName = "wellness", indices = [Index(value = ["uid", "type", "day"])])
data class WellnessEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val uid: String,
    val type: String, val day: Long, val value: Float, val source: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** role: user | assistant */
@Entity(tableName = "chat_messages", indices = [Index("uid")])
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val uid: String,
    val role: String, val text: String, val ts: Long = System.currentTimeMillis()
)