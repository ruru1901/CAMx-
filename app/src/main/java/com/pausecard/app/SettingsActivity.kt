package com.pausecard.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pausecard.app.groq.GroqCardGenerator
import com.pausecard.app.permissions.AutoStartHelper
import com.pausecard.app.permissions.BatteryOptimizationHelper
import com.pausecard.app.service.AppWatcherService
import com.pausecard.app.ui.theme.PauseCardTheme

private val DEFAULT_PACKAGES = setOf(
    "com.instagram.android", "com.instagram.barcelona",
    "com.google.android.youtube", "com.google.android.youtube.music",
    "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
    "com.twitter.android", "com.facebook.katana",
    "com.snapchat.android", "com.reddit.frontpage",
    "com.netflix.mediaclient", "com.spotify.music",
    "com.discord", "com.tiktok"
)

private data class AppItem(val packageName: String, val label: String, val isMonitored: Boolean)

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PauseCardTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tealColor = Color(0xFF00E5CC)
    val cardSurface = Color(0xFF141414)
    val prefs = remember { context.getSharedPreferences("pausecard_prefs", Context.MODE_PRIVATE) }
    val generator = remember { GroqCardGenerator(context) }

    var searchText by remember { mutableStateOf("") }
    var showAddApps by remember { mutableStateOf(false) }
    var groqKey by remember { mutableStateOf(generator.getGroqKey()) }
    var interests by remember { mutableStateOf(generator.getUserInterests()) }
    var showGroqEditor by remember { mutableStateOf(false) }
    var noSkipMode by remember { mutableStateOf(prefs.getBoolean("focus_hours_enabled", false)) }

    val allInstalledApps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                val label = pm.getApplicationLabel(info.activityInfo.applicationInfo).toString()
                val pkg = info.activityInfo.packageName
                AppItem(pkg, label, true)
            }
    }

    var monitoredApps by remember {
        val saved = mutableSetOf<String>()
        for (pkg in DEFAULT_PACKAGES) {
            if (prefs.getBoolean("app_enabled_$pkg", true)) {
                saved.add(pkg)
            }
        }
        mutableStateOf(allInstalledApps.map { it.copy(isMonitored = saved.contains(it.packageName)) })
    }

    val filteredApps = remember(searchText, monitoredApps, showAddApps) {
        if (showAddApps) {
            val unmonitored = allInstalledApps.filter { app ->
                monitoredApps.none { it.packageName == app.packageName }
            }
            if (searchText.isBlank()) unmonitored
            else unmonitored.filter { it.label.contains(searchText, ignoreCase = true) || it.packageName.contains(searchText, ignoreCase = true) }
        } else {
            if (searchText.isBlank()) monitoredApps
            else monitoredApps.filter { it.label.contains(searchText, ignoreCase = true) || it.packageName.contains(searchText, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "PauseCard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${monitoredApps.count { it.isMonitored }} apps",
                color = tealColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search apps...", color = Color(0xFF505050)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF808080)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF333333),
                unfocusedBorderColor = Color(0xFF222222),
                cursorColor = tealColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!showAddApps) {
                item {
                    Text(
                        "MONITORED APPS",
                        color = Color(0xFF606060), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            } else {
                item {
                    Text(
                        "ADD APPS",
                        color = Color(0xFF606060), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }

            items(filteredApps, key = { it.packageName }) { app ->
                val isOn = app.isMonitored
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isOn) Color(0xFF0A1A16) else cardSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.label,
                                color = if (isOn) Color.White else Color(0xFF808080),
                                fontSize = 14.sp, fontWeight = FontWeight.Medium
                            )
                            Text(
                                app.packageName,
                                color = Color(0xFF404040), fontSize = 9.sp, fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = isOn,
                            onCheckedChange = { newEnabled ->
                                prefs.edit().putBoolean("app_enabled_${app.packageName}", newEnabled).apply()
                                if (newEnabled) {
                                    monitoredApps = monitoredApps + app.copy(isMonitored = true)
                                } else {
                                    monitoredApps = monitoredApps.map {
                                        if (it.packageName == app.packageName) it.copy(isMonitored = false)
                                        else it
                                    }
                                }
                            },
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

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (showAddApps) Color(0xFF1A2A26) else cardSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable {
                        showAddApps = !showAddApps
                        searchText = ""
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                    ) {
                        Text(
                            if (showAddApps) "← Back to monitored" else "+ Add more apps to monitor",
                            color = tealColor, fontSize = 14.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    "BEHAVIOR",
                    color = Color(0xFF606060), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFFFAB00), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No-Skip Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Continue button hidden — must wait full 15s", color = Color(0xFF808080), fontSize = 11.sp)
                        }
                        Switch(
                            checked = noSkipMode,
                            onCheckedChange = { enabled ->
                                noSkipMode = enabled
                                prefs.edit().putBoolean("focus_hours_enabled", enabled).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFFFFAB00),
                                uncheckedThumbColor = Color(0xFF606060),
                                uncheckedTrackColor = Color(0xFF303030)
                            )
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Text(
                    "GROQ API (OPTIONAL)",
                    color = Color(0xFF606060), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { showGroqEditor = !showGroqEditor }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = tealColor, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Groq API Key", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    if (groqKey.isNotBlank()) "••••••${groqKey.takeLast(4)}" else "Not set — built-in cards active",
                                    color = if (groqKey.isNotBlank()) tealColor else Color(0xFF606060),
                                    fontSize = 11.sp, fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (showGroqEditor) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = groqKey,
                                onValueChange = { groqKey = it },
                                label = { Text("API Key", color = Color(0xFF808080)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = tealColor, unfocusedBorderColor = Color(0xFF333333), cursorColor = tealColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = interests,
                                onValueChange = { interests = it },
                                label = { Text("Interests (comma separated)", color = Color(0xFF808080)) },
                                placeholder = { Text("ECE, LLMs, Android, automation", color = Color(0xFF505050)) },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = tealColor, unfocusedBorderColor = Color(0xFF333333), cursorColor = tealColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    generator.setGroqKey(groqKey)
                                    generator.setUserInterests(interests)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = tealColor, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Text(
                    "SYSTEM",
                    color = Color(0xFF606060), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable {
                        AutoStartHelper.requestAutoStart(context as ComponentActivity)
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFFF4444), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Start", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("For Xiaomi, Oppo, Realme", color = Color(0xFF808080), fontSize = 11.sp)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable {
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context as ComponentActivity)
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = tealColor, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Battery Whitelist", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Keep service alive in background", color = Color(0xFF808080), fontSize = 11.sp)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
