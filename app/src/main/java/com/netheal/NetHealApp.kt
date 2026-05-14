package com.netheal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.netheal.bridge.RustBridge
import com.netheal.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        // Restore engine state
        CoroutineScope(Dispatchers.IO).launch {
            // Restore app rules
            val rules = database.netHealDao().getAllRules()
            rules.forEach { rule ->
                RustBridge.setAppRule(rule.appId, rule.isBlocked)
            }

            // Restore whitelist
            val whitelist = database.netHealDao().getWhitelist()
            val blacklist = database.netHealDao().getBlacklist()
            blacklist.forEach { entry ->
                // We need a Rust method to block manually
                RustBridge.addBlacklist(entry.target)
            }
            whitelist.forEach { entry ->
                RustBridge.addWhitelist(entry.domain)
            }

            // Restore security level
            val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)
            val isMilitary = prefs.getBoolean("military_mode", false)
            RustBridge.setSecurityLevel(if (isMilitary) 1.toByte() else 0.toByte())
        }
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
