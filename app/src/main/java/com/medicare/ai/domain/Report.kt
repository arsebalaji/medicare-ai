package com.medicare.ai.domain

import com.medicare.ai.data.*
import java.time.LocalDate

fun List<WellnessEntry>.dayTotal(type: String, day: Long): Float =
    filter { it.type == type && it.day == day }.sumOf { it.value.toDouble() }.toFloat()

fun buildReport(week: List<WellnessEntry>, logs: List<DoseLog>, appts: List<Appointment>, profile: HealthProfile?): String {
    val today = LocalDate.now().toEpochDay()
    val days = (0..6).map { today - it }
    fun avg(t: String) = "%.1f".format(days.map { week.dayTotal(t, it) }.average())
    val recent = logs.filter { it.day >= today - 6 }
    val now = System.currentTimeMillis()
    return buildString {
        appendLine("MediCare AI – 7-day health summary"); appendLine("Date: ${LocalDate.now()}"); appendLine()
        if (profile != null) {
            appendLine("Profile (shared by you):")
            profile.age?.let { appendLine("- Age: $it") }
            profile.heightCm?.let { appendLine("- Height: $it cm") }
            profile.weightKg?.let { appendLine("- Weight: $it kg") }
            if (profile.allergies.isNotBlank()) appendLine("- Allergies: ${profile.allergies}")
            if (profile.conditions.isNotBlank()) appendLine("- Conditions: ${profile.conditions}")
            appendLine()
        }
        appendLine("Daily averages (self-recorded):")
        appendLine("- Water: ${avg("WATER")} ml"); appendLine("- Sleep: ${avg("SLEEP")} h"); appendLine("- Steps: ${avg("STEPS")}")
        appendLine()
        appendLine("Medicine reminders: ${recent.count { it.status == "TAKEN" }} marked taken, ${recent.count { it.status == "MISSED" }} marked missed")
        appendLine("Upcoming appointments: ${appts.count { it.whenMillis >= now }}")
        appendLine(); appendLine("Self-reported data. General information only, not a medical record.")
    }
}