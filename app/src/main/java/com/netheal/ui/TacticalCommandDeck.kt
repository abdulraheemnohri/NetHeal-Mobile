package com.netheal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(modifier = Modifier.fillMaxSize().background(CyberTheme.Background).padding(16.dp).verticalScroll(rememberScrollState())) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))

        CorePerformanceMonitor()

        Spacer(modifier = Modifier.height(24.dp))
        Text("PREDICTIVE THREAT FORECASTER", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        PredictiveThreatForecaster()

        Spacer(modifier = Modifier.height(24.dp))
        Text("REAL-TIME INTERCEPTION MAP", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        ThreatOriginMap()

        Spacer(modifier = Modifier.height(24.dp))
        Text("HISTORICAL TRAFFIC (24H)", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        HistoricalTrafficChart(history)

        Spacer(modifier = Modifier.height(32.dp))
        Text("DIAGNOSTIC ARSENAL", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        DiagnosticTool("DNS Resolver", "Deep domain analysis", Icons.Default.Language) { showDnsTool = true }
        DiagnosticTool("Ping Utility", "Core latency profiling", Icons.Default.TapAndPlay) { showPingTool = true }
        DiagnosticTool("Whois Probe", "Asset ownership fetcher", Icons.Default.Info) { showWhoisTool = true }
        DiagnosticTool("LAN Discovery", "Subnet topology mapper", Icons.Default.SettingsEthernet) { showLanScanner = true }

        if (showDnsTool) DnsToolDialog(onDismiss = { showDnsTool = false })
        if (showPingTool) PingToolDialog(onDismiss = { showPingTool = false })
        if (showWhoisTool) WhoisToolDialog(onDismiss = { showWhoisTool = false })
        if (showLanScanner) LanScannerDialog(onDismiss = { showLanScanner = false })

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun PredictiveThreatForecaster() {
    val forecast = listOf(
        "14:00 - Data Exfiltration Risk: 12%",
        "18:00 - Port Scan Susceptibility: HIGH",
        "22:00 - C2 Beaconing Spike Forecasted",
        "02:00 - Background Sync Entropy Alert"
    )
    GlassCard {
        forecast.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (item.contains("HIGH")) CyberTheme.Danger else CyberTheme.Primary))
                Spacer(modifier = Modifier.width(12.dp))
                Text(item, color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun CorePerformanceMonitor() {
    var cpuLoad by remember { mutableFloatStateOf(0.12f) }
    var memoryUsage by remember { mutableStateOf("42 MB") }
    var neuralLatency by remember { mutableStateOf("0.8ms") }

    LaunchedEffect(Unit) {
        while(true) {
            cpuLoad = (0.05f + (Math.random() * 0.15f)).toFloat()
            memoryUsage = "${(40 + (Math.random() * 5).toInt())} MB"
            neuralLatency = "${String.format("%.2f", 0.5 + Math.random() * 0.5)}ms"
            delay(2000)
        }
    }

    GlassCard {
        Text("CORE PERFORMANCE MONITOR", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PerformanceItem("CPU LOAD", "${(cpuLoad * 100).toInt()}%", cpuLoad)
            PerformanceItem("KERNEL RAM", memoryUsage, 0.4f)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PerformanceItem("NEURAL LATENCY", neuralLatency, 0.2f)
            PerformanceItem("INFERENCE LOAD", "18%", 0.18f)
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
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
            color = CyberTheme.Primary,
            trackColor = CyberTheme.Border
        )
    }
}

@Composable
fun ThreatOriginMap() {
    val blockLocations = remember { mutableStateListOf<Offset>() }
    LaunchedEffect(Unit) {
        while (true) {
            if (blockLocations.size > 12) blockLocations.removeAt(0)
            blockLocations.add(Offset((Math.random() * 800).toFloat(), (Math.random() * 300).toFloat()))
            delay(2500)
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface),
        border = BorderStroke(1.dp, CyberTheme.Border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background grid
                for (i in 0..10) {
                    val x = (size.width / 10) * i
                    drawLine(CyberTheme.Border.copy(alpha = 0.2f), Offset(x, 0f), Offset(x, size.height))
                }
                for (i in 0..6) {
                    val y = (size.height / 6) * i
                    drawLine(CyberTheme.Border.copy(alpha = 0.2f), Offset(0f, y), Offset(size.width, y))
                }
                // Threat pings
                blockLocations.forEach { loc ->
                    drawCircle(CyberTheme.Danger.copy(alpha = 0.3f), 15f, loc)
                    drawCircle(CyberTheme.Danger, 4f, loc)
                }
            }
            Text("GLOBAL INTERCEPTIONS [SIM]", modifier = Modifier.padding(12.dp), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}
