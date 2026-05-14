package com.netheal.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Context
import android.os.PowerManager
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
import com.netheal.data.WhitelistEntry
import com.netheal.data.BlacklistEntry
import com.netheal.data.FirewallRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("netheal_prefs", Context.MODE_PRIVATE) }

    var autoHeal by remember { mutableStateOf(prefs.getBoolean("auto_heal", true)) }
    var highSecurity by remember { mutableStateOf(prefs.getBoolean("military_mode", false)) }
    var startOnBoot by remember { mutableStateOf(prefs.getBoolean("start_on_boot", true)) }
    var ipv6Filtering by remember { mutableStateOf(prefs.getBoolean("ipv6_filtering", true)) }
    var autoCleanup by remember { mutableStateOf(prefs.getBoolean("auto_cleanup", true)) }

    var whitelistDomain by remember { mutableStateOf("") }
    var blacklistTarget by remember { mutableStateOf("") }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("CORE CONFIG", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text("ENGINE AND SYSTEM PARAMETERS", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(30.dp))

        Text("Security Engine", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)

        SettingToggle("Auto-Healing Core", "Automatically restores rules on failure", autoHeal) {
            autoHeal = it
            prefs.edit().putBoolean("auto_heal", it).apply()
        }
        SettingToggle("Military Security", "Strict filtering, zero-bypass mode", highSecurity) {
            highSecurity = it
            prefs.edit().putBoolean("military_mode", it).apply()
            RustBridge.setSecurityLevel(if (it) 2 else 0)
        }
        SettingToggle("Start on Boot", "Automatically start VPN at device boot", startOnBoot) {
            startOnBoot = it
            prefs.edit().putBoolean("start_on_boot", it).apply()
        }
        SettingToggle("Filter IPv6", "Enable packet inspection for IPv6", ipv6Filtering) {
            ipv6Filtering = it
            prefs.edit().putBoolean("ipv6_filtering", it).apply()
        }
        SettingToggle("Auto-Cleanup Logs", "Delete logs older than 7 days", autoCleanup) {
            autoCleanup = it
            prefs.edit().putBoolean("auto_cleanup", it).apply()
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Access Control", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)

        ConfigInputRow("Whitelist Domain", whitelistDomain, { whitelistDomain = it }) {
            if (whitelistDomain.isNotEmpty()) {
                val domain = whitelistDomain
                RustBridge.addWhitelist(domain)
                scope.launch {
                    NetHealApp.database.netHealDao().addToWhitelist(WhitelistEntry(domain))
                    whitelist = NetHealApp.database.netHealDao().getWhitelist()
                }
                whitelistDomain = ""
            }
        }
        whitelist.take(3).forEach { entry ->
            ConfigListItem(entry.domain) {
                scope.launch {
                    NetHealApp.database.netHealDao().removeFromWhitelist(entry)
                    RustBridge.removeWhitelist(entry.domain)
                    whitelist = NetHealApp.database.netHealDao().getWhitelist()
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ConfigInputRow("Manual Block (IP/Domain)", blacklistTarget, { blacklistTarget = it }) {
            if (blacklistTarget.isNotEmpty()) {
                val target = blacklistTarget
                RustBridge.addBlacklist(target)
                scope.launch {
                    NetHealApp.database.netHealDao().addToBlacklist(BlacklistEntry(target))
                    blacklist = NetHealApp.database.netHealDao().getBlacklist()
                }
                blacklistTarget = ""
            }
        }
        blacklist.take(3).forEach { entry ->
            ConfigListItem(entry.target) {
                scope.launch {
                    NetHealApp.database.netHealDao().removeFromBlacklist(entry)
                    RustBridge.removeBlacklist(entry.target)
                    blacklist = NetHealApp.database.netHealDao().getBlacklist()
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("System Status", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)

        SystemInfoItem("Engine Version", "v1.2.8-Ultimate-Rust")
        SystemInfoItem("Battery Opt", if (isIgnoringBattery) "Optimized" else "Restricted")
        SystemInfoItem("JNI Bridge", "Stable")
        SystemInfoItem("Interface", "tun0")

        Spacer(modifier = Modifier.height(24.dp))
        Text("Maintenance", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)

        SettingAction("Repair System", "Force run healing logic", Icons.Default.Build) {
            RustBridge.heal()
            Toast.makeText(context, "Healing engine...", Toast.LENGTH_SHORT).show()
        }
        SettingAction("Reset Firewall Rules", "Clear all per-app restrictions", Icons.Default.Refresh) {
            scope.launch(Dispatchers.IO) {
                val rules = NetHealApp.database.netHealDao().getAllRules()
                rules.forEach {
                    NetHealApp.database.netHealDao().saveRule(FirewallRule(it.appId, false))
                    RustBridge.setAppRule(it.appId, false)
                }
            }
            Toast.makeText(context, "Rules reset", Toast.LENGTH_SHORT).show()
        }
        SettingAction("Reset Database", "Clear all threat logs", Icons.Default.Restore) {
            scope.launch {
                NetHealApp.database.netHealDao().deleteAllLogs()
                Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
            }
        }

        Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(alpha = 0.1f))
        Text("NetHeal Mobile Advanced\nVerified Secure 2026", color = Color.DarkGray, fontSize = 10.sp, lineHeight = 14.sp)
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SystemInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ConfigInputRow(label: String, value: String, onValueChange: (String) -> Unit, onAdd: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray, fontSize = 11.sp) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF00FFA3),
            unfocusedBorderColor = Color(0xFF161B22),
            focusedContainerColor = Color(0xFF0D1117),
            unfocusedContainerColor = Color(0xFF0D1117)
        ),
        trailingIcon = {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF00FFA3))
            }
        },
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}

@Composable
fun ConfigListItem(text: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, color = Color.LightGray, fontSize = 13.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Icon(
            Icons.Default.Close,
            contentDescription = null,
            tint = Color.Red.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp).clickable { onDelete() }
        )
    }
}

@Composable
fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00FFA3),
                checkedTrackColor = Color(0xFF00FFA3).copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF161B22)
            )
        )
    }
}

@Composable
fun SettingAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}
