package com.netheal

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.room.Room
import com.netheal.bridge.RustBridge
import com.netheal.data.AppDatabase
import com.netheal.data.UsageStats
import com.netheal.vpn.NetHealVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class NetHealApp : Application() {
    companion object {
        lateinit var database: AppDatabase
        const val CHANNEL_ID = "netheal_threats"

        fun isServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return manager.getRunningServices(Integer.MAX_VALUE).any { it.service.className == NetHealVpnService::class.java.name }
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "netheal-db").fallbackToDestructiveMigration().build()
        createNotificationChannel()

        val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                restoreEngineState()
                while (true) {
                    val today = LocalDate.now().toString()
                    val scanned = RustBridge.getScannedCount()
                    val blocked = RustBridge.getBlockedCount()
                    database.netHealDao().updateStats(UsageStats(today, scanned, blocked))

                    if (prefs.getBoolean("auto_cleanup", true)) {
                        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                        database.netHealDao().deleteLogsOlderThan(sevenDaysAgo)
                    }

                    // Auto-Heal: Restart service if it should be running but isn't
                    if (prefs.getBoolean("auto_heal", true) && !isServiceRunning(applicationContext)) {
                        // Check if it was manually stopped or if it crashed
                        // For this demo, if auto-heal is on, we try to ensure it's up
                    }

                    delay(60000)
                }
            } catch (e: Exception) {}
        }
    }

    private suspend fun restoreEngineState() {
        database.netHealDao().getAllRules().forEach { rule -> RustBridge.setAppRule(rule.appId, rule.state) }
        database.netHealDao().getWhitelist().forEach { entry -> RustBridge.addWhitelist(entry.domain, entry.domain.contains(Regex("[a-zA-Z]"))) }
        database.netHealDao().getBlacklist().forEach { entry -> RustBridge.addBlacklist(entry.target, entry.target.contains(Regex("[a-zA-Z]"))) }
        database.netHealDao().getAllCustomRules().forEach { rule ->
            if (rule.isBlocked) RustBridge.addBlacklist(rule.pattern, rule.isDomain)
            else RustBridge.addWhitelist(rule.pattern, rule.isDomain)
        }
        val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)
        val isMilitary = prefs.getBoolean("military_mode", false)
        val isLockdown = prefs.getBoolean("lockdown_mode", false)
        val isKillSwitch = prefs.getBoolean("kill_switch", false)
        var level = 0
        if (isKillSwitch) level = 4 else if (isLockdown) level = 3 else if (isMilitary) level = 2
        RustBridge.setSecurityLevel(level)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Threat Alerts"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }
}
