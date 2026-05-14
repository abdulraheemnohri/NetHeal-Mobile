package com.netheal.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.netheal.vpn.NetHealVpnService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("netheal_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("start_on_boot", true)) {
                val serviceIntent = Intent(context, NetHealVpnService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
