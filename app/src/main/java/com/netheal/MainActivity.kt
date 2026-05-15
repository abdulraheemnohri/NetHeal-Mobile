package com.netheal

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.*
import com.netheal.ui.Dashboard
import com.netheal.ui.FirewallScreen
import com.netheal.ui.SettingsScreen
import com.netheal.ui.TacticalCommandDeck
import com.netheal.vpn.NetHealVpnService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            Scaffold(
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF0D1117)) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Dashboard, null) },
                            label = { Text("CORE") },
                            selected = currentRoute == "dashboard",
                            onClick = { navController.navigate("dashboard") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Security, null) },
                            label = { Text("ARSENAL") },
                            selected = currentRoute == "firewall",
                            onClick = { navController.navigate("firewall") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Radar, null) },
                            label = { Text("TACTICAL") },
                            selected = currentRoute == "tactical",
                            onClick = { navController.navigate("tactical") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text("CONFIG") },
                            selected = currentRoute == "settings",
                            onClick = { navController.navigate("settings") }
                        )
                    }
                }
            ) { padding ->
                NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
                    composable("dashboard") { Dashboard { enabled -> toggleVpn(enabled) } }
                    composable("firewall") { FirewallScreen() }
                    composable("tactical") { TacticalCommandDeck() }
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }

    private fun toggleVpn(enabled: Boolean) {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, RESULT_OK, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val intent = Intent(this, NetHealVpnService::class.java)
            startService(intent)
        }
    }
}
