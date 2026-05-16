package com.netheal

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import java.util.*

class NetHealApp : Application(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var lastMotionTime: Long = 0
    private var isMotionShieldActive = false
    private var lastForegroundApp: String = ""

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
        setupPostureAwareness()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                restoreEngineState()
                automationLoop()
            } catch (e: Exception) { Log.e("NetHeal", "Init error", e) }
        }
    }

    private fun setupPostureAwareness() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble())
            if (acceleration > 15) {
                lastMotionTime = System.currentTimeMillis()
                if (!isMotionShieldActive) {
                    isMotionShieldActive = true
                    val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)
                    if (prefs.getBoolean("posture_awareness", true)) {
                        RustBridge.setSecurityLevel(3)
                        triggerHapticFeedback(VibrationEffect.EFFECT_HEAVY_CLICK)
                        Log.i("NetHeal", "Posture Awareness: Motion detected, activating High-Security profile.")
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private suspend fun automationLoop() {
        while (true) {
            RustBridge.recordHeartbeat()
            val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)

            // Motion Shield cooldown
            if (isMotionShieldActive && System.currentTimeMillis() - lastMotionTime > 30000) {
                isMotionShieldActive = false
                Log.i("NetHeal", "Posture Awareness: Device stationary, resetting security profile.")
                updateNetworkPosture()
            }

            // Neural Profile Auto-Switching
            if (prefs.getBoolean("neural_profile_switching", true)) {
                autoSwitchNeuralProfile()
            }

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
            generateIncidentTimeline()

            val cal = Calendar.getInstance()
            cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            database.netHealDao().updateHourly(HourlyUsage(hour = cal.timeInMillis, sent = scanned, recv = blocked))
            delay(15000)
        }
    }

    private fun autoSwitchNeuralProfile() {
        val currentApp = getForegroundApp()
        if (currentApp != lastForegroundApp) {
            lastForegroundApp = currentApp
            when {
                currentApp.contains("game") || currentApp.contains("tencent") -> {
                    RustBridge.setPerformanceMode(true)
                    RustBridge.setSecurityLevel(1) // Gaming Mode: Low latency priority
                    Log.i("NetHeal", "Neural: Gaming detected, optimizing for latency.")
                }
                currentApp.contains("bank") || currentApp.contains("wallet") || currentApp.contains("crypto") -> {
                    RustBridge.setSecurityLevel(4) // Strict: Maximum protection
                    triggerHapticFeedback(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    Log.i("NetHeal", "Neural: Finance app active, engaging MAXIMUM isolation.")
                }
                else -> {
                    // Standard switching logic handled by updateNetworkPosture
                }
            }
        }
    }

    private fun getForegroundApp(): String {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time)
        if (stats != null && stats.isNotEmpty()) {
            val lastApp = stats.maxByOrNull { it.lastTimeUsed }
            return lastApp?.packageName ?: ""
        }
        return ""
    }

    private fun triggerHapticFeedback(effectId: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        }
    }

    private suspend fun runPredictiveAnalytics(prefs: android.content.SharedPreferences) {
        val now = LocalTime.now()
        if (now.hour >= 23 || now.hour < 5) {
            if (prefs.getBoolean("neural_shield", true)) {
                RustBridge.setSecurityLevel(3)
                Log.i("JulesAI", "Predictive Analytics: Enabling Night-Shield protection.")
            }
        }
        val blocked = RustBridge.getBlockedCount()
        if (blocked > 1000) {
            database.netHealDao().insertIncident(Incident(title = "AI PREDICTION: BRUTE FORCE", description = "Automated escalation due to high block frequency.", severity = "CRITICAL"))
            RustBridge.setSecurityLevel(4)
            triggerHapticFeedback(VibrationEffect.EFFECT_HEAVY_CLICK)
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
                    if (appData.getLong("p") > 50000 && !appId.startsWith("com.android")) {
                        RustBridge.setAppRule(appId, 2)
                        database.netHealDao().insertIncident(Incident(title = "AUTO FIREWALL: ISOLATED", description = "App $appId restricted for background exfiltration.", severity = "WARNING", sourceApp = appId))
                        triggerHapticFeedback(VibrationEffect.EFFECT_TICK)
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
                database.netHealDao().insertIncident(Incident(title = "THREAT CLUSTER", description = "Identified ${highRisk.size} high-risk C2 callbacks.", severity = "CRITICAL"))
            }
        }
    }

    private fun checkBatteryStatus(prefs: android.content.SharedPreferences) {
        val batteryStatus: android.content.Intent? = IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { registerReceiver(null, it) }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()
        if (batteryPct < 5 && prefs.getBoolean("battery_safeguard", true)) {
            RustBridge.setSecurityLevel(0)
            RustBridge.setBoosterActive(false)
        }
    }

    private fun updateNetworkPosture() {
        if (isMotionShieldActive) return
        val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val actNw = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isCaptive = actNw?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) ?: false
        @Suppress("DEPRECATION")
        val ssid = wifi.connectionInfo.ssid?.replace("\"", "") ?: "Cellular"
        if (isCaptive) { RustBridge.setSecurityLevel(0) }
        else if (ssid != "Cellular" && (ssid.contains("Public") || ssid.contains("Guest"))) { RustBridge.setSecurityLevel(3) }
    }

    private suspend fun runScheduledTasks() {
        if (isMotionShieldActive) return
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
