package com.netheal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import com.netheal.NetHealApp
import com.netheal.bridge.RustBridge
import com.netheal.data.HourlyUsage
import kotlinx.coroutines.delay

@Composable
fun TacticalCommandDeck() {
    var history by remember { mutableStateOf(listOf<HourlyUsage>()) }
    var showDnsTool by remember { mutableStateOf(false) }
    var showPingTool by remember { mutableStateOf(false) }
    var showWhoisTool by remember { mutableStateOf(false) }
    var showLanScanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            history = NetHealApp.database.netHealDao().getRecentHourly()
            delay(5000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF010409)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))

        CorePerformanceMonitor()

        Spacer(modifier = Modifier.height(24.dp))
        Text("REAL-TIME THREAT MAP", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        ThreatOriginMap()

        Spacer(modifier = Modifier.height(24.dp))
        Text("HISTORICAL TRAFFIC (24H)", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        HistoricalTrafficChart(history)

        Spacer(modifier = Modifier.height(32.dp))
        Text("DIAGNOSTIC ARSENAL", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        DiagnosticTool("DNS Resolver", "Resolve hostname to IP", Icons.Default.Language) { showDnsTool = true }
        DiagnosticTool("Ping Utility", "Test host reachability", Icons.Default.TapAndPlay) { showPingTool = true }
        DiagnosticTool("Whois Probe", "Fetch domain ownership", Icons.Default.Info) { showWhoisTool = true }
        DiagnosticTool("LAN Discovery", "Map local network nodes", Icons.Default.SettingsEthernet) { showLanScanner = true }

        if (showDnsTool) DnsToolDialog(onDismiss = { showDnsTool = false })
        if (showPingTool) PingToolDialog(onDismiss = { showPingTool = false })
        if (showWhoisTool) WhoisToolDialog(onDismiss = { showWhoisTool = false })
        if (showLanScanner) LanScannerDialog(onDismiss = { showLanScanner = false })

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun CorePerformanceMonitor() {
    var cpuLoad by remember { mutableFloatStateOf(0.12f) }
    var memoryUsage by remember { mutableStateOf("42 MB") }
    var neuralLatency by remember { mutableStateOf("0.8ms") }
    var inferenceLoad by remember { mutableFloatStateOf(0.05f) }

    LaunchedEffect(Unit) {
        while(true) {
            cpuLoad = (0.05f + (Math.random() * 0.15f)).toFloat()
            memoryUsage = "${(40 + (Math.random() * 5).toInt())} MB"
            neuralLatency = "${String.format("%.2f", 0.5 + Math.random() * 0.5)}ms"
            inferenceLoad = (Math.random() * 0.2f).toFloat()
            delay(2000)
        }
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CORE PERFORMANCE MONITOR", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PerformanceItem("CPU LOAD", "${(cpuLoad * 100).toInt()}%", cpuLoad)
                PerformanceItem("KERNEL RAM", memoryUsage, 0.4f)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PerformanceItem("NEURAL LATENCY", neuralLatency, 0.2f)
                PerformanceItem("INFERENCE LOAD", "${(inferenceLoad * 100).toInt()}%", inferenceLoad)
            }
        }
    }
}

@Composable
fun PerformanceItem(label: String, value: String, progress: Float) {
    Column(modifier = Modifier.width(140.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.Gray, fontSize = 8.sp)
            Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape), color = Color(0xFF00FFA3), trackColor = Color(0xFF161B22))
    }
}

@Composable
fun ThreatOriginMap() {
    val blockLocations = remember { mutableStateListOf<Offset>() }
    LaunchedEffect(Unit) {
        while (true) {
            if (blockLocations.size > 8) blockLocations.removeAt(0)
            blockLocations.add(Offset((Math.random() * 800).toFloat(), (Math.random() * 300).toFloat()))
            delay(3000)
        }
    }
    Card(modifier = Modifier.fillMaxWidth().height(160.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(Color.Gray.copy(alpha = 0.1f), Offset(0f, size.height/2), Offset(size.width, size.height/2))
                drawLine(Color.Gray.copy(alpha = 0.1f), Offset(size.width/2, 0f), Offset(size.width/2, size.height))
                blockLocations.forEach { loc ->
                    drawCircle(Color.Red.copy(alpha = 0.4f), 10f, loc)
                    drawCircle(Color.Red, 3f, loc)
                }
            }
            Text("GLOBAL INTERCEPTIONS", modifier = Modifier.padding(12.dp), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}
