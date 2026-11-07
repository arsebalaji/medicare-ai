package com.medicare.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.medicare.ai.domain.dayTotal
import com.medicare.ai.ui.*
import com.medicare.ai.viewmodel.*
import java.time.LocalDate

@Composable
fun HomeScreen(nav: NavController, auth: AuthVM, well: WellnessVM, med: MedVM, appt: ApptVM) {
    val name by auth.name.collectAsStateWithLifecycle()
    val week by well.week.collectAsStateWithLifecycle()
    val goals by well.goals.collectAsStateWithLifecycle()
    val doses by med.today.collectAsStateWithLifecycle()
    val appts by appt.appts.collectAsStateWithLifecycle()
    val today = LocalDate.now().toEpochDay()
    val water = week.dayTotal("WATER", today); val sleep = week.dayTotal("SLEEP", today); val steps = week.dayTotal("STEPS", today)
    val upcomingAppts = appts.filter { it.whenMillis >= System.currentTimeMillis() }.take(2)
    val pendingDoses = doses.filter { it.status == "PENDING" }.take(3)

    AppScaffold("MediCare AI", nav, back = false) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Hello, ${name.substringBefore(' ').ifBlank { "there" }}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Today's wellness summary")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Ring(water / goals.water, "${water.toInt()} ml", "of ${goals.water} ml", Icons.Filled.WaterDrop, Modifier.weight(1f)) { nav.go(Route.WATER) }
                    Ring(sleep / goals.sleep, "%.1f h".format(sleep), "of %.0f h".format(goals.sleep), Icons.Filled.Bedtime, Modifier.weight(1f)) { nav.go(Route.SLEEP) }
                    Ring(steps / goals.steps, "${steps.toInt()}", "of ${goals.steps} steps", Icons.Filled.DirectionsWalk, Modifier.weight(1f)) { nav.go(Route.STEPS) }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Psychology, null); Spacer(Modifier.width(8.dp))
                            Text("Health assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Ask general health questions, understand medical terms or prepare for a doctor visit.", Modifier.padding(vertical = 8.dp))
                        Button({ nav.go(Route.CHAT) }) { Text("Ask the assistant") }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text("Upcoming medicine reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (pendingDoses.isEmpty()) Text("No pending doses for today.", Modifier.padding(top = 8.dp))
                    pendingDoses.forEach { Text("• ${it.time}  ${it.med.name}", Modifier.padding(top = 6.dp)) }
                    TextButton({ nav.go(Route.MEDS) }) { Text("Open reminders") }
                } }
            }
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text("Upcoming appointments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (upcomingAppts.isEmpty()) Text("No upcoming appointments.", Modifier.padding(top = 8.dp))
                    upcomingAppts.forEach { Text("• Dr. ${it.doctor}, ${fmtMillis(it.whenMillis)}", Modifier.padding(top = 6.dp)) }
                    TextButton({ nav.go(Route.APPTS) }) { Text("Manage appointments") }
                } }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Emergency, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(12.dp))
                        Text("In an emergency, call 112", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Button({ nav.go(Route.EMERGENCY) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Help") }
                    }
                }
            }
            item { Disclaimer() }
        }
    }
}

@Composable
fun MoreScreen(nav: NavController) {
    data class Item(val label: String, val sub: String, val icon: ImageVector, val route: String)
    val items = listOf(
        Item("Water intake", "Track millilitres", Icons.Filled.WaterDrop, Route.WATER),
        Item("Sleep", "Track hours", Icons.Filled.Bedtime, Route.SLEEP),
        Item("Activity", "Steps and walking", Icons.Filled.DirectionsWalk, Route.STEPS),
        Item("Symptom guide", "General information", Icons.Filled.Search, Route.SYMPTOMS),
        Item("Health profile", "Optional personal details", Icons.Filled.Person, Route.PROFILE),
        Item("History & reports", "Summary you can share", Icons.Filled.History, Route.HISTORY),
        Item("Emergency", "Call 112", Icons.Filled.Emergency, Route.EMERGENCY),
        Item("Settings", "Theme, privacy, log out", Icons.Filled.Settings, Route.SETTINGS)
    )
    AppScaffold("More", nav, back = false) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(items.size) { i ->
                val it = items[i]
                ListItem(headlineContent = { Text(it.label) }, supportingContent = { Text(it.sub) },
                    leadingContent = { Icon(it.icon, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.padding(horizontal = 8.dp).clickable { nav.go(it.route) },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) })
            }
        }
    }
}