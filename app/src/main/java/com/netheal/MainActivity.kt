package com.netheal

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.netheal.ui.Dashboard
import com.netheal.ui.SettingsScreen
import com.netheal.ui.FirewallScreen
import com.netheal.vpn.NetHealVpnService

class MainActivity : ComponentActivity() {

    private val vpnRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("dashboard") }

            when (currentScreen) {
                "dashboard" -> {
                    Dashboard(
                        onToggleFirewall = { enabled ->
                            if (enabled) prepareVpn() else stopVpn()
                        },
                        onNavigateToSettings = { currentScreen = "settings" },
                        onNavigateToFirewall = { currentScreen = "firewall" }
                    )
                }
                "settings" -> {
                    SettingsScreen(onBack = { currentScreen = "dashboard" })
                }
                "firewall" -> {
                    FirewallScreen(onBack = { currentScreen = "dashboard" })
                }
            }
        }
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnRequest.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, NetHealVpnService::class.java)
        startService(intent)
    }

    private fun stopVpn() {
        val intent = Intent(this, NetHealVpnService::class.java)
        stopService(intent)
    }
}
