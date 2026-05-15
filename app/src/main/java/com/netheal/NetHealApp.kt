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
import androidx.room.Room
import com.netheal.bridge.RustBridge
import com.netheal.data.AppDatabase
import com.netheal.data.UsageStats
import com.netheal.data.HourlyUsage
import com.netheal.vpn.NetHealVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            } catch (e: Exception) {}
        }
    }

    private suspend fun automationLoop() {
        while (true) {
            RustBridge.recordHeartbeat()

            // Jules AI Cloud Sync (Simulated)
            val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)
            if (prefs.getBoolean("jules_api_active", false)) {
                syncJulesThreatIntelligence()
            }

            val batteryStatus: android.content.Intent? = IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                registerReceiver(null, ifilter)
            }
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = level * 100 / scale.toFloat()

            if (batteryPct < 20 && !prefs.getBoolean("performance_mode", false)) {
                RustBridge.setPerformanceMode(true)
            }

            val today = LocalDate.now().toString()
            val scanned = RustBridge.getScannedCount()
            val blocked = RustBridge.getBlockedCount()
            database.netHealDao().updateStats(UsageStats(today, scanned, blocked))

            val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nw = connectivityManager.activeNetwork
            val actNw = connectivityManager.getNetworkCapabilities(nw)
            val isCaptive = actNw?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) ?: false

            val info = wifi.connectionInfo
            val ssid = info.ssid?.replace("\"", "") ?: "Cellular"

            if (isCaptive) {
                RustBridge.setSecurityLevel(0)
            } else {
                val ssidRules = database.netHealDao().getRulesForSsid(ssid)
                ssidRules.forEach { rule ->
                    RustBridge.setAppRule(rule.appId, if (rule.blockOnSsid) 2 else 0)
                }

                if (ssid != "Cellular" && ssid != "<unknown ssid>") {
                    if (ssid.contains("Public") || ssid.contains("Guest")) {
                        RustBridge.setSecurityLevel(3)
                    }
                }
            }

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

            val cal = Calendar.getInstance()
            cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            database.netHealDao().updateHourly(HourlyUsage(hour = cal.timeInMillis, sent = scanned, recv = blocked))

            delay(15000)
        }
    }

    private suspend fun syncJulesThreatIntelligence() {
        // Mock: In a real app, this would poll a secure endpoint for new domain risks
        val suspiciousTargets = listOf("malware-cnc.io", "tracker.api", "data-leak.net")
        suspiciousTargets.forEach {
            RustBridge.updateAiRisk(it, 95) // Mark high risk in kernel cache
        }
    }

    private suspend fun restoreEngineState() {
        database.netHealDao().getAllRules().forEach { rule ->
            RustBridge.setAppRule(rule.appId, rule.state)
            RustBridge.setAppBwLimit(rule.appId, rule.bwLimit)
        }
        database.netHealDao().getWhitelist().forEach { entry -> RustBridge.addWhitelist(entry.domain, entry.domain.contains(Regex("[a-zA-Z]"))) }
        database.netHealDao().getBlacklist().forEach { entry -> RustBridge.addBlacklist(entry.target, entry.target.contains(Regex("[a-zA-Z]"))) }
        database.netHealDao().getAllCustomRules().forEach { rule ->
            if (rule.isBlocked) RustBridge.addBlacklist(rule.pattern, rule.isDomain)
            else RustBridge.addWhitelist(rule.pattern, rule.isDomain)
        }
        database.netHealDao().getAllPortRules().forEach { rule -> if (rule.isBlocked) RustBridge.addPortBlock(rule.port) }
        database.netHealDao().getAllGeoRules().forEach { rule -> if (rule.isBlocked) RustBridge.addGeoBlock(rule.countryIso) }

        val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)
        val level = if (prefs.getBoolean("kill_switch", false)) 4 else if (prefs.getBoolean("lockdown_mode", false)) 3 else if (prefs.getBoolean("military_mode", false)) 2 else 0
        RustBridge.setSecurityLevel(level)
        RustBridge.setPerformanceMode(prefs.getBoolean("performance_mode", false))
        RustBridge.setStealthMode(prefs.getBoolean("stealth_mode", false))
        RustBridge.setDnsHardening(prefs.getBoolean("dns_hardening", false))
        RustBridge.setLearningMode(prefs.getBoolean("learning_mode", false))
        RustBridge.setJulesActive(prefs.getBoolean("jules_api_active", false))

        val dns = prefs.getString("upstream_dns", "Cloudflare") ?: "Cloudflare"
        RustBridge.setUpstreamDns(if (dns == "Cloudflare") "1.1.1.1" else if (dns == "AdGuard") "94.140.14.14" else "8.8.8.8")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Threat Alerts"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }
}
