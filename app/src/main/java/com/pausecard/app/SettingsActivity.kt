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
    var groqKey by remember { mutableStateOf(generator.getGroqKey()) }
    var interests by remember { mutableStateOf(generator.getUserInterests()) }
    var showGroqEditor by remember { mutableStateOf(false) }
    var noSkipMode by remember { mutableStateOf(prefs.getBoolean("focus_hours_enabled", false)) }

    val allApps = remember {
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
                val isDefault = DEFAULT_PACKAGES.contains(pkg)
                val isMonitored = prefs.getBoolean("app_enabled_$pkg", isDefault)
                AppItem(pkg, label, isMonitored)
            }
            .sortedWith(compareByDescending<AppItem> { it.isMonitored }.thenBy { it.label.lowercase() })
    }

    var apps by remember { mutableStateOf(allApps) }

    val filteredApps = remember(searchText, apps) {
        if (searchText.isBlank()) apps
        else apps.filter {
            it.label.contains(searchText, ignoreCase = true) ||
                    it.packageName.contains(searchText, ignoreCase = true)
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
                text = "${apps.count { it.isMonitored }} active",
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    "ALL INSTALLED APPS",
                    color = Color(0xFF606060), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            items(filteredApps, key = { it.packageName }) { app ->
                val isOn = app.isMonitored
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isOn) Color(0xFF0A1A16) else cardSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.label,
                                color = if (isOn) Color.White else Color(0xFF808080),
                                fontSize = 13.sp, fontWeight = FontWeight.Medium
                            )
                            Text(
                                app.packageName,
                                color = Color(0xFF353535), fontSize = 9.sp, fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = isOn,
                            onCheckedChange = { newEnabled ->
                                prefs.edit().putBoolean("app_enabled_${app.packageName}", newEnabled).apply()
                                apps = apps.map {
                                    if (it.packageName == app.packageName) it.copy(isMonitored = newEnabled)
                                    else it
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
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFFFAB00), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No-Skip Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Must wait full 15 seconds", color = Color(0xFF808080), fontSize = 10.sp)
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { showGroqEditor = !showGroqEditor }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = tealColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Groq API Key", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    if (groqKey.isNotBlank()) "••••••${groqKey.takeLast(4)}" else "Not set — built-in cards only",
                                    color = if (groqKey.isNotBlank()) tealColor else Color(0xFF606060),
                                    fontSize = 10.sp, fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (showGroqEditor) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = groqKey, onValueChange = { groqKey = it },
                                label = { Text("API Key", color = Color(0xFF808080)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = tealColor, unfocusedBorderColor = Color(0xFF333333), cursorColor = tealColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = interests, onValueChange = { interests = it },
                                label = { Text("Interests (comma separated)", color = Color(0xFF808080)) },
                                placeholder = { Text("ECE, LLMs, Android, automation", color = Color(0xFF505050)) },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = tealColor, unfocusedBorderColor = Color(0xFF333333), cursorColor = tealColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    generator.setGroqKey(groqKey)
                                    generator.setUserInterests(interests)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = tealColor, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
                            ) { Text("Save", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Text("SYSTEM", color = Color(0xFF606060), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = cardSurface), shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { AutoStartHelper.requestAutoStart(context as ComponentActivity) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFFF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Start", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("For Xiaomi, Oppo, Realme", color = Color(0xFF808080), fontSize = 10.sp)
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = cardSurface), shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context as ComponentActivity) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = tealColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Battery Whitelist", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Keep service alive", color = Color(0xFF808080), fontSize = 10.sp)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
