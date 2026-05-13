package com.netheal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.NetHealApp
import com.netheal.bridge.RustBridge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var autoHeal by remember { mutableStateOf(true) }
    var highSecurity by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
            Text("Engine", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 14.sp)

            SettingToggle("Enable Auto-Healing", "Restores rules if engine crashes", autoHeal) { autoHeal = it }
            SettingToggle("Military Mode", "Maximum blocking, no bypass allowed", highSecurity) { highSecurity = it }

            Spacer(modifier = Modifier.height(24.dp))
            Text("System", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 14.sp)

            SettingAction("Repair System", "Force run healing logic", Icons.Default.Build) {
                RustBridge.heal()
            }
            SettingAction("Reset Database", "Clear all threat logs", Icons.Default.Restore) {
                scope.launch {
                    NetHealApp.database.threatLogDao().deleteAllLogs()
                }
            }
            SettingAction("Engine Version", "V1.0.2-rust-core", Icons.Default.Info) {}

            Divider(modifier = Modifier.padding(vertical = 20.dp), color = Color.Gray.copy(alpha = 0.2f))

            Text("NetHeal Mobile relies on Android VPN permissions. Disabling the VPN will stop all traffic filtering.", color = Color.Gray, fontSize = 11.sp)
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
