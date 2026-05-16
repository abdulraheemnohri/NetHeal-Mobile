package com.netheal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.bridge.RustBridge
import kotlinx.coroutines.delay
import org.json.JSONObject

data class UsageInfo(val sent: Long, val recv: Long)

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

@Composable
fun LogsScreen() {
    var usageData by remember { mutableStateOf(mapOf<String, UsageInfo>()) }

    LaunchedEffect(Unit) {
        while (true) {
            val statsBytes = RustBridge.getAnalytics()
            if (statsBytes.isNotEmpty()) {
                try {
                    val json = JSONObject(String(statsBytes))
                    val usage = json.optJSONObject("usage")
                    val map = mutableMapOf<String, UsageInfo>()
                    usage?.keys()?.forEach { key ->
                        val data = usage.getJSONObject(key)
                        map[key] = UsageInfo(data.optLong("p"), data.optLong("r"))
                    }
                    usageData = map
                } catch (e: Exception) {}
            }
            delay(2000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF010409)).padding(16.dp)) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))
        Text("APPLICATION TELEMETRY", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(usageData.entries.toList()) { entry ->
                TelemetryItem(entry.key, entry.value)
            }
        }
    }
}

@Composable
fun TelemetryItem(appId: String, info: UsageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        border = BorderStroke(1.dp, Color(0xFF161B22))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(appId.substringAfterLast("."), color = Color.White, fontWeight = FontWeight.Bold)
                Text(appId, color = Color.Gray, fontSize = 9.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SENT: ${formatSize(info.sent)}", color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("RECV: ${formatSize(info.recv)}", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
