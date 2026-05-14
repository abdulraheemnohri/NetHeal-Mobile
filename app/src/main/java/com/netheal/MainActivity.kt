package com.netheal

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.netheal.ui.Dashboard
import com.netheal.ui.SettingsScreen
import com.netheal.ui.FirewallScreen
import com.netheal.ui.LogsScreen
import com.netheal.vpn.NetHealVpnService

class MainActivity : ComponentActivity() {

    private val vpnRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) { startVpnService() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("home") }
            Scaffold(
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF0A0E14), contentColor = Color.White) {
                        NavigationBarItem(
                            selected = currentScreen == "home", onClick = { currentScreen = "home" },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) }, label = { Text("Home") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00FFA3), unselectedIconColor = Color.Gray, selectedTextColor = Color(0xFF00FFA3), unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF161B22))
                        )
                        NavigationBarItem(
                            selected = currentScreen == "firewall", onClick = { currentScreen = "firewall" },
                            icon = { Icon(Icons.Default.Security, contentDescription = null) }, label = { Text("Policies") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00FFA3), unselectedIconColor = Color.Gray, selectedTextColor = Color(0xFF00FFA3), unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF161B22))
                        )
                        NavigationBarItem(
                            selected = currentScreen == "logs", onClick = { currentScreen = "logs" },
                            icon = { Icon(Icons.Default.List, contentDescription = null) }, label = { Text("Traffic") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00FFA3), unselectedIconColor = Color.Gray, selectedTextColor = Color(0xFF00FFA3), unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF161B22))
                        )
                        NavigationBarItem(
                            selected = currentScreen == "settings", onClick = { currentScreen = "settings" },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) }, label = { Text("Command") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00FFA3), unselectedIconColor = Color.Gray, selectedTextColor = Color(0xFF00FFA3), unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF161B22))
                        )
                    }
                }
            ) { innerPadding ->
                Surface(modifier = Modifier.padding(innerPadding)) {
                    when (currentScreen) {
                        "home" -> Dashboard(onToggleFirewall = { if (it) prepareVpn() else stopVpn() })
                        "firewall" -> FirewallScreen()
                        "logs" -> LogsScreen()
                        "settings" -> SettingsScreen()
                    }
                }
            }
        }
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) { vpnRequest.launch(intent) }
        else { startVpnService() }
    }

    private fun startVpnService() {
        val intent = Intent(this, NetHealVpnService::class.java).apply { action = "START" }
        startService(intent)
    }

    private fun stopVpn() {
        val intent = Intent(this, NetHealVpnService::class.java).apply { action = "STOP" }
        startService(intent)
    }
}
