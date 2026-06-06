package com.pausecard.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pausecard.app.groq.GroqCardGenerator
import com.pausecard.app.permissions.BatteryOptimizationHelper
import com.pausecard.app.permissions.PermissionHelper
import com.pausecard.app.service.AppWatcherService
import com.pausecard.app.ui.theme.PauseCardTheme

private val MONITORABLE_APPS = mapOf(
    "com.instagram.android" to "Instagram",
    "com.instagram.barcelona" to "Threads",
    "com.google.android.youtube" to "YouTube",
    "com.google.android.youtube.music" to "YouTube Music",
    "com.zhiliaoapp.musically" to "TikTok",
    "com.ss.android.ugc.trill" to "TikTok",
    "com.twitter.android" to "X (Twitter)",
    "com.facebook.katana" to "Facebook",
    "com.snapchat.android" to "Snapchat",
    "com.reddit.frontpage" to "Reddit",
    "com.netflix.mediaclient" to "Netflix",
    "com.spotify.music" to "Spotify",
    "com.discord" to "Discord",
)

class MainActivity : ComponentActivity() {

    private var currentStep by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("pausecard_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_complete", false)) {
            launchMainScreen()
            return
        }

        setContent {
            PauseCardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    OnboardingScreen(
                        onStepChanged = { currentStep = it },
                        onComplete = {
                            prefs.edit().putBoolean("onboarding_complete", true).apply()
                            launchMainScreen()
                        }
                    )
                }
            }
        }
    }

    private fun launchMainScreen() {
        AppWatcherService.start(this)
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }
}

@Composable
fun OnboardingScreen(
    onStepChanged: (Int) -> Unit,
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var groqKey by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }

    val context = LocalContext.current

    val installedApps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                val pkg = info.activityInfo.packageName
                val label = MONITORABLE_APPS[pkg]
                    ?: pm.getApplicationLabel(info.activityInfo.applicationInfo).toString()
                pkg to label
            }
            .sortedBy { it.second.lowercase() }
    }

    val selectedApps = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        for ((pkg, _) in installedApps) {
            selectedApps[pkg] = MONITORABLE_APPS.containsKey(pkg)
        }
    }

    LaunchedEffect(step) {
        onStepChanged(step)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (step) {
            0 -> WelcomeStep(onNext = { step = 1 })
            1 -> PermissionStep(
                title = "Usage Access",
                description = "PauseCard needs to detect which apps you open. This requires Usage Access permission.",
                hasPermission = { PermissionHelper.hasUsageStatsPermission(context) },
                onRequest = { PermissionHelper.requestUsageStatsPermission(context) },
                onNext = { step = 2 },
                onSkip = { step = 2 }
            )
            2 -> PermissionStep(
                title = "Display Over Apps",
                description = "PauseCard shows learning cards as an overlay on top of other apps.",
                hasPermission = { PermissionHelper.hasOverlayPermission(context) },
                onRequest = { PermissionHelper.requestOverlayPermission(context) },
                onNext = { step = 3 },
                onSkip = { step = 3 }
            )
            3 -> PermissionStep(
                title = "Notifications",
                description = "PauseCard runs as a foreground service to monitor apps reliably.",
                hasPermission = { PermissionHelper.hasNotificationPermission(context) },
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Notification permission
                    }
                },
                onNext = { step = 4 },
                onSkip = { step = 4 }
            )
            4 -> AppSelectionStep(
                installedApps = installedApps,
                selectedApps = selectedApps,
                onNext = {
                    val prefs = context.getSharedPreferences("pausecard_prefs", Context.MODE_PRIVATE)
                    for ((pkg, selected) in selectedApps) {
                        prefs.edit().putBoolean("app_enabled_$pkg", selected).apply()
                    }
                    step = 5
                }
            )
            5 -> GroqSetupStep(
                groqKey = groqKey,
                onGroqKeyChange = { groqKey = it },
                interests = interests,
                onInterestsChange = { interests = it },
                onNext = {
                    val generator = GroqCardGenerator(context)
                    if (groqKey.isNotBlank()) generator.setGroqKey(groqKey)
                    if (interests.isNotBlank()) generator.setUserInterests(interests)
                    if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                        try {
                            BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(
                                context as ComponentActivity
                            )
                        } catch (_: Exception) {}
                    }
                    onComplete()
                },
                onSkip = { onComplete() }
            )
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "PauseCard",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5CC),
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Instead of scrolling, learn one useful thing.",
            fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "A 15-second micro-learning card appears before addictive apps open.",
            fontSize = 14.sp, color = Color(0xFFB0B0B0), textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5CC), contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AppSelectionStep(
    installedApps: List<Pair<String, String>>,
    selectedApps: MutableMap<String, Boolean>,
    onNext: () -> Unit
) {
    val tealColor = Color(0xFF00E5CC)
    val cardSurface = Color(0xFF141414)

    Column(
        modifier = Modifier.fillMaxSize().padding(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Choose Apps to Block", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Toggle which apps trigger the learning card.",
            fontSize = 13.sp, color = Color(0xFFB0B0B0), textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(installedApps, key = { it.first }) { (pkg, label) ->
                val checked = selectedApps[pkg] ?: false
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (checked) Color(0xFF0A1A16) else cardSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(label, color = if (checked) Color.White else Color(0xFF808080), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Switch(
                            checked = checked,
                            onCheckedChange = { selectedApps[pkg] = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = tealColor,
                                uncheckedThumbColor = Color(0xFF606060),
                                uncheckedTrackColor = Color(0xFF303030)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = tealColor, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            val count = selectedApps.values.count { it }
            Text("Continue ($count apps selected)", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun PermissionStep(
    title: String,
    description: String,
    hasPermission: () -> Boolean,
    onRequest: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    var granted by remember { mutableStateOf(hasPermission()) }

    LaunchedEffect(Unit) { granted = hasPermission() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = null,
            tint = if (granted) Color(0xFF00E5CC) else Color(0xFFFFAB00),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text(description, fontSize = 14.sp, color = Color(0xFFB0B0B0), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            if (granted) "Permission Granted" else "Permission Required",
            fontSize = 14.sp,
            color = if (granted) Color(0xFF00E5CC) else Color(0xFFFF4444),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (!granted) {
            Button(
                onClick = { onRequest() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5CC), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Grant Permission", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onNext) { Text("Skip for now", color = Color(0xFFB0B0B0)) }
        } else {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5CC), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GroqSetupStep(
    groqKey: String,
    onGroqKeyChange: (String) -> Unit,
    interests: String,
    onInterestsChange: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val tealColor = Color(0xFF00E5CC)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Personalize Your Cards", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Enter your Groq API key for fresh AI-generated cards,\nor skip to use built-in cards.", fontSize = 14.sp, color = Color(0xFFB0B0B0), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = groqKey, onValueChange = onGroqKeyChange,
            label = { Text("Groq API Key", color = Color(0xFFB0B0B0)) },
            placeholder = { Text("gsk_...", color = Color(0xFF505050)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = tealColor, unfocusedBorderColor = Color(0xFF333333), cursorColor = tealColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = interests, onValueChange = onInterestsChange,
            label = { Text("Your Interests", color = Color(0xFFB0B0B0)) },
            placeholder = { Text("ECE — signals, op-amps; LLMs; Android dev", color = Color(0xFF505050)) },
            minLines = 3, maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = tealColor, unfocusedBorderColor = Color(0xFF333333), cursorColor = tealColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = tealColor, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Start Using PauseCard", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onSkip) { Text("Skip — use built-in cards", color = Color(0xFFB0B0B0)) }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
