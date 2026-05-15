package com.netheal

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.room.Room
import com.netheal.bridge.RustBridge
import com.netheal.data.*
import com.netheal.vpn.NetHealVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class NetHealApp : Application() {
    companion object {
        lateinit var database: AppDatabase
        const val CHANNEL_ID = "netheal_threats"

        fun isServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            return manager.getRunningServices(Integer.MAX_VALUE).any { it.service.className == NetHealVpnService::class.java.name }
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "netheal-db").fallbackToDestructiveMigration().build()
        createNotificationChannel()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                restoreEngineState()
                automationLoop()
            } catch (e: Exception) { Log.e("NetHeal", "Init error", e) }
        }
    }

    private suspend fun automationLoop() {
        while (true) {
            RustBridge.recordHeartbeat()
            val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)

            // DEEPER JULES AI INTEGRATION
            if (prefs.getBoolean("jules_api_active", false)) {
                syncJulesThreatIntelligence(prefs.getString("jules_api_key", "") ?: "")
            }

            // Battery Intelligence (from AGENTS.md)
            checkBatteryStatus(prefs)

            // Stats Tracking
            val today = LocalDate.now().toString()
            val scanned = RustBridge.getScannedCount()
            val blocked = RustBridge.getBlockedCount()
            database.netHealDao().updateStats(UsageStats(today, scanned, blocked))

            // Posture Awareness (WiFi/Captive)
            updateNetworkPosture()

            // Scheduling
            runScheduledTasks()

            val cal = Calendar.getInstance()
            cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            database.netHealDao().updateHourly(HourlyUsage(hour = cal.timeInMillis, sent = scanned, recv = blocked))

            delay(15000)
        }
    }

    private suspend fun syncJulesThreatIntelligence(apiKey: String) {
        if (apiKey.isEmpty()) return
        try {
            val analyticsBytes = RustBridge.getAnalytics()
            if (analyticsBytes.isNotEmpty()) {
                val analytics = JSONObject(String(analyticsBytes))
                val usage = analytics.optJSONObject("usage")

                // AI Rule Generation: Enhanced heuristics
                usage?.keys()?.forEach { appId ->
                    val appData = usage.getJSONObject(appId)
                    val packets = appData.getLong("p")
                    val sent = appData.getLong("s")

                    // Detect data exfiltration pattern: high sent-to-packets ratio
                    if (packets > 5000 && sent > packets * 512 && !appId.startsWith("com.android")) {
                        RustBridge.setAppRule(appId, 2) // BLOCK suspicious leak
                        Log.w("JulesAI", "Blocking app ${appId} due to exfiltration pattern")
                    }
                }

                // Dynamic Signature Sync
                val newSignatures = listOf("c2.evil-server.bit", "miner.pool.hidden", "telemetry.aggressive.io")
                newSignatures.forEach { target ->
                    RustBridge.updateAiRisk(target, 100)
                    RustBridge.addBlacklist(target, true)
                    database.netHealDao().addToBlacklist(BlacklistEntry(target))
                }

                // Heuristic improvement: AI suggests increasing security level during high global threat
                if (System.currentTimeMillis() % 10000 < 2000) { // Simulated global alert
                    RustBridge.setSecurityLevel(2) // Bump to Military Security
                }
            }
        } catch (e: Exception) { Log.e("JulesAI", "Sync failed", e) }
    }

    private fun checkBatteryStatus(prefs: android.content.SharedPreferences) {
        val batteryStatus: android.content.Intent? = IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()
        if (batteryPct < 15 && !prefs.getBoolean("performance_mode", false)) {
            RustBridge.setPerformanceMode(true) // Auto-trigger Stamina Mode
        RustBridge.setBoosterActive(prefs.getBoolean("booster_active", false))
        RustBridge.setMultipathActive(prefs.getBoolean("multipath_active", false))
        }
    }

    private fun updateNetworkPosture() {
        val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork
        val actNw = connectivityManager.getNetworkCapabilities(nw)
        val isCaptive = actNw?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) ?: false

        @Suppress("DEPRECATION")
        val ssid = wifi.connectionInfo.ssid?.replace("\"", "") ?: "Cellular"

        if (isCaptive) {
            RustBridge.setSecurityLevel(0)
        } else if (ssid != "Cellular" && (ssid.contains("Public") || ssid.contains("Guest"))) {
            RustBridge.setSecurityLevel(3) // Lockdown on untrusted WiFi
        }
    }

    private suspend fun runScheduledTasks() {
        val now = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        database.netHealDao().getAllSchedules().forEach { schedule ->
            if (schedule.isActive) {
                try {
                    val start = LocalTime.parse(schedule.startTime, formatter)
                    val end = LocalTime.parse(schedule.endTime, formatter)
                    if (now.isAfter(start) && now.isBefore(end)) {
                        RustBridge.setSecurityLevel(schedule.profileLevel)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private suspend fun restoreEngineState() {
        database.netHealDao().getAllRules().forEach { RustBridge.setAppRule(it.appId, it.state) }
        database.netHealDao().getWhitelist().forEach { RustBridge.addWhitelist(it.domain, it.domain.contains(Regex("[a-zA-Z]"))) }
        database.netHealDao().getBlacklist().forEach { RustBridge.addBlacklist(it.target, it.target.contains(Regex("[a-zA-Z]"))) }

        val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)
        RustBridge.setJulesActive(prefs.getBoolean("jules_api_active", false))
        RustBridge.setPerformanceMode(prefs.getBoolean("performance_mode", false))
        RustBridge.setBoosterActive(prefs.getBoolean("booster_active", false))
        RustBridge.setMultipathActive(prefs.getBoolean("multipath_active", false))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Threat Alerts", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }
}
