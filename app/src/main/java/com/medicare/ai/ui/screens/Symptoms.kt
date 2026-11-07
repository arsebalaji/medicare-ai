package com.medicare.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.medicare.ai.domain.*
import com.medicare.ai.ui.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomScreen(nav: NavController) {
    var sel by rememberSaveable { mutableIntStateOf(-1) }
    var dur by rememberSaveable { mutableIntStateOf(0) }
    var note by rememberSaveable { mutableStateOf("") }
    var flags by remember { mutableStateOf(setOf<Int>()) }
    var shown by rememberSaveable { mutableStateOf(false) }
    var call by remember { mutableStateOf(false) }
    if (call) CallConfirm("112", "emergency services") { call = false }
    val s = Symptoms.all.getOrNull(sel)

    AppScaffold("Symptom guide", nav) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Disclaimer(text = "Educational information only. This is not a diagnosis and does not show what condition you may have.")
            Text("1. Choose a symptom", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Symptoms.all.forEachIndexed { i, x -> FilterChip(sel == i, { sel = i; flags = emptySet(); shown = false }, { Text(x.name) }) }
            }
            if (s != null) {
                Text("2. How long has it lasted?", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Symptoms.durations.forEachIndexed { i, d -> FilterChip(dur == i, { dur = i; shown = false }, { Text(d.first) }) }
                }
                Text("3. Do any of these apply?", fontWeight = FontWeight.SemiBold)
                s.redFlags.forEachIndexed { i, r ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(i in flags, { flags = if (it) flags + i else flags - i; shown = false }); Text(r)
                    }
                }
                OutlinedTextField(note, { note = it; shown = false }, Modifier.fillMaxWidth(), label = { Text("Anything else to note (optional)") })
                Button({ shown = true }, Modifier.fillMaxWidth().height(52.dp)) { Text("Show guidance") }
            }
            if (s != null && shown) {
                val u = Symptoms.urgency(s, Symptoms.durations[dur].second, flags.isNotEmpty(), note)
                val (title, body, container) = when (u) {
                    Urgency.EMERGENCY -> Triple("Seek emergency care now", "Call 112 or go to the nearest emergency department. Don't wait.", MaterialTheme.colorScheme.errorContainer)
                    Urgency.SOON -> Triple("Please contact a doctor soon", "Because of how long this has lasted, it's a good idea to book a consultation.", MaterialTheme.colorScheme.secondaryContainer)
                    Urgency.MONITOR -> Triple("Keep an eye on it", "Rest, drink fluids and note any changes. Contact a doctor if it gets worse, lasts longer or new symptoms appear.", MaterialTheme.colorScheme.primaryContainer)
                }
                Card(colors = CardDefaults.cardColors(containerColor = container)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(body)
                    if (u == Urgency.EMERGENCY) Button({ call = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Call 112") }
                } }
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("About ${s.name.lowercase()}", fontWeight = FontWeight.SemiBold); Text(s.info)
                    Text("Questions to prepare for your consultation", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    s.questions.forEach { Text("• $it") }
                    Text("This information is general. Only a healthcare professional can assess your situation.", style = MaterialTheme.typography.bodySmall)
                } }
            }
        }
    }
}