package com.netheal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.netheal.bridge.RustBridge
import com.netheal.data.AppDatabase
import com.netheal.data.UsageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class NetHealApp : Application() {
    companion object {
        lateinit var database: AppDatabase
        const val CHANNEL_ID = "netheal_threats"
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "netheal-db"
        ).fallbackToDestructiveMigration().build()

        createNotificationChannel()

        val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)

        // Restore engine state and background management
        CoroutineScope(Dispatchers.IO).launch {
            try {
                restoreEngineState()

                // Management loop
                while (true) {
                    val today = LocalDate.now().toString()
                    val scanned = RustBridge.getScannedCount()
                    val blocked = RustBridge.getBlockedCount()
                    database.netHealDao().updateStats(UsageStats(today, scanned, blocked))

                    // Auto-cleanup logs if enabled (default 7 days)
                    if (prefs.getBoolean("auto_cleanup", true)) {
                        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                        database.netHealDao().deleteLogsOlderThan(sevenDaysAgo)
                    }

                    delay(60000) // Every minute
                }
            } catch (e: Exception) {
                // Initial launch or error
            }
        }
    }

    private suspend fun restoreEngineState() {
        database.netHealDao().getAllRules().forEach { rule ->
            RustBridge.setAppRule(rule.appId, rule.isBlocked)
        }
        database.netHealDao().getWhitelist().forEach { entry ->
            RustBridge.addWhitelist(entry.domain)
        }
        database.netHealDao().getBlacklist().forEach { entry ->
            RustBridge.addBlacklist(entry.target)
        }
        val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)
        val isMilitary = prefs.getBoolean("military_mode", false)
        RustBridge.setSecurityLevel(if (isMilitary) 1.toByte() else 0.toByte())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Threat Alerts"
            val descriptionText = "Notifications for blocked malicious connections"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
