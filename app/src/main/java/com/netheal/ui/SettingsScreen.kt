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
    var startOnBoot by remember { mutableStateOf(prefs.getBoolean("start_on_boot", true)) }
    var silentMode by remember { mutableStateOf(prefs.getBoolean("silent_mode", false)) }

    var whitelist by remember { mutableStateOf(listOf<WhitelistEntry>()) }
    var blacklist by remember { mutableStateOf(listOf<BlacklistEntry>()) }

    val scope = rememberCoroutineScope()
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringBattery = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    LaunchedEffect(Unit) {
        whitelist = NetHealApp.database.netHealDao().getWhitelist()
        blacklist = NetHealApp.database.netHealDao().getBlacklist()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("CORE CONFIG", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text("ENGINE AND SYSTEM PARAMETERS", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(30.dp))
        Text("Stability & Persistence", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)

        if (!isIgnoringBattery) {
            SettingAction("Disable Battery Optimization", "Keep firewall alive in background", Icons.Default.BatteryAlert) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        }

        SettingToggle("Auto-Healing Core", "Automatically restores rules on failure", autoHeal) { autoHeal = it; prefs.edit().putBoolean("auto_heal", it).apply() }
        SettingToggle("Start on Boot", "Automatically start VPN at device boot", startOnBoot) { startOnBoot = it; prefs.edit().putBoolean("start_on_boot", it).apply() }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Security Engine", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)

        SettingToggle("Lockdown Mode", "Drop ALL non-whitelisted traffic", lockdownMode) {
            lockdownMode = it; prefs.edit().putBoolean("lockdown_mode", it).apply()
            RustBridge.setSecurityLevel(if (it) 3 else if (highSecurity) 2 else 0)
        }
        SettingToggle("Military Security", "Aggressive filtering rules", highSecurity) {
            highSecurity = it; prefs.edit().putBoolean("military_mode", it).apply()
            if (!lockdownMode) RustBridge.setSecurityLevel(if (it) 2 else 0)
        }
        SettingToggle("Silent Mode", "Supress non-critical notifications", silentMode) { silentMode = it; prefs.edit().putBoolean("silent_mode", it).apply() }

        Spacer(modifier = Modifier.height(32.dp))
        Text("System Tools", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)

        SettingAction("Network Diagnostic", "Test connectivity and latency", Icons.Default.NetworkCheck) {
            val result = String(RustBridge.runDiagnostics())
            Toast.makeText(context, result, Toast.LENGTH_LONG).show()
        }

        SettingAction("Repair System", "Force run healing logic", Icons.Default.Build) { RustBridge.heal(); Toast.makeText(context, "Healing engine...", Toast.LENGTH_SHORT).show() }
        SettingAction("Reset Stats", "Clear scanning telemetry", Icons.Default.BarChart) { RustBridge.resetStats(); Toast.makeText(context, "Stats reset", Toast.LENGTH_SHORT).show() }

        Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(alpha = 0.1f))
        SystemInfoItem("Engine Version", "v2.0.0-Immune-Rust")
        SystemInfoItem("Battery Opt", if (isIgnoringBattery) "Optimized" else "Restricted")
        SystemInfoItem("Interface", "tun0")

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SystemInfoItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 12.sp); Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

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
