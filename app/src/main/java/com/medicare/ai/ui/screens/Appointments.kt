package com.medicare.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.medicare.ai.data.Appointment
import com.medicare.ai.ui.*
import com.medicare.ai.viewmodel.ApptVM
import java.time.*

@Composable
fun AppointmentScreen(nav: NavController, vm: ApptVM) {
    val all by vm.appts.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Appointment?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Appointment?>(null) }
    val now = System.currentTimeMillis()
    val upcoming = all.filter { it.whenMillis >= now }; val past = all.filter { it.whenMillis < now }.reversed()

    if (adding || editing != null) ApptDialog(editing, { adding = false; editing = null }) { a -> vm.save(a); adding = false; editing = null }
    deleting?.let { a ->
        AlertDialog({ deleting = null }, title = { Text("Delete appointment?") }, text = { Text("Dr. ${a.doctor}, ${fmtMillis(a.whenMillis)}") },
            confirmButton = { TextButton({ vm.delete(a); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancel") } })
    }

    @Composable fun row(a: Appointment) = Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Dr. ${a.doctor}", fontWeight = FontWeight.SemiBold)
            if (a.place.isNotBlank()) Text(a.place); Text(fmtMillis(a.whenMillis), style = MaterialTheme.typography.bodySmall)
            if (a.notes.isNotBlank()) Text(a.notes, style = MaterialTheme.typography.bodySmall)
        }
        IconButton({ editing = a }) { Icon(Icons.Filled.Edit, "Edit appointment") }
        IconButton({ deleting = a }) { Icon(Icons.Filled.Delete, "Delete appointment") }
    } }

    AppScaffold("Appointments", nav, back = false, fab = {
        ExtendedFloatingActionButton({ adding = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add") })
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Upcoming", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (upcoming.isEmpty()) item { Text("No upcoming appointments.") }
            items(upcoming, key = { it.id }) { row(it) }
            item { Text("Past", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (past.isEmpty()) item { Text("No past appointments.") }
            items(past, key = { it.id }) { row(it) }
        }
    }
}

@Composable
fun ApptDialog(initial: Appointment?, onDismiss: () -> Unit, onSave: (Appointment) -> Unit) {
    val z = ZoneId.systemDefault()
    val init = initial?.let { Instant.ofEpochMilli(it.whenMillis).atZone(z).toLocalDateTime() } ?: LocalDateTime.now().plusDays(1).withMinute(0)
    var doctor by remember { mutableStateOf(initial?.doctor ?: "") }
    var place by remember { mutableStateOf(initial?.place ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var day by remember { mutableLongStateOf(init.toLocalDate().toEpochDay()) }
    var time by remember { mutableStateOf(hhmm(init.hour, init.minute)) }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "Add appointment" else "Edit appointment") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(doctor, { doctor = it }, label = { Text("Doctor name") }, singleLine = true)
            OutlinedTextField(place, { place = it }, label = { Text("Hospital or clinic") }, singleLine = true)
            DateField("Date", day, { day = it }); TimeField("Time", time, { time = it })
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (questions to ask, documents to bring)") })
            err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { Button({
            if (doctor.isBlank()) err = "Enter the doctor's name." else {
                val ms = LocalDateTime.of(LocalDate.ofEpochDay(day), LocalTime.parse(time)).atZone(z).toInstant().toEpochMilli()
                onSave(Appointment(id = initial?.id ?: 0, uid = initial?.uid ?: "", doctor = doctor.trim(), place = place.trim(), whenMillis = ms, notes = notes.trim()))
            }
        }) { Text("Save") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}