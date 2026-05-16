package com.netheal

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.*
import androidx.navigation.*
import com.netheal.ui.*
import com.netheal.vpn.NetHealVpnService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            Box(modifier = Modifier.fillMaxSize()) {
                CyberBackground()
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        NavigationBar(containerColor = CyberTheme.Surface) {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route

                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Dashboard, null) },
                                label = { Text("CORE") },
                                selected = currentRoute == "dashboard",
                                onClick = { navController.navigate("dashboard") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberTheme.Primary, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Security, null) },
                                label = { Text("ARSENAL") },
                                selected = currentRoute == "firewall",
                                onClick = { navController.navigate("firewall") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberTheme.Primary, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.VerifiedUser, null) },
                                label = { Text("AUDIT") },
                                selected = currentRoute == "audit",
                                onClick = { navController.navigate("audit") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberTheme.Primary, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Radar, null) },
                                label = { Text("TACTICAL") },
                                selected = currentRoute == "tactical",
                                onClick = { navController.navigate("tactical") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberTheme.Primary, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Article, null) },
                                label = { Text("LOGS") },
                                selected = currentRoute == "logs",
                                onClick = { navController.navigate("logs") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberTheme.Primary, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, null) },
                                label = { Text("CONFIG") },
                                selected = currentRoute == "settings",
                                onClick = { navController.navigate("settings") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberTheme.Primary, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                        }
                    }
                ) { padding ->
                    NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
                        composable("dashboard") { Dashboard { enabled -> toggleVpn(enabled) } }
                        composable("firewall") { FirewallScreen() }
                        composable("audit") { SecurityAuditSection() }
                        composable("tactical") { TacticalCommandDeck() }
                        composable("logs") { FirewallLogScreen() }
                        composable("settings") { SettingsScreen() }
                    }
                }
            }
        }
    }

    private fun toggleVpn(enabled: Boolean) {
        if (!enabled) {
            startService(Intent(this, NetHealVpnService::class.java).setAction("STOP"))
            return
        }

        val intent = VpnService.prepare(this)
        if (intent != null) {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 100)
        } else {
            onActivityResult(100, RESULT_OK, null)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val intent = Intent(this, NetHealVpnService::class.java)
            startService(intent)
        }
    }
}
