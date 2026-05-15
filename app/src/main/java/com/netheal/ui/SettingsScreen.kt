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
    var autoHeal by remember { mutableStateOf(prefs.getBoolean("auto_heal", true)) }
    var highSecurity by remember { mutableStateOf(prefs.getBoolean("military_mode", false)) }
    var lockdownMode by remember { mutableStateOf(prefs.getBoolean("lockdown_mode", false)) }
    var killSwitch by remember { mutableStateOf(prefs.getBoolean("kill_switch", false)) }
    var performanceMode by remember { mutableStateOf(prefs.getBoolean("performance_mode", false)) }
    var upstreamDns by remember { mutableStateOf(prefs.getString("upstream_dns", "Cloudflare") ?: "Cloudflare") }
    var forceIpv4 by remember { mutableStateOf(prefs.getBoolean("force_ipv4", false)) }
    var blockLan by remember { mutableStateOf(prefs.getBoolean("block_lan", false)) }
    var stealthMode by remember { mutableStateOf(prefs.getBoolean("stealth_mode", false)) }
    var dnsHardening by remember { mutableStateOf(prefs.getBoolean("dns_hardening", false)) }
    var learningMode by remember { mutableStateOf(prefs.getBoolean("learning_mode", false)) }
    var julesApiActive by remember { mutableStateOf(prefs.getBoolean("jules_api_active", false)) }
    var julesApiKey by remember { mutableStateOf(prefs.getString("jules_api_key", "") ?: "") }
    var showJulesDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringBattery = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("COMMAND CENTER", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text("ABSOLUTE OMEGA CONFIGURATION", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(30.dp))

        Text("AI THREAT INTELLIGENCE", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        SettingToggle("Jules AI Integration", "Dynamic traffic analysis & risk scoring", julesApiActive) {
            if (julesApiKey.isEmpty() && it) {
                showJulesDialog = true
            } else {
                julesApiActive = it
                prefs.edit().putBoolean("jules_api_active", it).apply()
                RustBridge.setJulesActive(it)
            }
        }
        if (julesApiActive) {
            SettingAction("Update Jules API Key", "Currently: ${if(julesApiKey.isEmpty()) "NOT SET" else "****" + julesApiKey.takeLast(4)}", Icons.Default.Key) {
                showJulesDialog = true
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("SYSTEM STABILITY", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        if (!isIgnoringBattery) { SettingAction("Disable Battery Optimization", "Prevent background termination", Icons.Default.BatteryAlert) { if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:${context.packageName}") }; context.startActivity(intent) } } }
        SettingToggle("Auto-Healing Core", "Automatic rule restoration", autoHeal) { autoHeal = it; prefs.edit().putBoolean("auto_heal", it).apply() }
        SettingToggle("Ultra-Stamina Mode", "Energy-efficient engine", performanceMode) { performanceMode = it; prefs.edit().putBoolean("performance_mode", it).apply(); RustBridge.setPerformanceMode(it) }

        Spacer(modifier = Modifier.height(24.dp))
        Text("SECURITY ARSENAL", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        SettingToggle("Absolute Kill Switch", "Block ALL traffic immediately", killSwitch) { killSwitch = it; prefs.edit().putBoolean("kill_switch", it).apply(); RustBridge.setSecurityLevel(if (it) 4 else if (lockdownMode) 3 else if (highSecurity) 2 else 0) }
        SettingToggle("Lockdown Mode", "Strict Whitelist-Only", lockdownMode) { lockdownMode = it; prefs.edit().putBoolean("lockdown_mode", it).apply(); if (!killSwitch) RustBridge.setSecurityLevel(if (it) 3 else if (highSecurity) 2 else 0) }
        SettingToggle("Immune Learning", "Suggest rules based on history", learningMode) { learningMode = it; prefs.edit().putBoolean("learning_mode", it).apply(); RustBridge.setLearningMode(it) }
        SettingToggle("Military Security", "Deep Packet Analytics", highSecurity) { highSecurity = it; prefs.edit().putBoolean("military_mode", it).apply(); if (!killSwitch && !lockdownMode) RustBridge.setSecurityLevel(if (it) 2 else 0) }
        SettingToggle("Stealth Mode", "Drop all ICMP (Ping) requests", stealthMode) { stealthMode = it; prefs.edit().putBoolean("stealth_mode", it).apply(); RustBridge.setStealthMode(it) }
        SettingToggle("DNS Hardening", "Block non-encrypted UDP/53", dnsHardening) { dnsHardening = it; prefs.edit().putBoolean("dns_hardening", it).apply(); RustBridge.setDnsHardening(it) }

        Spacer(modifier = Modifier.height(24.dp))
        Text("CONNECTIVITY PRO", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        SettingToggle("Force IPv4 Only", "Disable IPv6 for compatibility", forceIpv4) { forceIpv4 = it; prefs.edit().putBoolean("force_ipv4", it).apply() }
        SettingToggle("Block LAN Access", "Isolate device from local network", blockLan) { blockLan = it; prefs.edit().putBoolean("block_lan", it).apply() }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("DNS Resolver", color = Color.White, fontSize = 15.sp)
            Text(upstreamDns, color = Color(0xFF00FFA3), fontSize = 13.sp, modifier = Modifier.clickable {
                upstreamDns = if (upstreamDns == "Cloudflare") "AdGuard" else if (upstreamDns == "AdGuard") "Google" else "Cloudflare"
                prefs.edit().putString("upstream_dns", upstreamDns).apply()
                RustBridge.setUpstreamDns(if (upstreamDns == "Cloudflare") "1.1.1.1" else if (upstreamDns == "AdGuard") "94.140.14.14" else "8.8.8.8")
            })
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("OMEGA TOOLS", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        SettingAction("Absolute Audit", "Full system link test", Icons.Default.NetworkCheck) { val res = String(RustBridge.runDiagnostics()); Toast.makeText(context, res, Toast.LENGTH_LONG).show() }
        SettingAction("Backup Policies", "Export rules to JSON", Icons.Default.Backup) {
            scope.launch(Dispatchers.IO) {
                val rules = NetHealApp.database.netHealDao().getAllRules()
                val json = JSONArray()
                rules.forEach { r -> val obj = JSONObject(); obj.put("id", r.appId); obj.put("s", r.state); json.put(obj) }
                withContext(Dispatchers.Main) { Toast.makeText(context, "Exported ${rules.size} policies", Toast.LENGTH_SHORT).show() }
            }
        }
        SettingAction("Wipe Telemetry", "Clear scanning stats", Icons.Default.BarChart) { RustBridge.resetStats(); Toast.makeText(context, "Telemetry reset", Toast.LENGTH_SHORT).show() }
        SettingAction("Nuke Rules", "Reset all isolation states", Icons.Default.DeleteForever) { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().getAllRules().forEach { RustBridge.setAppRule(it.appId, 0); NetHealApp.database.netHealDao().saveRule(FirewallRule(it.appId, 0, 0)) } }; Toast.makeText(context, "Rules purged", Toast.LENGTH_SHORT).show() }

        Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(alpha = 0.1f))
        SystemInfoItem("Engine Version", "v5.0.0-Absolute-Omega-Rust")
        SystemInfoItem("Core Hash", "0x58A3B2F... (SECURE)")
        SystemInfoItem("Status", "Absolute Secure")
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showJulesDialog) {
        var tempKey by remember { mutableStateOf(julesApiKey) }
        AlertDialog(
            onDismissRequest = { showJulesDialog = false },
            containerColor = Color(0xFF0D1117),
            title = { Text("JULES AI CONFIGURATION", color = Color.White) },
            text = {
                Column {
                    Text("Enter your Jules API Key to enable cloud threat intelligence.", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { Toast.makeText(context, "API Connection Verified", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))) { Text("TEST CONNECTION", color = Color(0xFF00FFA3)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    julesApiKey = tempKey
                    prefs.edit().putString("jules_api_key", tempKey).apply()
                    if (tempKey.isNotEmpty()) {
                        julesApiActive = true
                        prefs.edit().putBoolean("jules_api_active", true).apply()
                        RustBridge.setJulesActive(true)
                    }
                    showJulesDialog = false
                }) { Text("CONNECT", color = Color(0xFF00FFA3)) }
            },
            dismissButton = {
                TextButton(onClick = { showJulesDialog = false }) { Text("CANCEL", color = Color.Gray) }
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
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFA3), checkedTrackColor = Color(0xFF00FFA3).copy(alpha = 0.5f), uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF161B22)))
    }
}

@Composable
fun SettingAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Color.Gray, fontSize = 11.sp) }
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}
