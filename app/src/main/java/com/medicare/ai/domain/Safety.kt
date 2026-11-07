package com.medicare.ai.domain

object Safety {
    // Conservative keyword screen. Runs on-device BEFORE any network call.
    private val patterns = listOf(
        "chest pain", "chest tightness", "heart attack", "can't breathe", "cannot breathe",
        "trouble breathing", "difficulty breathing", "severe bleeding", "bleeding heavily",
        "unconscious", "not responding", "passed out", "fainted", "having a stroke",
        "face drooping", "slurred speech", "seizure", "suicid", "kill myself", "end my life",
        "overdose", "poisoning", "anaphyla", "vomiting blood", "coughing blood", "severe allergic"
    )
    fun isEmergency(text: String): Boolean {
        val s = text.lowercase().replace("’", "'")
        return patterns.any { it in s }
    }
    const val EMERGENCY_REPLY =
        "This may be an emergency. Please call 112 now or go to the nearest emergency department. " +
        "Don't wait for an online answer. If you are thinking about harming yourself, call 112 or " +
        "Tele-MANAS (14416) right away."
}

data class Symptom(
    val name: String, val info: String, val questions: List<String>,
    val redFlags: List<String>, val seeDoctorAfterDays: Int
)
enum class Urgency { EMERGENCY, SOON, MONITOR }

object Symptoms {
    /** minDays = lower bound of the chosen duration (0, 1, 4 or 8). No probabilities, no diagnosis. */
    fun urgency(s: Symptom, minDays: Int, redFlag: Boolean, note: String) = when {
        redFlag || Safety.isEmergency(note) -> Urgency.EMERGENCY
        minDays >= s.seeDoctorAfterDays -> Urgency.SOON
        else -> Urgency.MONITOR
    }
    val durations = listOf("Less than 1 day" to 0, "1–3 days" to 1, "4–7 days" to 4, "More than 1 week" to 8)

    val all = listOf(
        Symptom("Headache",
            "Headaches are common and can be linked to tension, dehydration, poor sleep or eye strain. Most are not serious, but a few need urgent care.",
            listOf("When did it start and how often does it happen?", "Where is the pain and what does it feel like?", "What makes it better or worse?", "Which medicines have you taken? (bring the list)"),
            listOf("Sudden, severe \"worst ever\" headache", "Headache after a head injury", "Weakness, numbness, confusion, or trouble speaking or seeing", "Stiff neck with fever"), 3),
        Symptom("Fever",
            "Fever is a body temperature higher than usual and is often a sign that the body is fighting an infection.",
            listOf("What temperature did you measure, and how?", "Any cough, sore throat, rash, diarrhoea or burning urine?", "Recent travel, mosquito bites or contact with sick people?", "Do you have any long-term conditions?"),
            listOf("Fever in a baby under 3 months", "Difficulty breathing", "Confusion or extreme drowsiness", "Stiff neck or seizure", "Unable to drink fluids"), 3),
        Symptom("Cough",
            "Cough is a reflex that clears the airways. Short coughs often follow colds; longer ones can have other causes such as allergies, asthma or reflux.",
            listOf("Is it dry or with mucus?", "Is it worse at night or after exercise?", "Any fever, wheeze or weight loss?", "Do you smoke or have dust or smoke exposure?"),
            listOf("Difficulty breathing or wheezing at rest", "Coughing up blood", "Chest pain", "Blue lips or face"), 7),
        Symptom("Sore throat",
            "Sore throats are often caused by viral infections and usually improve on their own, but some infections need treatment.",
            listOf("Is it painful to swallow?", "Any fever or swollen glands?", "Any cough or runny nose?", "Have you been near someone with a throat infection?"),
            listOf("Trouble breathing or swallowing saliva", "Drooling or not being able to open the mouth", "Swelling of the face or neck"), 4),
        Symptom("Stomach pain",
            "Abdominal pain has many causes, from indigestion and gas to infections. Its location and pattern help doctors understand it.",
            listOf("Where exactly is the pain, and does it move?", "Is it related to meals, nausea, vomiting or diarrhoea?", "Any change in bowel habits?", "Anything that triggers or relieves it?"),
            listOf("Sudden or severe pain", "Vomiting blood or black stools", "Hard, rigid belly", "Pain with fever and unable to keep fluids down", "Pain spreading to chest, arm or jaw"), 2),
        Symptom("Fatigue",
            "Tiredness can come from poor sleep, stress, diet, infections or health conditions. Persistent unexplained tiredness is worth discussing.",
            listOf("How is your sleep?", "Any change in appetite or weight?", "Any mood changes or stress?", "Which medicines or supplements do you take?"),
            listOf("Fainting", "Chest pain or palpitations with breathlessness", "Thoughts of self-harm"), 8),
        Symptom("Dizziness",
            "Dizziness can follow dehydration, standing up quickly, inner-ear problems or low blood sugar, and sometimes other conditions.",
            listOf("Is it spinning or light-headedness?", "Does it happen when changing position?", "Any hearing change or ringing?", "Did you eat and drink normally today?"),
            listOf("Fainting", "Chest pain or fast, irregular heartbeat", "Sudden weakness, face droop or slurred speech", "Sudden severe headache or loss of vision"), 4),
        Symptom("Skin rash",
            "Rashes can come from allergies, infections, irritants or skin conditions. Appearance, location and timing are helpful details.",
            listOf("When did it appear and is it spreading?", "Is it itchy or painful?", "Any new food, soap, medicine or plant contact?", "Any fever or other symptoms?"),
            listOf("Rash with swelling of lips, face or tongue, or trouble breathing", "Rash with high fever", "Purple or blood-coloured spots", "Blisters in the mouth or eyes"), 4)
    )
}