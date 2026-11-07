package com.medicare.ai

import com.medicare.ai.domain.*
import com.medicare.ai.reminders.ReminderScheduler
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class UnitTests {
    private fun ms(dt: LocalDateTime) = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val now = LocalDateTime.of(2026, 11, 7, 9, 0)
    private val day = now.toLocalDate().toEpochDay()

    @Test fun emergencyDetected() = assertTrue(Safety.isEmergency("I have severe chest pain"))
    @Test fun normalQuestionNotEmergency() = assertFalse(Safety.isEmergency("How do I prepare for a doctor visit?"))
    @Test fun laterTodayIsScheduledToday() = assertEquals(ms(LocalDateTime.of(2026, 11, 7, 20, 0)), ReminderScheduler.nextTrigger(day, day + 5, "20:00", now))
    @Test fun passedTimeGoesToTomorrow() = assertEquals(ms(LocalDateTime.of(2026, 11, 8, 8, 0)), ReminderScheduler.nextTrigger(day, day + 5, "08:00", now))
    @Test fun afterEndDateIsNull() = assertNull(ReminderScheduler.nextTrigger(day, day, "08:00", now))
    @Test fun futureStartWaits() = assertEquals(ms(LocalDateTime.of(2026, 11, 10, 8, 0)), ReminderScheduler.nextTrigger(day + 3, day + 9, "08:00", now))
    @Test fun redFlagMeansEmergency() = assertEquals(Urgency.EMERGENCY, Symptoms.urgency(Symptoms.all[0], 0, true, ""))
    @Test fun longFeverMeansSeeDoctor() = assertEquals(Urgency.SOON, Symptoms.urgency(Symptoms.all[1], 4, false, ""))
    @Test fun mildShortIsMonitor() = assertEquals(Urgency.MONITOR, Symptoms.urgency(Symptoms.all[0], 0, false, ""))
}