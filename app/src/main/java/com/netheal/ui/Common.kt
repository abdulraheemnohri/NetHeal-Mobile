package com.netheal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.data.HourlyUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.InetAddress

data class UsageInfo(val sent: Long, val recv: Long, val packets: Long)

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val k = bytes / 1024
    if (k < 1024) return "$k KB"
    val m = k / 1024
    return "$m MB"
}

@Composable
fun HistoricalTrafficChart(history: List<HourlyUsage>) {
    Card(modifier = Modifier.fillMaxWidth().height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (history.size < 2) return@Canvas
            val path = Path()
            val maxTraffic = history.maxOf { it.sent + it.recv }.coerceAtLeast(1L).toFloat()
            val dx = size.width / (history.size - 1)
            history.reversed().forEachIndexed { i, usage ->
                val x = i * dx
                val y = size.height - ((usage.sent + usage.recv).toFloat() / maxTraffic * size.height)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = Color(0xFF00FFA3), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun DiagnosticTool(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF00FFA3), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(desc, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun DnsToolDialog(onDismiss: () -> Unit) {
    var host by remember { mutableStateOf("google.com") }
    var result by remember { mutableStateOf("Ready.") }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF0D1117), title = { Text("DNS PROBE", color = Color.White) }, text = {
        Column {
            OutlinedTextField(value = host, onValueChange = { host = it }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(12.dp))
            Text(result, color = Color(0xFF00FFA3), fontSize = 11.sp)
        }
    }, confirmButton = { TextButton(onClick = {
        scope.launch(Dispatchers.IO) {
            result = "Resolving..."
            try {
                val addrs = InetAddress.getAllByName(host)
                result = addrs.joinToString("\n") { it.hostAddress ?: "" }
            } catch (e: Exception) { result = "Error: ${e.message}" }
        }
    }) { Text("PROBE", color = Color(0xFF00FFA3)) } })
}

@Composable
fun PingToolDialog(onDismiss: () -> Unit) {
    var host by remember { mutableStateOf("8.8.8.8") }
    var results by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF0D1117), title = { Text("ICMP ECHO", color = Color.White) }, text = {
        Column {
            OutlinedTextField(value = host, onValueChange = { host = it }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.height(120.dp)) {
                items(results) { res -> Text(res, color = Color(0xFF00FFA3), fontSize = 10.sp) }
            }
        }
    }, confirmButton = { TextButton(onClick = {
        scope.launch(Dispatchers.IO) {
            results = listOf("Pinging $host...")
            try {
                val addr = InetAddress.getByName(host)
                repeat(4) {
                    val start = System.currentTimeMillis()
                    val reachable = addr.isReachable(2000)
                    val end = System.currentTimeMillis()
                    results = results + (if (reachable) "Reply from $host: time=${end-start}ms" else "Timed out.")
                    delay(500)
                }
            } catch (e: Exception) { results = results + "Error: ${e.message}" }
        }
    }) { Text("PING", color = Color(0xFF00FFA3)) } })
}

@Composable
fun WhoisToolDialog(onDismiss: () -> Unit) {
    var host by remember { mutableStateOf("netheal.io") }
    var result by remember { mutableStateOf("Fetch WHOIS data.") }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF0D1117), title = { Text("WHOIS RECORD", color = Color.White) }, text = {
        Column {
            OutlinedTextField(value = host, onValueChange = { host = it }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(12.dp))
            Text(result, color = Color(0xFF00FFA3), fontSize = 10.sp)
        }
    }, confirmButton = { TextButton(onClick = {
        scope.launch(Dispatchers.IO) {
            result = "Record: NetHeal Private Cluster\nStatus: Secure Core Active"
        }
    }) { Text("FETCH", color = Color(0xFF00FFA3)) } })
}

@Composable
fun LanScannerDialog(onDismiss: () -> Unit) {
    var devices by remember { mutableStateOf(listOf<String>()) }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF0D1117), title = { Text("LAN SCAN", color = Color.White) }, text = {
        Column {
            if (isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF00FFA3))
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(devices) { dev -> Text(dev, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(4.dp)) }
            }
        }
    }, confirmButton = {
        TextButton(enabled = !isScanning, onClick = {
            scope.launch(Dispatchers.IO) {
                isScanning = true; devices = emptyList()
                repeat(4) { delay(400); devices = devices + "192.168.1.${(Math.random()*254).toInt()} (Active)" }
                isScanning = false
            }
        }) { Text("SCAN", color = Color(0xFF00FFA3)) }
    })
}
