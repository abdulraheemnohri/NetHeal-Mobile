package com.netheal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.bridge.RustBridge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("netheal_prefs", android.content.Context.MODE_PRIVATE)

    var aiApiKey by remember { mutableStateOf(prefs.getString("ai_api_key", "") ?: "") }
    var aiActive by remember { mutableStateOf(prefs.getBoolean("ai_enabled", false)) }
    var aiProvider by remember { mutableStateOf(prefs.getString("ai_provider", "OpenRouter") ?: "OpenRouter") }
    var aiApiUrl by remember { mutableStateOf(prefs.getString("ai_api_url", DEFAULT_OPENROUTER_API_URL) ?: DEFAULT_OPENROUTER_API_URL) }
    var aiModel by remember { mutableStateOf(prefs.getString("ai_model", DEFAULT_OPENROUTER_MODEL) ?: DEFAULT_OPENROUTER_MODEL) }
    var aiConfigSaved by remember { mutableStateOf(false) }
    var neuralShield by remember { mutableStateOf(prefs.getBoolean("neural_shield", true)) }
    var aiSensitivity by remember { mutableFloatStateOf(prefs.getFloat("ai_sensitivity", 0.5f)) }
    var boosterActive by remember { mutableStateOf(prefs.getBoolean("booster_active", false)) }
    var multipathActive by remember { mutableStateOf(prefs.getBoolean("multipath_active", false)) }
    var performanceMode by remember { mutableStateOf(prefs.getBoolean("performance_mode", false)) }
    var obfuscationActive by remember { mutableStateOf(prefs.getBoolean("obfuscation_active", false)) }
    var batterySafeguard by remember { mutableStateOf(prefs.getBoolean("battery_safeguard", true)) }
    var postureAwareness by remember { mutableStateOf(prefs.getBoolean("posture_awareness", true)) }
    var neuralProfileSwitching by remember { mutableStateOf(prefs.getBoolean("neural_profile_switching", true)) }
    var honeypotActive by remember { mutableStateOf(prefs.getBoolean("honeypot_active", false)) }
    var fingerprintType by remember { mutableIntStateOf(prefs.getInt("fingerprint_type", 0)) }
    var dnsProvider by remember { mutableStateOf(prefs.getString("upstream_dns", "Cloudflare") ?: "Cloudflare") }
    var trustedSsids by remember { mutableStateOf(prefs.getString("trusted_ssids", "") ?: "") }
    var lowBatteryThreshold by remember { mutableFloatStateOf(prefs.getInt("low_battery_threshold", 15).toFloat()) }
    var bufferSize by remember { mutableFloatStateOf(prefs.getInt("buffer_size", 32768).toFloat()) }
    var shapingMode by remember { mutableIntStateOf(prefs.getInt("shaping_mode", 0)) }
    var aiAutoIsolate by remember { mutableStateOf(prefs.getBoolean("ai_auto_isolate", true)) }
    var aiLocalFallback by remember { mutableStateOf(prefs.getBoolean("ai_local_fallback", true)) }
    var aiRedactTelemetry by remember { mutableStateOf(prefs.getBoolean("ai_redact_telemetry", true)) }
    var aiRiskThreshold by remember { mutableFloatStateOf(prefs.getInt("ai_risk_threshold", 85).toFloat()) }
    var aiSyncInterval by remember { mutableFloatStateOf(prefs.getInt("ai_sync_interval_seconds", 120).toFloat()) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))
        Text("SYSTEM CONFIGURATION", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        SettingCategory("DYNAMIC AI THREAT CO-PROCESSOR") {
            Text("Provider", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedChips(listOf("OpenRouter", "Custom"), aiProvider) { provider ->
                aiProvider = provider
                if (provider == "OpenRouter") {
                    aiApiUrl = DEFAULT_OPENROUTER_API_URL
                    aiModel = DEFAULT_OPENROUTER_MODEL
                }
                prefs.edit()
                    .putString("ai_provider", provider)
                    .putString("ai_api_url", aiApiUrl)
                    .putString("ai_model", aiModel)
                    .apply()
                aiConfigSaved = true
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = aiApiKey,
                onValueChange = {
                    aiApiKey = it
                    prefs.edit().putString("ai_api_key", it).apply()
                    aiConfigSaved = false
                },
                label = { Text("OpenRouter / Dynamic API Key", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberTheme.Primary),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        aiActive = !aiActive
                        prefs.edit().putBoolean("ai_enabled", aiActive).apply()
                        RustBridge.setAiActive(aiActive)
                    }) {
                        Icon(Icons.Default.CloudSync, null, tint = if (aiActive) CyberTheme.Primary else Color.Gray)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = aiApiUrl,
                onValueChange = {
                    aiApiUrl = it
                    prefs.edit().putString("ai_api_url", it).apply()
                    aiConfigSaved = false
                },
                label = { Text("Chat Completion API URL", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberTheme.Primary),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = aiModel,
                onValueChange = {
                    aiModel = it
                    prefs.edit().putString("ai_model", it).apply()
                    aiConfigSaved = false
                },
                label = { Text("Model ID", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberTheme.Primary),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            CyberButton("UPDATE DYNAMIC API", Icons.Default.AutoFixHigh, onClick = {
                prefs.edit()
                    .putString("ai_provider", aiProvider)
                    .putString("ai_api_url", aiApiUrl.ifBlank { DEFAULT_OPENROUTER_API_URL })
                    .putString("ai_model", aiModel.ifBlank { DEFAULT_OPENROUTER_MODEL })
                    .putString("ai_api_key", aiApiKey)
                    .putBoolean("ai_enabled", true)
                    .apply()
                aiActive = true
                aiConfigSaved = true
                RustBridge.setAiActive(true)
            }, modifier = Modifier.fillMaxWidth())
            Text(
                if (aiConfigSaved) "Dynamic API updated: $aiModel" else "Default: openrouter/owl-alpha via OpenRouter chat completions.",
                color = if (aiConfigSaved) CyberTheme.Primary else Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            ModernSettingToggle("AI Auto-Isolate", aiAutoIsolate, "Apply high-confidence AI isolation directives automatically") {
                aiAutoIsolate = it
                prefs.edit().putBoolean("ai_auto_isolate", it).apply()
            }
            ModernSettingToggle("Local Heuristic Fallback", aiLocalFallback, "Keep protecting traffic when the dynamic API is offline") {
                aiLocalFallback = it
                prefs.edit().putBoolean("ai_local_fallback", it).apply()
            }
            ModernSettingToggle("Redact Telemetry", aiRedactTelemetry, "Minimize app identifiers before sending AI telemetry") {
                aiRedactTelemetry = it
                prefs.edit().putBoolean("ai_redact_telemetry", it).apply()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI Risk Gate ${aiRiskThreshold.toInt()}", color = Color.Gray, modifier = Modifier.weight(1f), fontSize = 12.sp)
                Slider(
                    value = aiRiskThreshold,
                    onValueChange = {
                        aiRiskThreshold = it
                        prefs.edit().putInt("ai_risk_threshold", it.toInt()).apply()
                    },
                    valueRange = 50f..100f,
                    steps = 9,
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(thumbColor = CyberTheme.Primary, activeTrackColor = CyberTheme.Primary)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sync Every ${aiSyncInterval.toInt()}s", color = Color.Gray, modifier = Modifier.weight(1f), fontSize = 12.sp)
                Slider(
                    value = aiSyncInterval,
                    onValueChange = {
                        aiSyncInterval = it
                        prefs.edit().putInt("ai_sync_interval_seconds", it.toInt()).apply()
                    },
                    valueRange = 30f..600f,
                    steps = 18,
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(thumbColor = CyberTheme.Secondary, activeTrackColor = CyberTheme.Secondary)
                )
            }
            ModernSettingToggle("Neural Shield", neuralShield, "Night protection and predictive rule escalation") {
                neuralShield = it
                prefs.edit().putBoolean("neural_shield", it).apply()
                RustBridge.setNeuralShield(it)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inspection Depth ${(aiSensitivity * 100).toInt()}%", color = Color.Gray, modifier = Modifier.weight(1f), fontSize = 12.sp)
                Slider(
                    value = aiSensitivity, onValueChange = {
                        aiSensitivity = it
                        prefs.edit().putFloat("ai_sensitivity", it).apply()
                    },
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(thumbColor = CyberTheme.Primary, activeTrackColor = CyberTheme.Primary)
                )
            }
        }

        SettingCategory("NETWORK POSTURE") {
            Text("Upstream DNS", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedChips(listOf("Cloudflare", "AdGuard", "Google"), dnsProvider) { provider ->
                dnsProvider = provider
                prefs.edit().putString("upstream_dns", provider).apply()
                RustBridge.setUpstreamDns(providerToDnsIp(provider))
            }
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = trustedSsids,
                onValueChange = {
                    trustedSsids = it
                    prefs.edit().putString("trusted_ssids", it).apply()
                },
                label = { Text("Trusted SSIDs (comma separated)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberTheme.Primary),
                shape = RoundedCornerShape(12.dp)
            )
            ModernSettingToggle("Posture Awareness", postureAwareness, "Escalate on captive portals, public SSIDs, and motion events") {
                postureAwareness = it
                prefs.edit().putBoolean("posture_awareness", it).apply()
            }
            ModernSettingToggle("Neural Profile Switching", neuralProfileSwitching, "Auto-tune security for games, wallets, and banking apps") {
                neuralProfileSwitching = it
                prefs.edit().putBoolean("neural_profile_switching", it).apply()
            }
        }

        SettingCategory("STEALTH & DECEPTION") {
            ModernSettingToggle("Traffic Obfuscation", obfuscationActive, "Mask packet signatures before egress") {
                obfuscationActive = it
                prefs.edit().putBoolean("obfuscation_active", it).putBoolean("stealth_active", it).apply()
                RustBridge.setObfuscation(it)
            }
            ModernSettingToggle("Honeypot Mode", honeypotActive, "Lure and trap potential network scans") {
                honeypotActive = it
                prefs.edit().putBoolean("honeypot_active", it).apply()
                RustBridge.setHoneypotMode(it)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("OS Fingerprint Mask", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val masks = listOf("NONE", "WIN-11", "LINUX", "IOS-17")
            SegmentedChips(masks, masks.getOrElse(fingerprintType) { "NONE" }) { name ->
                fingerprintType = masks.indexOf(name)
                prefs.edit().putInt("fingerprint_type", fingerprintType).apply()
                RustBridge.setFingerprintMask(fingerprintType)
            }
        }

        SettingCategory("OMEGA ENGINE TUNING") {
            ModernSettingToggle("Performance Fast-Path", performanceMode, "Skip redundant L7 checks during high load") {
                performanceMode = it
                prefs.edit().putBoolean("performance_mode", it).apply()
                RustBridge.setPerformanceMode(it)
            }
            ModernSettingToggle("Packet Optimization", boosterActive, "Inject PSH flags for low-latency TCP") {
                boosterActive = it
                prefs.edit().putBoolean("booster_active", it).apply()
                RustBridge.setBoosterActive(it)
            }
            ModernSettingToggle("Link Bonding", multipathActive, "Aggregate WiFi + Cellular data streams") {
                multipathActive = it
                prefs.edit().putBoolean("multipath_active", it).apply()
                RustBridge.setMultipathActive(it)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Traffic Shaping", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val shapingModes = listOf("Balanced", "Streaming", "Gaming")
            SegmentedChips(shapingModes, shapingModes.getOrElse(shapingMode) { "Balanced" }) { name ->
                shapingMode = shapingModes.indexOf(name)
                prefs.edit().putInt("shaping_mode", shapingMode).apply()
                RustBridge.setShapingMode(shapingMode)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Buffer ${bufferSize.toInt() / 1024}KB", color = Color.Gray, modifier = Modifier.weight(1f), fontSize = 12.sp)
                Slider(
                    value = bufferSize,
                    onValueChange = {
                        bufferSize = it
                        prefs.edit().putInt("buffer_size", it.toInt()).apply()
                        RustBridge.setBufferSize(it.toInt())
                    },
                    valueRange = 8192f..65536f,
                    steps = 6,
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(thumbColor = CyberTheme.Primary, activeTrackColor = CyberTheme.Primary)
                )
            }
        }

        SettingCategory("BATTERY & PERSISTENCE") {
            ModernSettingToggle("Battery Safeguard", batterySafeguard, "Auto-trigger Stamina Mode and disable boosters on low battery") {
                batterySafeguard = it
                prefs.edit().putBoolean("battery_safeguard", it).apply()
                RustBridge.setBatterySafeguard(it)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Stamina Threshold ${lowBatteryThreshold.toInt()}%", color = Color.Gray, modifier = Modifier.weight(1f), fontSize = 12.sp)
                Slider(
                    value = lowBatteryThreshold,
                    onValueChange = {
                        lowBatteryThreshold = it
                        prefs.edit().putInt("low_battery_threshold", it.toInt()).apply()
                    },
                    valueRange = 5f..30f,
                    steps = 4,
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(thumbColor = CyberTheme.Warning, activeTrackColor = CyberTheme.Warning)
                )
            }
            Text("Foreground service, boot restore, and WakeLock persistence stay enabled by default.", color = Color.Gray, fontSize = 10.sp)
        }

        SettingCategory("DATA MANAGEMENT") {
            CyberButton("PURGE TELEMETRY LOGS", Icons.Default.DeleteSweep, onClick = { RustBridge.clearLogs() }, modifier = Modifier.fillMaxWidth(), isDanger = true)
            Spacer(modifier = Modifier.height(12.dp))
            CyberButton("NEURAL KNOWLEDGE BASE", Icons.Default.MenuBook, onClick = { }, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text("NetHeal Absolute v3.0.0-OMEGA", color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(40.dp))
    }
}

private const val DEFAULT_OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
private const val DEFAULT_OPENROUTER_MODEL = "openrouter/owl-alpha"

private fun providerToDnsIp(provider: String): String = when (provider) {
    "AdGuard" -> "94.140.14.14"
    "Google" -> "8.8.8.8"
    else -> "1.1.1.1"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedChips(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(option, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberTheme.Primary, selectedLabelColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SettingCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        GlassCard { content() }
    }
}

@Composable
fun ModernSettingToggle(title: String, checked: Boolean, desc: String, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = Color.Gray, fontSize = 10.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CyberTheme.Primary, checkedTrackColor = CyberTheme.Primary.copy(alpha = 0.5f))
        )
    }
}
