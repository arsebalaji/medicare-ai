package com.medicare.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.medicare.ai.domain.dayTotal
import com.medicare.ai.ui.*
import com.medicare.ai.viewmodel.WellnessVM
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private data class Cfg(val title: String, val unit: String, val icon: ImageVector, val quick: List<Float>)
private fun fmtVal(type: String, v: Float) = if (type == "SLEEP") "%.1f".format(v) else v.toInt().toString()

@Composable
fun TrackerScreen(nav: NavController, vm: WellnessVM, type: String) {
    val cfg = when (type) {
        "WATER" -> Cfg("Water intake", "ml", Icons.Filled.WaterDrop, listOf(150f, 250f, 500f))
        "SLEEP" -> Cfg("Sleep", "hours", Icons.Filled.Bedtime, emptyList())
        else -> Cfg("Activity", "steps", Icons.Filled.DirectionsWalk, emptyList())
    }
    val week by vm.week.collectAsStateWithLifecycle(); val goals by vm.goals.collectAsStateWithLifecycle()
    val goal = when (type) { "WATER" -> goals.water.toFloat(); "SLEEP" -> goals.sleep; else -> goals.steps.toFloat() }
    val today = LocalDate.now().toEpochDay()
    val days = (6 downTo 0).map { today - it }; val values = days.map { week.dayTotal(type, it) }
    val ctx = LocalContext.current; val snack = remember { SnackbarHostState() }
    var input by remember { mutableStateOf("") }; var editGoal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.message.collect { snack.showSnackbar(it) } }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.syncSteps() else vm.say("Permission denied. You can still enter steps manually.")
    }

    if (editGoal) {
        var g by remember { mutableStateOf(fmtVal(type, goal)) }
        AlertDialog({ editGoal = false }, title = { Text("Daily goal (${cfg.unit})") },
            text = { OutlinedTextField(g, { g = it }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) },
            confirmButton = { TextButton({
                val v = g.toFloatOrNull()
                if (v == null || v <= 0f) vm.say("Enter a goal above 0.") else { vm.setGoal(type, v); editGoal = false }
            }) { Text("Save") } },
            dismissButton = { TextButton({ editGoal = false }) { Text("Cancel") } })
    }

    AppScaffold(cfg.title, nav, snackbar = snack) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { (values.last() / goal).coerceIn(0f, 1f) }, modifier = Modifier.size(140.dp), strokeWidth = 12.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(cfg.icon, null, tint = MaterialTheme.colorScheme.primary)
                            Text(fmtVal(type, values.last()), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("of ${fmtVal(type, goal)} ${cfg.unit}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    TextButton({ editGoal = true }) { Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(4.dp)); Text("Edit goal") }
                } }
            }
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add ${cfg.unit}", fontWeight = FontWeight.SemiBold)
                    if (cfg.quick.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cfg.quick.forEach { q -> FilledTonalButton({ vm.add(type, q) }) { Text("+${q.toInt()}") } }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(input, { input = it }, Modifier.weight(1f), singleLine = true, label = { Text("Enter ${cfg.unit}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        Button({
                            val v = input.toFloatOrNull()
                            if (v == null) vm.say("Enter a number.") else { vm.add(type, v); input = "" }
                        }) { Text("Add") }
                    }
                    if (type == "STEPS") OutlinedButton({
                        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
                            perm.launch(Manifest.permission.ACTIVITY_RECOGNITION) else vm.syncSteps()
                    }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Sync, null); Spacer(Modifier.width(8.dp)); Text("Sync phone step counter") }
                    Text("“Manual” values are typed by you. “Device” values come from your phone's step sensor.", style = MaterialTheme.typography.bodySmall)
                } }
            }
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text("Last 7 days", fontWeight = FontWeight.SemiBold)
                    WeekChart(values, days.map { LocalDate.ofEpochDay(it).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }, goal,
                        "Weekly ${cfg.title} chart. " + values.joinToString { fmtVal(type, it) })
                } }
            }
            item { Text("Entries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            val entries = week.filter { it.type == type }.sortedByDescending { it.createdAt }
            if (entries.isEmpty()) item { Text("No entries in the last 7 days.") }
            items(entries, key = { it.id }) { e ->
                ListItem(
                    headlineContent = { Text("${fmtVal(type, e.value)} ${cfg.unit}") },
                    supportingContent = { Text("${if (e.day == today) "Today" else fmtDay(e.day)} • ${if (e.source == "DEVICE") "Device-measured" else "Manual entry"}") },
                    trailingContent = { IconButton({ vm.delete(e.id) }) { Icon(Icons.Filled.Delete, "Delete entry") } })
            }
        }
    }
}

@Composable
fun WeekChart(values: List<Float>, labels: List<String>, goal: Float, description: String) {
    val bar = MaterialTheme.colorScheme.primary; val line = MaterialTheme.colorScheme.secondary
    Canvas(Modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = description }) {
        val max = maxOf(goal, values.maxOrNull() ?: 0f, 1f) * 1.1f
        val w = size.width / values.size
        values.forEachIndexed { i, v ->
            val h = size.height * (v / max)
            drawRoundRect(bar, Offset(i * w + w * 0.2f, size.height - h), Size(w * 0.6f, h), CornerRadius(10f))
        }
        val y = size.height * (1 - goal / max)
        drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
    }
    Row(Modifier.fillMaxWidth()) { labels.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
    Text("Line = your daily goal", style = MaterialTheme.typography.bodySmall)
}