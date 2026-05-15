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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("netheal_prefs", Context.MODE_PRIVATE)
    var autoHeal by remember { mutableStateOf(prefs.getBoolean("auto_heal", true)) }
    var highSecurity by remember { mutableStateOf(prefs.getBoolean("military_mode", false)) }
    var lockdownMode by remember { mutableStateOf(prefs.getBoolean("lockdown_mode", false)) }
    var killSwitch by remember { mutableStateOf(prefs.getBoolean("kill_switch", false)) }
    var killSwitchAuto by remember { mutableStateOf(prefs.getBoolean("kill_switch_auto", true)) }
    var stealthMode by remember { mutableStateOf(prefs.getBoolean("stealth_mode", false)) }
    var upstreamDns by remember { mutableStateOf(prefs.getString("upstream_dns", "Cloudflare") ?: "Cloudflare") }
    val scope = rememberCoroutineScope()
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringBattery = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("COMMAND CENTER", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text("ABSOLUTE OMEGA CONFIGURATION", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(30.dp))

        Text("SYSTEM STABILITY", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        if (!isIgnoringBattery) { SettingAction("Disable Battery Optimization", "Prevent background termination", Icons.Default.BatteryAlert) { if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:${context.packageName}") }; context.startActivity(intent) } } }
        SettingToggle("Auto-Healing Core", "Automatic rule restoration", autoHeal) { autoHeal = it; prefs.edit().putBoolean("auto_heal", it).apply() }
        SettingToggle("Stealth Mode", "Hide protection presence", stealthMode) { stealthMode = it; prefs.edit().putBoolean("stealth_mode", it).apply() }

        Spacer(modifier = Modifier.height(24.dp))
        Text("SECURITY ARSENAL", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        SettingToggle("Absolute Kill Switch", "Block ALL traffic immediately", killSwitch) { killSwitch = it; prefs.edit().putBoolean("kill_switch", it).apply(); RustBridge.setSecurityLevel(if (it) 4 else if (lockdownMode) 3 else if (highSecurity) 2 else 0) }
        SettingToggle("Kill Switch Auto-Trigger", "Active if VPN is disrupted", killSwitchAuto) { killSwitchAuto = it; prefs.edit().putBoolean("kill_switch_auto", it).apply() }
        SettingToggle("Lockdown Mode", "Strict Whitelist-Only", lockdownMode) { lockdownMode = it; prefs.edit().putBoolean("lockdown_mode", it).apply(); if (!killSwitch) RustBridge.setSecurityLevel(if (it) 3 else if (highSecurity) 2 else 0) }
        SettingToggle("Military Security", "Deep Packet Analytics", highSecurity) { highSecurity = it; prefs.edit().putBoolean("military_mode", it).apply(); if (!killSwitch && !lockdownMode) RustBridge.setSecurityLevel(if (it) 2 else 0) }

        Spacer(modifier = Modifier.height(24.dp))
        Text("NETWORK UPSTREAM", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
        SettingAction("Wipe Telemetry", "Clear scanning stats", Icons.Default.BarChart) { RustBridge.resetStats(); Toast.makeText(context, "Telemetry reset", Toast.LENGTH_SHORT).show() }
        SettingAction("Nuke Rules", "Reset all isolation states", Icons.Default.DeleteForever) { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().getAllRules().forEach { RustBridge.setAppRule(it.appId, 0); NetHealApp.database.netHealDao().saveRule(FirewallRule(it.appId, 0, 0)) } }; Toast.makeText(context, "Rules purged", Toast.LENGTH_SHORT).show() }

        Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(alpha = 0.1f))
        SystemInfoItem("Engine Version", "v5.0.0-Absolute-Omega-Rust")
        SystemInfoItem("Status", "Absolute Secure")
        Spacer(modifier = Modifier.height(40.dp))
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
