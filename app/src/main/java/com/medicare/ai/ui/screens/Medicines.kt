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
import com.medicare.ai.data.Medicine
import com.medicare.ai.ui.*
import com.medicare.ai.viewmodel.*
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun MedicineScreen(nav: NavController, vm: MedVM) {
    val meds by vm.meds.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Medicine?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Medicine?>(null) }

    if (adding || editing != null) MedDialog(editing, { adding = false; editing = null }) { m -> vm.save(m, editing); adding = false; editing = null }
    deleting?.let { m ->
        AlertDialog({ deleting = null }, title = { Text("Delete ${m.name}?") }, text = { Text("Its reminders will be cancelled. Past history is kept.") },
            confirmButton = { TextButton({ vm.delete(m); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancel") } })
    }

    AppScaffold("Medicine reminders", nav, back = false, fab = {
        ExtendedFloatingActionButton({ adding = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add medicine") })
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Disclaimer(text = "This app only reminds you. It doesn't recommend medicines or doses. Follow your doctor's prescription.") }
            item { Text("Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (today.isEmpty()) item { Text("No doses scheduled for today.") }
            items(today, key = { "${it.med.id}-${it.time}" }) { d ->
                val overdue = d.status == "PENDING" && LocalTime.parse(d.time).isBefore(LocalTime.now())
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text("${d.time}  ${d.med.name}", fontWeight = FontWeight.SemiBold)
                    if (d.med.schedule.isNotBlank()) Text(d.med.schedule, style = MaterialTheme.typography.bodySmall)
                    Text(when { d.status == "TAKEN" -> "Taken"; d.status == "MISSED" -> "Missed"; overdue -> "Overdue"; else -> "Upcoming" },
                        color = if (d.status == "MISSED" || overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        FilledTonalButton({ vm.mark(d, "TAKEN") }, enabled = d.status != "TAKEN") { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(4.dp)); Text("Taken") }
                        OutlinedButton({ vm.mark(d, "MISSED") }, enabled = d.status != "MISSED") { Icon(Icons.Filled.Close, null); Spacer(Modifier.width(4.dp)); Text("Missed") }
                        if (d.status != "PENDING") TextButton({ vm.mark(d, "PENDING") }) { Text("Undo") }
                    }
                } }
            }
            item { Text("All medicines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (meds.isEmpty()) item { Text("You haven't added any medicines yet.") }
            items(meds, key = { it.id }) { m ->
                Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name, fontWeight = FontWeight.SemiBold)
                        Text("Times: ${m.times.replace(",", ", ")}", style = MaterialTheme.typography.bodySmall)
                        Text("${fmtDay(m.startDay)} → ${fmtDay(m.endDay)}", style = MaterialTheme.typography.bodySmall)
                        if (m.schedule.isNotBlank()) Text(m.schedule, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton({ editing = m }) { Icon(Icons.Filled.Edit, "Edit ${m.name}") }
                    IconButton({ deleting = m }) { Icon(Icons.Filled.Delete, "Delete ${m.name}") }
                } }
            }
        }
    }
}

@Composable
fun MedDialog(initial: Medicine?, onDismiss: () -> Unit, onSave: (Medicine) -> Unit) {
    val today = LocalDate.now().toEpochDay()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sched by remember { mutableStateOf(initial?.schedule ?: "") }
    var times by remember { mutableStateOf(initial?.times?.split(",") ?: listOf("08:00")) }
    var start by remember { mutableLongStateOf(initial?.startDay ?: today) }
    var end by remember { mutableLongStateOf(initial?.endDay ?: (today + 7)) }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(if (initial == null) "Add medicine" else "Edit medicine") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Medicine name") }, singleLine = true)
            OutlinedTextField(sched, { sched = it }, label = { Text("Prescribed schedule (as written by your doctor)") })
            Text("Reminder times", style = MaterialTheme.typography.labelLarge)
            times.forEachIndexed { i, t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeField("Time", t, { n -> times = times.toMutableList().also { it[i] = n } }, Modifier.weight(1f))
                    if (times.size > 1) IconButton({ times = times.filterIndexed { j, _ -> j != i } }) { Icon(Icons.Filled.Close, "Remove time") }
                }
            }
            TextButton({ times = times + "12:00" }) { Text("+ Add another time") }
            DateField("Start", start, { start = it }); DateField("End", end, { end = it })
            err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { Button({
            when {
                name.isBlank() -> err = "Enter the medicine name."
                end < start -> err = "End date must be on or after the start date."
                times.distinct().size != times.size -> err = "Remove duplicate times."
                else -> onSave(Medicine(id = initial?.id ?: 0, uid = initial?.uid ?: "", name = name.trim(), schedule = sched.trim(),
                    times = times.sorted().joinToString(","), startDay = start, endDay = end))
            }
        }) { Text("Save") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}