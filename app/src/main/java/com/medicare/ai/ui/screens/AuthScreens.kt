package com.medicare.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medicare.ai.AppContainer
import com.medicare.ai.ui.*
import com.medicare.ai.viewmodel.AuthVM
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(c: AppContainer, onDone: (String) -> Unit) {
    LaunchedEffect(Unit) {
        delay(1300)
        val onb = c.prefs.onboarded.first()
        onDone(if (!onb) Route.ONBOARD else if (c.auth.currentUid == null) Route.AUTH else Route.HOME)
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.HealthAndSafety, "MediCare AI logo", Modifier.size(96.dp), tint = Color.White)
            Text("MediCare AI", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Your everyday wellness companion", color = Color.White.copy(alpha = .9f))
        }
    }
}

private data class Page(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val text: String)

@Composable
fun OnboardingScreen(c: AppContainer, onDone: () -> Unit) {
    val pages = listOf(
        Page(Icons.Filled.HealthAndSafety, "Welcome to MediCare AI", "Track water, sleep and activity, keep medicine reminders and prepare for doctor visits, all in one place."),
        Page(Icons.Filled.Psychology, "A helpful, careful assistant", "Ask general health questions in simple language. The assistant never diagnoses, never prescribes and always points you to a doctor when needed."),
        Page(Icons.Filled.Lock, "Your data stays with you", "Health data is stored on this device. Chat history can be switched off or deleted at any time. In an emergency, call 112.")
    )
    val pager = rememberPagerState(pageCount = { pages.size }); val scope = rememberCoroutineScope()
    val finish: () -> Unit = { scope.launch { c.prefs.setOnboarded(); onDone() } }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(finish) { Text("Skip") } }
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(pages[i].icon, null, Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))
                Text(pages[i].title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(pages[i].text, textAlign = TextAlign.Center)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(pages.size) { i ->
                Box(Modifier.size(if (i == pager.currentPage) 12.dp else 8.dp).background(
                    if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.CircleShape))
            }
        }
        Spacer(Modifier.height(16.dp))
        Button({ if (pager.currentPage == pages.lastIndex) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            Modifier.fillMaxWidth().height(52.dp)) { Text(if (pager.currentPage == pages.lastIndex) "Get started" else "Next") }
    }
}

@Composable
fun AuthScreen(vm: AuthVM, onSuccess: () -> Unit) {
    var signUp by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pw by rememberSaveable { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var consent by rememberSaveable { mutableStateOf(false) }
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val info by vm.info.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.HealthAndSafety, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text(if (signUp) "Create your account" else "Welcome back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (signUp) OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Your name") }, singleLine = true)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(pw, { pw = it }, Modifier.fillMaxWidth(), label = { Text("Password (min 6 characters)") }, singleLine = true,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = { IconButton({ show = !show }) { Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (show) "Hide password" else "Show password") } })
        if (signUp) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(consent, { consent = it })
            Text("I understand MediCare AI gives general information only and is not a substitute for a doctor.", style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button({ vm.submit(signUp, name, email, pw, onSuccess) }, enabled = !loading && (!signUp || consent), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (signUp) "Sign up" else "Log in")
        }
        if (!signUp) TextButton({ vm.resetPassword(email) }) { Text("Forgot password?") }
        TextButton({ signUp = !signUp; vm.clear() }) { Text(if (signUp) "Already have an account? Log in" else "New here? Create an account") }
        Disclaimer()
    }
}