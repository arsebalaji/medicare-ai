package com.medicare.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.medicare.ai.ui.*
import com.medicare.ai.viewmodel.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(nav: NavController, vm: ChatVM) {
    val msgs by vm.messages.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val err by vm.error.collectAsStateWithLifecycle()
    val emergency by vm.emergency.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState(); val snack = remember { SnackbarHostState() }
    var call by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.reload() }
    LaunchedEffect(msgs.size, emergency) { if (msgs.isNotEmpty()) list.animateScrollToItem(list.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1) }
    LaunchedEffect(err) { err?.let { snack.showSnackbar(it); vm.clearError() } }
    if (call) CallConfirm("112", "emergency services") { call = false }

    AppScaffold("Health assistant", nav, back = false, snackbar = snack) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().imePadding()) {
            Disclaimer(Modifier.padding(12.dp))
            LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (msgs.isEmpty()) item {
                    Text("Try asking:", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("How do I prepare for a doctor visit?", "What does hypertension mean?", "Tips for better sleep", "Why is hydration important?")
                            .forEach { s -> SuggestionChip({ vm.send(s) }, { Text(s) }) }
                    }
                }
                items(msgs) { m ->
                    val mine = m.role == "user"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(
                            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            shape = MaterialTheme.shapes.medium, modifier = Modifier.widthIn(max = 300.dp)
                        ) { Text(m.text, Modifier.padding(12.dp)) }
                    }
                }
                if (emergency) item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Possible emergency", fontWeight = FontWeight.Bold)
                            Button({ call = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Icon(Icons.Filled.Call, null); Spacer(Modifier.width(8.dp)); Text("Call 112")
                            }
                            TextButton({ vm.askAnyway() }) { Text("This is a general question. Answer anyway") }
                        }
                    }
                }
                if (busy) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Thinking…") } }
            }
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Ask a general health question") }, maxLines = 4)
                IconButton({ vm.send(input); input = "" }, enabled = input.isNotBlank() && !busy) { Icon(Icons.Filled.Send, "Send message") }
            }
        }
    }
}

@Composable
fun EmergencyScreen(nav: NavController, vm: ProfileVM) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    var call by remember { mutableStateOf<Pair<String, String>?>(null) }
    call?.let { CallConfirm(it.first, it.second) { call = null } }
    val err = MaterialTheme.colorScheme.error
    AppScaffold("Emergency", nav) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Emergency number (India)", style = MaterialTheme.typography.titleMedium)
                    Text("112", style = MaterialTheme.typography.displayLarge, color = err, fontWeight = FontWeight.Bold)
                    Button({ call = "112" to "emergency services" }, Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = err)) {
                        Icon(Icons.Filled.Call, null); Spacer(Modifier.width(8.dp)); Text("Call 112")
                    }
                }
            }
            profile?.takeIf { it.emergencyPhone.isNotBlank() }?.let { p ->
                OutlinedButton({ call = p.emergencyPhone to (p.emergencyName.ifBlank { "your emergency contact" }) }, Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Filled.Person, null); Spacer(Modifier.width(8.dp)); Text("Call ${p.emergencyName.ifBlank { "emergency contact" }}")
                }
            }
            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What to do", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                listOf(
                    "Call 112 first. Don't search online or use the chatbot.",
                    "Say clearly where you are: address, landmark, area.",
                    "Describe what happened and whether the person is awake and breathing.",
                    "Stay on the line and follow the operator's instructions.",
                    "Don't move an injured person unless they are in immediate danger.",
                    "If you can, ask someone to meet the ambulance and guide it in."
                ).forEachIndexed { i, s -> Text("${i + 1}. $s") }
            } }
            Card { Column(Modifier.padding(16.dp)) {
                Text("Mental health support", fontWeight = FontWeight.SemiBold)
                Text("Tele-MANAS: 14416 (please verify the current number on the official government site).", Modifier.padding(vertical = 8.dp))
                OutlinedButton({ call = "14416" to "Tele-MANAS" }) { Text("Call 14416") }
            } }
            Disclaimer(text = "This app cannot send help automatically. Always contact emergency services directly.")
        }
    }
}