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
            if (prefs.getBoolean("jules_api_active", false)) {
                syncJulesThreatIntelligence(prefs.getString("jules_api_key", "") ?: "")
                runPredictiveAnalytics(prefs)
            }
            checkBatteryStatus(prefs)
            val today = LocalDate.now().toString()
            val scanned = RustBridge.getScannedCount()
            val blocked = RustBridge.getBlockedCount()
            database.netHealDao().updateStats(UsageStats(today, scanned, blocked))
            updateNetworkPosture()
            runScheduledTasks()

            // Incident Timeline Generation
            generateIncidentTimeline()

            val cal = Calendar.getInstance()
            cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            database.netHealDao().updateHourly(HourlyUsage(hour = cal.timeInMillis, sent = scanned, recv = blocked))
            delay(15000)
        }
    }

    private suspend fun runPredictiveAnalytics(prefs: android.content.SharedPreferences) {
        val now = LocalTime.now()
        // Night-Shield: AI predicts higher risk of background exfiltration at night
        if (now.hour >= 23 || now.hour < 5) {
            if (prefs.getBoolean("neural_shield", true)) {
                RustBridge.setSecurityLevel(3)
                Log.i("JulesAI", "Predictive Analytics: Enabling Night-Shield protection.")
            }
        }

        // Dynamic Attack Prediction based on blocked counts
        val blocked = RustBridge.getBlockedCount()
        if (blocked > 1000) {
            val incident = Incident(
                title = "AI PREDICTION: BRUTE FORCE DETECTED",
                description = "Rapid blocked connection attempts suggest an ongoing automated attack. Escalating security level.",
                severity = "CRITICAL"
            )
            database.netHealDao().insertIncident(incident)
            RustBridge.setSecurityLevel(4)
        }
    }

    private suspend fun syncJulesThreatIntelligence(apiKey: String) {
        if (apiKey.isEmpty()) return
        try {
            val analyticsBytes = RustBridge.getAnalytics()
            if (analyticsBytes.isNotEmpty()) {
                val analytics = JSONObject(String(analyticsBytes))
                val usage = analytics.optJSONObject("usage")
                usage?.keys()?.forEach { appId ->
                    val appData = usage.getJSONObject(appId)
                    // Auto Firewall Rule: If an app consumes too much background data, auto-restrict it
                    if (appData.getLong("p") > 50000 && !appId.startsWith("com.android")) {
                        RustBridge.setAppRule(appId, 2)
                        database.netHealDao().insertIncident(Incident(
                            title = "AUTO FIREWALL: APP ISOLATED",
                            description = "App $appId flagged for excessive background traffic. Connectivity restricted.",
                            severity = "WARNING",
                            sourceApp = appId
                        ))
                    }
                }
            }
        } catch (e: Exception) { Log.e("JulesAI", "Sync failed", e) }
    }

    private suspend fun generateIncidentTimeline() {
        val logs = database.netHealDao().getAllLogs()
        if (logs.size > 50) {
            val highRisk = logs.filter { it.riskScore > 90 }
            if (highRisk.isNotEmpty()) {
                database.netHealDao().insertIncident(Incident(
                    title = "THREAT CLUSTER IDENTIFIED",
                    description = "Detected ${highRisk.size} high-risk packets targeting external C2 nodes.",
                    severity = "CRITICAL"
                ))
            }
        }
    }

    private fun checkBatteryStatus(prefs: android.content.SharedPreferences) {
        val batteryStatus: android.content.Intent? = IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()
            if (batteryPct < 5 && prefs.getBoolean("battery_safeguard", true)) {
                RustBridge.setSecurityLevel(0)
                RustBridge.setBoosterActive(false)
                Log.w("NetHeal", "Battery Safeguard Triggered: Defenses Offline")
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
        if (isCaptive) { RustBridge.setSecurityLevel(0) }
        else if (ssid != "Cellular" && (ssid.contains("Public") || ssid.contains("Guest"))) { RustBridge.setSecurityLevel(3) }
    }

    private suspend fun runScheduledTasks() {
        val now = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        database.netHealDao().getAllSchedules().forEach { schedule ->
            if (schedule.isActive) {
                try {
                    val start = LocalTime.parse(schedule.startTime, formatter)
                    val end = LocalTime.parse(schedule.endTime, formatter)
                    if (now.isAfter(start) && now.isBefore(end)) { RustBridge.setSecurityLevel(schedule.profileLevel) }
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
        RustBridge.setNeuralShield(prefs.getBoolean("neural_shield", false))
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
