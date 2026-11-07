package com.medicare.ai.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.*
import java.time.format.DateTimeFormatter

fun fmtDay(d: Long): String = LocalDate.ofEpochDay(d).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
fun fmtMillis(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • hh:mm a"))
fun hhmm(h: Int, m: Int) = "%02d:%02d".format(h, m)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String, nav: NavController?, back: Boolean = true, snackbar: SnackbarHostState? = null,
    fab: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit
) = Scaffold(
    topBar = {
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.SemiBold) },
            navigationIcon = { if (back && nav != null) IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                if (nav != null) IconButton({ nav.navigate(Route.EMERGENCY) { launchSingleTop = true } }) {
                    Icon(Icons.Filled.Emergency, "Emergency help", tint = MaterialTheme.colorScheme.error)
                }
            })
    },
    snackbarHost = { snackbar?.let { SnackbarHost(it) } },
    floatingActionButton = fab, content = content
)

@Composable
fun Disclaimer(
    modifier: Modifier = Modifier,
    text: String = "MediCare AI shares general health information only. It does not diagnose, prescribe or replace a doctor. In an emergency call 112."
) = Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.Info, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun Ring(progress: Float, title: String, sub: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Card(onClick, modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.size(72.dp), strokeWidth = 8.dp)
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall)
        }
    }

@Composable
fun CallConfirm(number: String, label: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val clean = number.filter { it.isDigit() || it == '+' }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Call $number?") },
        text = { Text("This opens your phone dialer for $label. You still need to press the call button.") },
        confirmButton = { Button({ ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))); onDismiss() }) { Text("Open dialer") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, day: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton({ show = true }, modifier.fillMaxWidth()) {
        Icon(Icons.Filled.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("$label: ${fmtDay(day)}")
    }
    if (show) {
        val st = rememberDatePickerState(initialSelectedDateMillis = LocalDate.ofEpochDay(day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = { TextButton({
                st.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) }; show = false
            }) { Text("OK") } },
            dismissButton = { TextButton({ show = false }) { Text("Cancel") } }
        ) { DatePicker(st) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, time: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton({ show = true }, modifier.fillMaxWidth()) {
        Icon(Icons.Filled.AccessTime, null); Spacer(Modifier.width(8.dp)); Text("$label: $time")
    }
    if (show) {
        val t = LocalTime.parse(time)
        val st = rememberTimePickerState(t.hour, t.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { show = false },
            confirmButton = { TextButton({ onChange(hhmm(st.hour, st.minute)); show = false }) { Text("OK") } },
            dismissButton = { TextButton({ show = false }) { Text("Cancel") } },
            text = { TimePicker(st) }
        )
    }
}