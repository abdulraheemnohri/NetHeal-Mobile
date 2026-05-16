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
    var batterySafeguard by remember { mutableStateOf(prefs.getBoolean("battery_safeguard", true)) }
    var honeypotActive by remember { mutableStateOf(prefs.getBoolean("honeypot_active", false)) }
    var fingerprintType by remember { mutableIntStateOf(prefs.getInt("fingerprint_type", 0)) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))
        Text("SYSTEM CONFIGURATION", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // AI Settings
        SettingCategory("JULES AI CO-PROCESSOR") {
            OutlinedTextField(
                value = julesKey,
                onValueChange = {
                    julesKey = it
                    prefs.edit().putString("jules_api_key", it).apply()
                },
                label = { Text("Jules API Access Key", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberTheme.Primary),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        julesActive = !julesActive
                        prefs.edit().putBoolean("jules_api_active", julesActive).apply()
                        RustBridge.setJulesActive(julesActive)
                    }) {
                        Icon(Icons.Default.CloudSync, null, tint = if (julesActive) CyberTheme.Primary else Color.Gray)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inspection Depth", color = Color.Gray, modifier = Modifier.weight(1f), fontSize = 12.sp)
                Slider(
                    value = aiSensitivity, onValueChange = {
                        aiSensitivity = it
                        prefs.edit().putFloat("ai_sensitivity", it).apply()
                    },
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(thumbColor = CyberTheme.Primary, activeTrackColor = CyberTheme.Primary)
                )
            }
        }

        // Cyber Deception
        SettingCategory("STEALTH & DECEPTION") {
            ModernSettingToggle("Honeypot Mode", honeypotActive, "Lure and trap potential network scans") {
                honeypotActive = it
                prefs.edit().putBoolean("honeypot_active", it).apply()
                RustBridge.setHoneypotMode(it)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("OS Fingerprint Mask", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val masks = listOf("NONE", "WIN-11", "LINUX", "IOS-17")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                masks.forEachIndexed { index, name ->
                    FilterChip(
                        selected = fingerprintType == index,
                        onClick = {
                            fingerprintType = index
                            prefs.edit().putInt("fingerprint_type", index).apply()
                            RustBridge.setFingerprintMask(index)
                        },
                        label = { Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberTheme.Primary, selectedLabelColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Performance
        SettingCategory("OMEGA ENGINE TUNING") {
            ModernSettingToggle("Packet Optimization", boosterActive, "Inject PSH flags for low-latency TCP") {
                boosterActive = it
                prefs.edit().putBoolean("booster_active", it).apply()
                RustBridge.setBoosterActive(it)
            }
            ModernSettingToggle("Link Bonding", multipathActive, "Aggregate WiFi + Cellular data streams") {
                multipathActive = it
                prefs.edit().putBoolean("multipath_active", it).apply()
                RustBridge.setMultipathActive(it)
            }
            ModernSettingToggle("Battery Safeguard", batterySafeguard, "Dynamic scaling for power efficiency") {
                batterySafeguard = it
                prefs.edit().putBoolean("battery_safeguard", it).apply()
                RustBridge.setBatterySafeguard(it)
            }
        }

        // Data
        SettingCategory("DATA MANAGEMENT") {
            CyberButton("PURGE TELEMETRY LOGS", Icons.Default.DeleteSweep, onClick = { RustBridge.clearLogs() }, modifier = Modifier.fillMaxWidth(), isDanger = true)
            Spacer(modifier = Modifier.height(12.dp))
            CyberButton("NEURAL KNOWLEDGE BASE", Icons.Default.MenuBook, onClick = { /* Open Wiki */ }, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text("NetHeal Absolute v2.1.0-OMEGA", color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        GlassCard { content() }
    }
}

@Composable
fun ModernSettingToggle(title: String, checked: Boolean, desc: String, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = Color.Gray, fontSize = 10.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CyberTheme.Primary, checkedTrackColor = CyberTheme.Primary.copy(alpha = 0.5f))
        )
    }
}
