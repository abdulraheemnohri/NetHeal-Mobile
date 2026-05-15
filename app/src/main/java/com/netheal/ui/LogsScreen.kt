package com.netheal.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.bridge.RustBridge
import com.netheal.NetHealApp
import com.netheal.data.ThreatLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(listOf<ThreatLog>()) }
    var appUsage by remember { mutableStateOf(mapOf<String, UsageInfo>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            logs = NetHealApp.database.netHealDao().getAllLogs()
            try {
                val analytics = String(RustBridge.getAnalytics())
                if (analytics.isNotEmpty()) {
                    val json = JSONObject(analytics)
                    val usage = json.getJSONObject("usage")
                    val map = mutableMapOf<String, UsageInfo>()
                    usage.keys().forEach { k ->
                        val obj = usage.getJSONObject(k)
                        map[k] = UsageInfo(obj.getLong("s"), obj.getLong("r"), obj.getLong("p"))
                    }
                    appUsage = map
                }
            } catch (e: Exception) {}
            delay(5000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("TRAFFIC CENTER", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text("ABSOLUTE DATA TELEMETRY", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { scope.launch { NetHealApp.database.netHealDao().deleteAllLogs(); logs = emptyList() } }) { Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color(0xFF00FFA3)) }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("A TO Z APP BANDWIDTH", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(appUsage.toList().sortedByDescending { it.second.packets }) { (pkg, info) ->
                TrafficUsageCard(pkg, info)
            }
            if (appUsage.isEmpty()) { item { Text("Analyzing data streams...", color = Color.DarkGray, fontSize = 11.sp) } }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("CRITICAL INTERCEPTIONS", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(logs.take(20), key = { it.id }) { log -> LogCard(log) }
            if (logs.isEmpty()) { item { Text("No threats detected.", color = Color.DarkGray, fontSize = 12.sp) } }
        }
    }
}


@Composable
fun TrafficUsageCard(pkg: String, info: UsageInfo) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).background(Color(0xFF161B22), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = Color(0xFF00FFA3), modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pkg.split(".").last().uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${info.packets} PKTS", color = Color.Gray, fontSize = 8.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("↑ ${formatSize(info.sent)}", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("↓ ${formatSize(info.recv)}", color = Color(0xFF2196F3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024f)
    return String.format("%.1f MB", bytes / (1024f * 1024f))
}

@Composable
fun LogCard(log: ThreatLog) {
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)), shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(3.dp, 25.dp).background(Color.Red, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) { Text(log.domain, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1); Text(time, color = Color.Gray, fontSize = 9.sp) }
            Text("${log.riskScore}", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

data class UsageInfo(val sent: Long, val recv: Long, val packets: Long)
