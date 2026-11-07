package com.medicare.ai.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.medicare.ai.data.HealthProfile
import com.medicare.ai.domain.buildReport
import com.medicare.ai.ui.*
import com.medicare.ai.viewmodel.*

@Composable
fun ProfileScreen(nav: NavController, vm: ProfileVM) {
    val p by vm.profile.collectAsStateWithLifecycle()
    var age by remember(p) { mutableStateOf(p?.age?.toString() ?: "") }
    var height by remember(p) { mutableStateOf(p?.heightCm?.toString() ?: "") }
    var weight by remember(p) { mutableStateOf(p?.weightKg?.toString() ?: "") }
    var allergies by remember(p) { mutableStateOf(p?.allergies ?: "") }
    var cond by remember(p) { mutableStateOf(p?.conditions ?: "") }
    var ecName by remember(p) { mutableStateOf(p?.emergencyName ?: "") }
    var ecPhone by remember(p) { mutableStateOf(p?.emergencyPhone ?: "") }
    var msg by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val num = KeyboardOptions(keyboardType = KeyboardType.Number)

    if (confirmDelete) AlertDialog({ confirmDelete = false }, title = { Text("Delete health profile?") }, text = { Text("This removes your profile from this device.") },
        confirmButton = { TextButton({ vm.deleteProfile(); confirmDelete = false; msg = "Profile deleted." }) { Text("Delete") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } })

    AppScaffold("Health profile", nav) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Disclaimer(text = "All fields are optional. Details are stored only on this device and are never sent to the AI assistant.")
            OutlinedTextField(age, { age = it }, Modifier.fillMaxWidth(), label = { Text("Age (years)") }, keyboardOptions = num, singleLine = true)
            OutlinedTextField(height, { height = it }, Modifier.fillMaxWidth(), label = { Text("Height (cm)") }, keyboardOptions = num, singleLine = true)
            OutlinedTextField(weight, { weight = it }, Modifier.fillMaxWidth(), label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(allergies, { allergies = it }, Modifier.fillMaxWidth(), label = { Text("Allergies") })
            OutlinedTextField(cond, { cond = it }, Modifier.fillMaxWidth(), label = { Text("Existing medical conditions") })
            OutlinedTextField(ecName, { ecName = it }, Modifier.fillMaxWidth(), label = { Text("Emergency contact name") }, singleLine = true)
            OutlinedTextField(ecPhone, { ecPhone = it }, Modifier.fillMaxWidth(), label = { Text("Emergency contact phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button({
                val a = age.toIntOrNull(); val h = height.toIntOrNull(); val w = weight.toFloatOrNull()
                msg = when {
                    age.isNotBlank() && (a == null || a !in 0..120) -> "Age must be between 0 and 120."
                    height.isNotBlank() && (h == null || h !in 30..260) -> "Height must be between 30 and 260 cm."
                    weight.isNotBlank() && (w == null || w !in 1f..500f) -> "Weight must be between 1 and 500 kg."
                    else -> { vm.save(HealthProfile("", a, h, w, allergies.trim(), cond.trim(), ecName.trim(), ecPhone.trim())); "Saved." }
                }
            }, Modifier.fillMaxWidth().height(52.dp)) { Text("Save profile") }
            OutlinedButton({ confirmDelete = true }, Modifier.fillMaxWidth()) { Text("Delete my profile") }
        }
    }
}

@Composable
fun HistoryScreen(nav: NavController, well: WellnessVM, med: MedVM, appt: ApptVM, prof: ProfileVM) {
    val week by well.week.collectAsStateWithLifecycle(); val logs by med.logs.collectAsStateWithLifecycle()
    val appts by appt.appts.collectAsStateWithLifecycle(); val profile by prof.profile.collectAsStateWithLifecycle()
    var include by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val report = buildReport(week, logs, appts, if (include) profile else null)
    AppScaffold("History & reports", nav) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) { Text(report, Modifier.padding(16.dp)) }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(include, { include = it }); Text("Include my health profile") }
            Button({
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report), "Share summary"))
            }, Modifier.fillMaxWidth().height(52.dp)) { Text("Share summary with my doctor") }
            Text("Recent medicine log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (logs.isEmpty()) Text("Nothing logged yet.")
            logs.take(20).forEach { Text("• ${fmtDay(it.day)} ${it.time} ${it.medicineName}: ${it.status.lowercase()}") }
            Text("Past appointments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val past = appts.filter { it.whenMillis < System.currentTimeMillis() }.takeLast(10)
            if (past.isEmpty()) Text("None yet.")
            past.forEach { Text("• ${fmtMillis(it.whenMillis)}: Dr. ${it.doctor}") }
        }
    }
}

@Composable
fun SettingsScreen(nav: NavController, vm: ProfileVM) {
    val theme by vm.theme.collectAsStateWithLifecycle(); val save by vm.saveChat.collectAsStateWithLifecycle()
    var dlg by remember { mutableStateOf("") }
    if (dlg.isNotEmpty()) AlertDialog({ dlg = "" },
        title = { Text(when (dlg) { "wipe" -> "Delete all my data?"; "chat" -> "Clear chat history?"; else -> "Log out?" }) },
        text = { Text(when (dlg) { "wipe" -> "Profile, reminders, appointments, trackers and chats on this device will be erased. This can't be undone."; "chat" -> "All saved assistant conversations will be deleted."; else -> "Medicine reminders pause until you log in again." }) },
        confirmButton = { TextButton({ when (dlg) { "wipe" -> vm.wipeAll(); "chat" -> vm.clearChat(); else -> vm.logout() }; dlg = "" }) { Text("Confirm") } },
        dismissButton = { TextButton({ dlg = "" }) { Text("Cancel") } })
    AppScaffold("Settings", nav) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System", "Light", "Dark").forEachIndexed { i, l -> FilterChip(theme == i, { vm.setTheme(i) }, { Text(l) }) }
            }
            Text("Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Save assistant chat history"); Text("Chat text you send is processed by the AI server to answer you.", style = MaterialTheme.typography.bodySmall) }
                Switch(save, { vm.setSaveChat(it) })
            }
            OutlinedButton({ dlg = "chat" }, Modifier.fillMaxWidth()) { Text("Clear chat history") }
            OutlinedButton({ dlg = "wipe" }, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete all my data") }
            HorizontalDivider()
            Button({ dlg = "logout" }, Modifier.fillMaxWidth().height(52.dp)) { Text("Log out") }
            Disclaimer(text = "MediCare AI v1.0. General wellness information only. Not a medical device. In an emergency call 112.")
        }
    }
}