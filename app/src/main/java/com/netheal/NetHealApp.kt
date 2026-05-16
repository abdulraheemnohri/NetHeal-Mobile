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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

private const val DEFAULT_OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
private const val DEFAULT_OPENROUTER_MODEL = "openrouter/owl-alpha"

class NetHealApp : Application(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var lastMotionTime: Long = 0
    private var isMotionShieldActive = false
    private var lastForegroundApp: String = ""
    private var lastAiSyncTime: Long = 0L

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

            if (prefs.getBoolean("ai_enabled", false)) {
                val aiSyncIntervalMs = prefs.getInt("ai_sync_interval_seconds", 120).coerceAtLeast(30) * 1000L
                if (System.currentTimeMillis() - lastAiSyncTime >= aiSyncIntervalMs) {
                    lastAiSyncTime = System.currentTimeMillis()
                    syncDynamicAiThreatIntelligence(prefs)
                }
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
                Log.i("DynamicAI", "Predictive Analytics: Enabling Night-Shield protection.")
            }
        }
        val blocked = RustBridge.getBlockedCount()
        if (blocked > 1000) {
            database.netHealDao().insertIncident(Incident(title = "AI PREDICTION: BRUTE FORCE", description = "Automated escalation due to high block frequency.", severity = "CRITICAL"))
            RustBridge.setSecurityLevel(4)
            triggerHapticFeedback(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
    }

    private suspend fun syncDynamicAiThreatIntelligence(prefs: android.content.SharedPreferences) {
        val analyticsBytes = RustBridge.getAnalytics()
        if (analyticsBytes.isEmpty()) return

        val analytics = try {
            JSONObject(String(analyticsBytes))
        } catch (e: Exception) {
            Log.e("DynamicAI", "Invalid analytics payload", e)
            return
        }

        val apiUrl = prefs.getString("ai_api_url", DEFAULT_OPENROUTER_API_URL)?.ifBlank { DEFAULT_OPENROUTER_API_URL } ?: DEFAULT_OPENROUTER_API_URL
        val model = prefs.getString("ai_model", DEFAULT_OPENROUTER_MODEL)?.ifBlank { DEFAULT_OPENROUTER_MODEL } ?: DEFAULT_OPENROUTER_MODEL
        val apiKey = prefs.getString("ai_api_key", "") ?: ""
        val openRouterEndpoint = apiUrl.contains("openrouter.ai", ignoreCase = true)
        val fallbackEnabled = prefs.getBoolean("ai_local_fallback", true)
        val telemetry = buildAiTelemetry(analytics, prefs.getBoolean("ai_redact_telemetry", true))

        if (apiKey.isBlank() && openRouterEndpoint) {
            if (fallbackEnabled) applyLocalTelemetryHeuristic(analytics, prefs)
            return
        }

        try {
            val directive = requestAiDirective(apiUrl, model, apiKey, telemetry, openRouterEndpoint)
            applyAiDirective(directive, model, prefs)
        } catch (e: Exception) {
            Log.e("DynamicAI", "Remote sync failed; applying local heuristic", e)
            if (fallbackEnabled) applyLocalTelemetryHeuristic(analytics, prefs)
        }
    }

    private fun requestAiDirective(apiUrl: String, model: String, apiKey: String, analytics: JSONObject, openRouterEndpoint: Boolean): JSONObject {
        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 12000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
            if (openRouterEndpoint) {
                setRequestProperty("HTTP-Referer", "https://netheal.local")
                setRequestProperty("X-Title", "NetHeal Mobile")
            }
        }

        val body = JSONObject()
            .put("model", model)
            .put("temperature", 0.1)
            .put("max_tokens", 500)
            .put("messages", JSONArray()
                .put(JSONObject()
                    .put("role", "system")
                    .put("content", "You are NetHeal's mobile threat co-processor. Return only compact JSON with optional securityLevel and rules. Each rule must include appId, action monitor|isolate, risk, and reason."))
                .put(JSONObject()
                    .put("role", "user")
                    .put("content", "Analyze this Android VPN telemetry and return enforcement JSON: $analytics")))

        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val responseCode = connection.responseCode
        val responseText = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
        }
        connection.disconnect()

        if (responseCode !in 200..299) error("AI API returned $responseCode: $responseText")
        val response = JSONObject(responseText)
        val content = response
            .optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            .orEmpty()
        return JSONObject(extractJsonObject(content))
    }

    private suspend fun applyAiDirective(directive: JSONObject, model: String, prefs: android.content.SharedPreferences) {
        val level = directive.optInt("securityLevel", -1)
        if (level in 0..4) RustBridge.setSecurityLevel(level)

        val autoIsolate = prefs.getBoolean("ai_auto_isolate", true)
        val riskThreshold = prefs.getInt("ai_risk_threshold", 85)
        val rules = directive.optJSONArray("rules") ?: JSONArray()
        for (index in 0 until rules.length()) {
            val rule = rules.optJSONObject(index) ?: continue
            val appId = rule.optString("appId")
            if (appId.isBlank()) continue

            val action = rule.optString("action", "monitor")
            val risk = rule.optInt("risk", 0)
            if (action == "isolate" || risk >= riskThreshold) {
                if (autoIsolate) RustBridge.setAppRule(appId, 2)
                database.netHealDao().insertIncident(Incident(
                    title = if (autoIsolate) "AI FIREWALL: ISOLATED" else "AI FIREWALL: RECOMMENDED",
                    description = "Model $model flagged $appId at risk $risk. ${rule.optString("reason")}",
                    severity = if (risk >= 90) "CRITICAL" else "WARNING",
                    sourceApp = appId
                ))
                triggerHapticFeedback(VibrationEffect.EFFECT_TICK)
            }
        }
    }

    private suspend fun applyLocalTelemetryHeuristic(analytics: JSONObject, prefs: android.content.SharedPreferences) {
        val usage = analytics.optJSONObject("usage") ?: return
        val autoIsolate = prefs.getBoolean("ai_auto_isolate", true)
        val riskThreshold = prefs.getInt("ai_risk_threshold", 85)
        usage.keys().forEach { appId ->
            val appData = usage.getJSONObject(appId)
            val estimatedRisk = if (appData.getLong("p") > 100000) 90 else 80
            if (appData.getLong("p") > 50000 && estimatedRisk >= riskThreshold && !appId.startsWith("com.android")) {
                if (autoIsolate) RustBridge.setAppRule(appId, 2)
                database.netHealDao().insertIncident(Incident(title = if (autoIsolate) "AUTO FIREWALL: ISOLATED" else "AUTO FIREWALL: RECOMMENDED", description = "App $appId flagged for background exfiltration risk $estimatedRisk.", severity = "WARNING", sourceApp = appId))
                triggerHapticFeedback(VibrationEffect.EFFECT_TICK)
            }
        }
    }

    private fun buildAiTelemetry(analytics: JSONObject, redactTelemetry: Boolean): JSONObject {
        if (!redactTelemetry) return analytics

        val redacted = JSONObject()
        val usage = analytics.optJSONObject("usage") ?: return analytics
        val safeUsage = JSONObject()
        usage.keys().forEach { appId ->
            val appData = usage.getJSONObject(appId)
            val label = "app_" + kotlin.math.abs(appId.hashCode()).toString(16)
            safeUsage.put(label, JSONObject()
                .put("sent", appData.optLong("p"))
                .put("received", appData.optLong("r"))
                .put("system", appId.startsWith("com.android")))
        }
        redacted.put("usage", safeUsage)
        redacted.put("redacted", true)
        redacted.put("scanned", RustBridge.getScannedCount())
        redacted.put("blocked", RustBridge.getBlockedCount())
        return redacted
    }

    private fun extractJsonObject(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        return "{}"
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
        if (level < 0 || scale <= 0) return

        val batteryPct = (level * 100f) / scale
        val threshold = prefs.getInt("low_battery_threshold", 15)
        val staminaActive = prefs.getBoolean("stamina_mode_active", false)
        if (batteryPct <= threshold && prefs.getBoolean("battery_safeguard", true)) {
            RustBridge.setBatterySafeguard(true)
            RustBridge.setPerformanceMode(true)
            RustBridge.setBoosterActive(false)
            RustBridge.setMultipathActive(false)
            prefs.edit().putBoolean("stamina_mode_active", true).apply()
            if (!staminaActive) {
                Log.i("NetHeal", "Battery Intelligence: Stamina Mode active at ${batteryPct.toInt()}%.")
            }
        } else if (staminaActive && batteryPct > threshold + 5) {
            prefs.edit().putBoolean("stamina_mode_active", false).apply()
            RustBridge.setPerformanceMode(prefs.getBoolean("performance_mode", false))
            RustBridge.setBoosterActive(prefs.getBoolean("booster_active", false))
            RustBridge.setMultipathActive(prefs.getBoolean("multipath_active", false))
            Log.i("NetHeal", "Battery Intelligence: Stamina Mode released at ${batteryPct.toInt()}%.")
        }
    }

    private fun updateNetworkPosture() {
        if (isMotionShieldActive) return
        val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("posture_awareness", true)) return

        val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val actNw = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isCaptive = actNw?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) ?: false
        @Suppress("DEPRECATION")
        val ssid = wifi.connectionInfo.ssid?.replace("\"", "") ?: "Cellular"
        val trustedSsids = prefs.getString("trusted_ssids", "")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        when {
            isCaptive -> RustBridge.setSecurityLevel(4)
            ssid != "Cellular" && trustedSsids.any { it.equals(ssid, ignoreCase = true) } -> RustBridge.setSecurityLevel(1)
            ssid != "Cellular" && (ssid.contains("Public", true) || ssid.contains("Guest", true) || ssid.contains("Free", true)) -> RustBridge.setSecurityLevel(3)
        }
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
        RustBridge.setAiActive(prefs.getBoolean("ai_enabled", false))
        RustBridge.setNeuralShield(prefs.getBoolean("neural_shield", false))
        RustBridge.setPerformanceMode(prefs.getBoolean("performance_mode", false))
        RustBridge.setBoosterActive(prefs.getBoolean("booster_active", false))
        RustBridge.setMultipathActive(prefs.getBoolean("multipath_active", false))
        RustBridge.setObfuscation(prefs.getBoolean("obfuscation_active", false))
        RustBridge.setBatterySafeguard(prefs.getBoolean("battery_safeguard", true))
        RustBridge.setBufferSize(prefs.getInt("buffer_size", 32768))
        RustBridge.setShapingMode(prefs.getInt("shaping_mode", 0))
        RustBridge.setHoneypotMode(prefs.getBoolean("honeypot_active", false))
        RustBridge.setFingerprintMask(prefs.getInt("fingerprint_type", 0))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Threat Alerts", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }
}
