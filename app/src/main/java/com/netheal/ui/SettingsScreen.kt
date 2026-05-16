package com.netheal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.bridge.RustBridge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("netheal_prefs", android.content.Context.MODE_PRIVATE)

    var julesKey by remember { mutableStateOf(prefs.getString("jules_api_key", "") ?: "") }
    var julesActive by remember { mutableStateOf(prefs.getBoolean("jules_api_active", false)) }
    var aiSensitivity by remember { mutableFloatStateOf(prefs.getFloat("ai_sensitivity", 0.5f)) }
    var boosterActive by remember { mutableStateOf(prefs.getBoolean("booster_active", false)) }
    var multipathActive by remember { mutableStateOf(prefs.getBoolean("multipath_active", false)) }
    var shapingMode by remember { mutableIntStateOf(prefs.getInt("shaping_mode", 0)) }
    var bufferSize by remember { mutableIntStateOf(prefs.getInt("buffer_size", 16384)) }
    var batterySafeguard by remember { mutableStateOf(prefs.getBoolean("battery_safeguard", true)) }
    var honeypotActive by remember { mutableStateOf(prefs.getBoolean("honeypot_active", false)) }
    var fingerprintType by remember { mutableIntStateOf(prefs.getInt("fingerprint_type", 0)) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF010409)).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))
        Text("SYSTEM CONFIGURATION", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Jules AI Settings
        SettingSection("JULES AI INTEGRATION") {
            OutlinedTextField(
                value = julesKey,
                onValueChange = {
                    julesKey = it
                    prefs.edit().putString("jules_api_key", it).apply()
                },
                label = { Text("Jules API Key", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                trailingIcon = {
                    IconButton(onClick = {
                        julesActive = !julesActive
                        prefs.edit().putBoolean("jules_api_active", julesActive).apply()
                    }) {
                        Icon(Icons.Default.CloudSync, null, tint = if (julesActive) Color(0xFF00FFA3) else Color.Gray)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI Sensitivity", color = Color.Gray, modifier = Modifier.weight(1f), fontSize = 12.sp)
                Slider(value = aiSensitivity, onValueChange = {
                    aiSensitivity = it
                    prefs.edit().putFloat("ai_sensitivity", it).apply()
                }, modifier = Modifier.weight(2f))
            }
        }

        // Cyber Deception
        SettingSection("CYBER DECEPTION") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Honeypot Mode", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Lure attackers into fake services", color = Color.Gray, fontSize = 10.sp)
                }
                Switch(checked = honeypotActive, onCheckedChange = {
                    honeypotActive = it
                    prefs.edit().putBoolean("honeypot_active", it).apply()
                    RustBridge.setHoneypotMode(it)
                })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Fingerprint Masking", color = Color.Gray, fontSize = 12.sp)
            val masks = listOf("None", "Windows 11", "Linux Kernel", "iOS 17")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                masks.forEachIndexed { index, name ->
                    FilterChip(
                        selected = fingerprintType == index,
                        onClick = {
                            fingerprintType = index
                            prefs.edit().putInt("fingerprint_type", index).apply()
                            RustBridge.setFingerprintMask(index)
                        },
                        label = { Text(name, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00FFA3), selectedLabelColor = Color.Black)
                    )
                }
            }
        }

        // Internet Booster & Link Bonding
        SettingSection("OMEGA SPEED ENGINE") {
            SettingToggle("Internet Speed Boost", boosterActive, "Optimizes TCP/IP for low latency") {
                boosterActive = it
                prefs.edit().putBoolean("booster_active", it).apply()
                RustBridge.setBooster(it)
            }
            SettingToggle("Link Bonding (Multipath)", multipathActive, "Aggregate WiFi + SIM data") {
                multipathActive = it
                prefs.edit().putBoolean("multipath_active", it).apply()
                RustBridge.setMultipath(it)
            }
        }

        // Traffic Shaping
        SettingSection("TRAFFIC SHAPING") {
            Text("Optimization Mode", color = Color.Gray, fontSize = 12.sp)
            val modes = listOf("Default", "Gaming", "Streaming", "Battery")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                modes.forEachIndexed { index, name ->
                    FilterChip(
                        selected = shapingMode == index,
                        onClick = {
                            shapingMode = index
                            prefs.edit().putInt("shaping_mode", index).apply()
                            RustBridge.setShapingMode(index)
                        },
                        label = { Text(name, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00FFA3), selectedLabelColor = Color.Black)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Buffer Size: ${bufferSize/1024}KB", color = Color.Gray, modifier = Modifier.weight(1f), fontSize = 12.sp)
                Slider(value = bufferSize.toFloat(), onValueChange = {
                    bufferSize = it.toInt()
                    prefs.edit().putInt("buffer_size", it.toInt()).apply()
                    RustBridge.setBufferSize(it.toInt())
                }, valueRange = 4096f..65536f, modifier = Modifier.weight(2f))
            }
        }

        // Power Management
        SettingSection("POWER MANAGEMENT") {
            SettingToggle("Battery Safeguard", batterySafeguard, "Reduce CPU polling when battery < 20%") {
                batterySafeguard = it
                prefs.edit().putBoolean("battery_safeguard", it).apply()
                RustBridge.setBatterySafeguard(it)
            }
        }

        // Database & Info
        SettingSection("DATA & KNOWLEDGE") {
            Button(onClick = { RustBridge.clearLogs() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))) {
                Icon(Icons.Default.DeleteSweep, null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp)); Text("PURGE ALL LOGS", color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* Open Wiki */ }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))) {
                Icon(Icons.Default.MenuBook, null, tint = Color(0xFF00FFA3))
                Spacer(modifier = Modifier.width(8.dp)); Text("NEURAL KNOWLEDGE BASE", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text("NetHeal Mobile v2.0 - Omega Build", color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
fun SettingToggle(title: String, checked: Boolean, desc: String, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(desc, color = Color.Gray, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
