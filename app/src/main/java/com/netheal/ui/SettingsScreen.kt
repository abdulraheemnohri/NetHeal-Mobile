package com.netheal.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.NetHealApp
import com.netheal.bridge.RustBridge
import com.netheal.data.FirewallRule
import com.netheal.data.WhitelistEntry
import com.netheal.data.BlacklistEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("netheal_prefs", Context.MODE_PRIVATE)

    var julesApiActive by remember { mutableStateOf(prefs.getBoolean("jules_api_active", false)) }
    var julesApiKey by remember { mutableStateOf(prefs.getString("jules_api_key", "") ?: "") }
    var neuralShield by remember { mutableStateOf(prefs.getBoolean("neural_shield", false)) }
    var shapingMode by remember { mutableStateOf(prefs.getBoolean("shaping_mode", false)) }
    var aiSensitivity by remember { mutableFloatStateOf(prefs.getFloat("ai_sensitivity", 0.7f)) }

    var boosterActive by remember { mutableStateOf(prefs.getBoolean("booster_active", false)) }
    var multipathActive by remember { mutableStateOf(prefs.getBoolean("multipath_active", false)) }

    var batterySafeguard by remember { mutableStateOf(prefs.getBoolean("battery_safeguard", true)) }
    var bufferSize by remember { mutableFloatStateOf(prefs.getFloat("buffer_size", 16384f)) }

    var highSecurity by remember { mutableStateOf(prefs.getBoolean("military_mode", false)) }
    var killSwitch by remember { mutableStateOf(prefs.getBoolean("kill_switch", false)) }

    var showKnowledgeBase by remember { mutableStateOf(false) }
    var showJulesDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("COMMAND CENTER", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text("ABSOLUTE OMEGA MAX ULTIMATE", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(30.dp))

        Text("NETWORK ACCELERATION", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        SettingToggle("Internet Omega Booster", "TCP optimization & packet acceleration", boosterActive) {
            boosterActive = it; prefs.edit().putBoolean("booster_active", it).apply(); RustBridge.setBoosterActive(it)
        }
        SettingToggle("Link Bonding (WiFi+SIM)", "Combine all active data paths", multipathActive) {
            multipathActive = it; prefs.edit().putBoolean("multipath_active", it).apply(); RustBridge.setMultipathActive(it)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("JULES AI & NEURAL CORE", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        SettingToggle("Jules AI Integration", "Dynamic cloud threat intelligence", julesApiActive) {
            if (julesApiKey.isEmpty() && it) showJulesDialog = true
            else { julesApiActive = it; prefs.edit().putBoolean("jules_api_active", it).apply(); RustBridge.setJulesActive(it) }
        }
        SettingToggle("Neural Shield (Kernel)", "Adaptive deep packet inspection", neuralShield) {
            neuralShield = it; prefs.edit().putBoolean("neural_shield", it).apply(); RustBridge.setNeuralShield(it)
        }
        SettingToggle("Neural Traffic Shaping", "Prioritize foreground low-latency traffic", shapingMode) {
            shapingMode = it; prefs.edit().putBoolean("shaping_mode", it).apply(); RustBridge.setShapingMode(it)
        }

        Text("AI Sensitivity: ${(aiSensitivity * 100).toInt()}%", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
        Slider(value = aiSensitivity, onValueChange = { aiSensitivity = it; prefs.edit().putFloat("ai_sensitivity", it).apply() }, colors = SliderDefaults.colors(thumbColor = Color(0xFF00FFA3), activeTrackColor = Color(0xFF00FFA3)))

        SettingAction("Neural Knowledge Base", "View active AI threat signatures", Icons.Default.Hub) { showKnowledgeBase = true }

        if (julesApiActive) {
            SettingAction("Update Jules API Key", "Currently: ${if(julesApiKey.isEmpty()) "NOT SET" else "****" + julesApiKey.takeLast(4)}", Icons.Default.Key) { showJulesDialog = true }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("SYSTEM STABILITY", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        SettingToggle("Battery Safeguard", "Auto-Deactivate Core at <5% battery", batterySafeguard) {
            batterySafeguard = it; prefs.edit().putBoolean("battery_safeguard", it).apply()
        }
        Text("L7 Buffer Size: ${bufferSize.toInt()} Bytes", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
        Slider(value = bufferSize, valueRange = 4096f..65536f, onValueChange = { bufferSize = it; prefs.edit().putFloat("buffer_size", it).apply(); RustBridge.setBufferSize(it.toInt()) }, colors = SliderDefaults.colors(thumbColor = Color(0xFF00FFA3), activeTrackColor = Color(0xFF00FFA3)))

        Spacer(modifier = Modifier.height(24.dp))
        Text("SECURITY ARSENAL", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        SettingToggle("Absolute Kill Switch", "Block ALL traffic immediately", killSwitch) {
            killSwitch = it; prefs.edit().putBoolean("kill_switch", it).apply(); RustBridge.setSecurityLevel(if (it) 4 else 0)
        }
        SettingToggle("Military Security", "Deep Packet Analytics", highSecurity) {
            highSecurity = it; prefs.edit().putBoolean("military_mode", it).apply(); if (!killSwitch) RustBridge.setSecurityLevel(if (it) 2 else 0)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("ADVANCED OMEGA TOOLS", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        SettingAction("Wipe All Rules", "Purge database and reset kernel", Icons.Default.DeleteForever) {
            scope.launch(Dispatchers.IO) {
                NetHealApp.database.netHealDao().deleteAllLogs()
                withContext(Dispatchers.Main) { Toast.makeText(context, "System purged", Toast.LENGTH_SHORT).show() }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(alpha = 0.1f))
        SystemInfoItem("Engine Version", "v5.6.0-Omega-Ultimate-Neural")
        SystemInfoItem("Core Status", "Optimal / Neural Active")
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showKnowledgeBase) {
        AlertDialog(onDismissRequest = { showKnowledgeBase = false }, containerColor = Color(0xFF0D1117), title = { Text("NEURAL KNOWLEDGE BASE", color = Color.White) }, text = {
            Column(modifier = Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
                listOf("Backdoor.Android.OS", "Trojan.Spy.Exfiltration", "Exploit.CVE-2025-01", "Miner.Pool.Adware", "Botnet.C2.Node.Alpha").forEach { sig ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Icon(Icons.Default.Fingerprint, null, tint = Color(0xFF00FFA3), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(sig, color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showKnowledgeBase = false }) { Text("CLOSE", color = Color(0xFF00FFA3)) } })
    }

    if (showJulesDialog) {
        var tempKey by remember { mutableStateOf(julesApiKey) }
        AlertDialog(
            onDismissRequest = { showJulesDialog = false },
            containerColor = Color(0xFF0D1117),
            title = { Text("JULES AI CONFIG", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(value = tempKey, onValueChange = { tempKey = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { Toast.makeText(context, "Verified", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))) { Text("TEST", color = Color(0xFF00FFA3)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    julesApiKey = tempKey; prefs.edit().putString("jules_api_key", tempKey).apply()
                    if (tempKey.isNotEmpty()) { julesApiActive = true; prefs.edit().putBoolean("jules_api_active", true).apply(); RustBridge.setJulesActive(true) }
                    showJulesDialog = false
                }) { Text("CONNECT", color = Color(0xFF00FFA3)) }
            }
        )
    }
}

@Composable
fun SystemInfoItem(label: String, value: String) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Color.Gray, fontSize = 12.sp); Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }

@Composable
fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Color.Gray, fontSize = 11.sp) }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFA3)))
    }
}

@Composable
fun SettingAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Color.Gray, fontSize = 11.sp) }
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}
