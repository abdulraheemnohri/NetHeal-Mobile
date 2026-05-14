package com.netheal.ui

import android.content.Context
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("netheal_prefs", Context.MODE_PRIVATE) }

    var autoHeal by remember { mutableStateOf(prefs.getBoolean("auto_heal", true)) }
    var highSecurity by remember { mutableStateOf(prefs.getBoolean("military_mode", false)) }

    var whitelistDomain by remember { mutableStateOf("") }
    var blacklistTarget by remember { mutableStateOf("") }

    var whitelist by remember { mutableStateOf(listOf<WhitelistEntry>()) }
    var blacklist by remember { mutableStateOf(listOf<BlacklistEntry>()) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        whitelist = NetHealApp.database.netHealDao().getWhitelist()
        blacklist = NetHealApp.database.netHealDao().getBlacklist()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E14))
            )
        },
        containerColor = Color(0xFF05070A)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Engine", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 13.sp)

            SettingToggle("Enable Auto-Healing", "Restores rules if engine crashes", autoHeal) {
                autoHeal = it
                prefs.edit().putBoolean("auto_heal", it).apply()
            }
            SettingToggle("Military Mode", "Maximum blocking, zero bypass", highSecurity) {
                highSecurity = it
                prefs.edit().putBoolean("military_mode", it).apply()
                RustBridge.setSecurityLevel(if (it) 1.toByte() else 0.toByte())
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("WHITELISTED DOMAINS", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 13.sp)

            OutlinedTextField(
                value = whitelistDomain,
                onValueChange = { whitelistDomain = it },
                label = { Text("Add Domain", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00FFA3),
                    unfocusedBorderColor = Color.Gray
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        if (whitelistDomain.isNotEmpty()) {
                            val domain = whitelistDomain
                            RustBridge.addWhitelist(domain)
                            scope.launch {
                                NetHealApp.database.netHealDao().addToWhitelist(WhitelistEntry(domain))
                                whitelist = NetHealApp.database.netHealDao().getWhitelist()
                            }
                            whitelistDomain = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00FFA3))
                    }
                }
            )

            whitelist.forEach { entry ->
                ListItem(entry.domain, onDelete = {
                    scope.launch {
                        NetHealApp.database.netHealDao().removeFromWhitelist(entry)
                        whitelist = NetHealApp.database.netHealDao().getWhitelist()
                        // Note: In a real app, we'd also remove it from the active Rust engine
                    }
                })
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("MANUAL BLACKLIST", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 13.sp)

            OutlinedTextField(
                value = blacklistTarget,
                onValueChange = { blacklistTarget = it },
                label = { Text("Add Domain or IP", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00FFA3),
                    unfocusedBorderColor = Color.Gray
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        if (blacklistTarget.isNotEmpty()) {
                            val target = blacklistTarget
                            RustBridge.addBlacklist(target)
                            scope.launch {
                                NetHealApp.database.netHealDao().addToBlacklist(BlacklistEntry(target))
                                blacklist = NetHealApp.database.netHealDao().getBlacklist()
                            }
                            blacklistTarget = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00FFA3))
                    }
                }
            )

            blacklist.forEach { entry ->
                ListItem(entry.target, onDelete = {
                    scope.launch {
                        NetHealApp.database.netHealDao().removeFromBlacklist(entry)
                        blacklist = NetHealApp.database.netHealDao().getBlacklist()
                    }
                })
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("SYSTEM CONTROL", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 13.sp)

            SettingAction("Repair Firewall", "Manual integrity check", Icons.Default.Build) {
                RustBridge.heal()
            }
            SettingAction("Clear Threat Logs", "Permanent database wipe", Icons.Default.Restore) {
                scope.launch {
                    NetHealApp.database.netHealDao().deleteAllLogs()
                }
            }

            Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(alpha = 0.2f))
            Text("Engine v1.1.2-Ultimate-Rust\nNetHeal Core connected via JNI", color = Color.DarkGray, fontSize = 10.sp)
        }
    }
}

@Composable
fun ListItem(text: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, color = Color.LightGray, fontSize = 14.sp)
        IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFA3))
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
            Text(title, color = Color.White, fontSize = 16.sp)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Icon(icon, contentDescription = null, tint = Color.Gray)
    }
}
