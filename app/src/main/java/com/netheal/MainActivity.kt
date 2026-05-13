package com.netheal

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.netheal.ui.Dashboard
import com.netheal.ui.SettingsScreen
import com.netheal.vpn.NetHealVpnService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("dashboard") }

            if (currentScreen == "dashboard") {
                Dashboard(
                    onToggleFirewall = { enabled ->
                        if (enabled) prepareVpn() else stopVpn()
                    },
                    onNavigateToSettings = { currentScreen = "settings" }
                )
            } else {
                SettingsScreen(onBack = { currentScreen = "dashboard" })
            }
        }
    }

    private fun prepareVpn() {
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

    private fun stopVpn() {
        val intent = Intent(this, NetHealVpnService::class.java)
        stopService(intent)
    }
}
