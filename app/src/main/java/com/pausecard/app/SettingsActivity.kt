package com.pausecard.app

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pausecard.app.data.db.PauseCardDatabase
import com.pausecard.app.data.entity.InterceptedAppEntity
import com.pausecard.app.groq.GroqCardGenerator
import com.pausecard.app.permissions.AutoStartHelper
import com.pausecard.app.permissions.BatteryOptimizationHelper
import com.pausecard.app.ui.theme.PauseCardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val coroutineScope = rememberCoroutineScope()
    val tealColor = Color(0xFF00E5CC)
    val darkSurface = Color(0xFF0A0A0A)
    val cardSurface = Color(0xFF141414)

    val db = remember { PauseCardDatabase.getInstance(context) }
    val appDao = remember { db.interceptedAppDao() }
    val generator = remember { GroqCardGenerator(context) }

    var apps by remember { mutableStateOf<List<InterceptedAppEntity>>(emptyList()) }
    var groqKey by remember { mutableStateOf(generator.getGroqKey()) }
    var interests by remember { mutableStateOf(generator.getUserInterests()) }
    var showGroqEditor by remember { mutableStateOf(false) }
    var focusHoursEnabled by remember {
        mutableStateOf(context.getSharedPreferences("pausecard_prefs", 0).getBoolean("focus_hours_enabled", false))
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            apps = appDao.getAllAppsList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Service Status
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = tealColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Service Running",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "PauseCard monitors app launches in the background",
                            color = Color(0xFF808080),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Monitored Apps
            item {
                Text(
                    "Monitored Apps",
                    color = Color(0xFF808080),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            items(apps) { app ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.appLabel,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                app.packageName,
                                color = Color(0xFF606060),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = app.isEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        appDao.setAppEnabled(app.packageName, enabled)
                                        apps = appDao.getAllAppsList()
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

            // Focus Hours
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFFFFAB00),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Focus Hours",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "During focus hours, Continue button is hidden",
                                color = Color(0xFF808080),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = focusHoursEnabled,
                            onCheckedChange = { enabled ->
                                focusHoursEnabled = enabled
                                context.getSharedPreferences("pausecard_prefs", 0)
                                    .edit()
                                    .putBoolean("focus_hours_enabled", enabled)
                                    .apply()
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

            // Groq Configuration
            item {
                Text(
                    "AI Configuration",
                    color = Color(0xFF808080),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { showGroqEditor = !showGroqEditor }
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = tealColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Groq API Key",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (groqKey.isNotBlank()) "••••••${groqKey.takeLast(4)}" else "Not set",
                                    color = if (groqKey.isNotBlank()) tealColor else Color(0xFF606060),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (showGroqEditor) {
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = groqKey,
                                onValueChange = { groqKey = it },
                                label = { Text("API Key", color = Color(0xFF808080)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tealColor,
                                    unfocusedBorderColor = Color(0xFF333333),
                                    cursorColor = tealColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = interests,
                                onValueChange = { interests = it },
                                label = { Text("Interests", color = Color(0xFF808080)) },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tealColor,
                                    unfocusedBorderColor = Color(0xFF333333),
                                    cursorColor = tealColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    generator.setGroqKey(groqKey)
                                    generator.setUserInterests(interests)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tealColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Auto-Start
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable {
                        AutoStartHelper.requestAutoStart(context as ComponentActivity)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Auto-Start Permission",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Required for Xiaomi, Oppo, Realme devices",
                                color = Color(0xFF808080),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Battery Optimization
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable {
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(
                            context as ComponentActivity
                        )
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = Color(0xFF00E5CC),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Battery Optimization",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Whitelist PauseCard from battery optimization",
                                color = Color(0xFF808080),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
