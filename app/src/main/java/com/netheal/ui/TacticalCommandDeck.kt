package com.netheal.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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

    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(16.dp).verticalScroll(rememberScrollState())) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))

        CorePerformanceMonitor()

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("BIO-METRIC STATUS", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                PostureAwarenessCard()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("PROTOCOL ENTROPY", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                ProtocolEntropyRing()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("NEURAL PACKET MESH", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        NeuralMeshVisualizer()

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
fun ProtocolEntropyRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing)),
        label = "ring_rotate"
    )
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface),
        border = BorderStroke(1.dp, CyberTheme.Border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(60.dp).rotate(rotation)) {
                drawArc(CyberTheme.Primary, 0f, 120f, false, style = Stroke(width = 4f))
                drawArc(CyberTheme.Secondary, 130f, 100f, false, style = Stroke(width = 4f))
                drawArc(CyberTheme.Danger, 240f, 90f, false, style = Stroke(width = 4f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TCP", color = CyberTheme.Primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("UDP", color = CyberTheme.Secondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NeuralMeshVisualizer() {
    val nodes = remember { List(15) { Offset((Math.random() * 800).toFloat(), (Math.random() * 400).toFloat()) } }
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Reverse),
        label = "mesh_pulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface),
        border = BorderStroke(1.dp, CyberTheme.Border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in nodes.indices) {
                    for (j in i + 1 until nodes.size) {
                        val dist = (nodes[i] - nodes[j]).getDistance()
                        if (dist < 250) {
                            drawLine(
                                color = CyberTheme.Primary.copy(alpha = (1.0f - dist / 250f) * 0.2f),
                                start = nodes[i],
                                end = nodes[j],
                                strokeWidth = 1f
                            )
                        }
                    }
                }
                nodes.forEach { node ->
                    drawCircle(CyberTheme.Primary.copy(alpha = pulse * 0.3f), 8f, node)
                    drawCircle(CyberTheme.Primary, 3f, node)
                }
            }
            Text("LIVE TRAFFIC NODES", modifier = Modifier.padding(12.dp), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PostureAwarenessCard() {
    var isMoving by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while(true) {
            isMoving = Math.random() > 0.8
            delay(5000)
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface),
        border = BorderStroke(1.dp, CyberTheme.Border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isMoving) CyberTheme.Warning else CyberTheme.Primary))
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (isMoving) "ACTIVE" else "STATIC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("MOTION", color = Color.Gray, fontSize = 8.sp)
        }
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
                for (i in 0..10) {
                    val x = (size.width / 10) * i
                    drawLine(CyberTheme.Border.copy(alpha = 0.2f), Offset(x, 0f), Offset(x, size.height))
                }
                for (i in 0..6) {
                    val y = (size.height / 6) * i
                    drawLine(CyberTheme.Border.copy(alpha = 0.2f), Offset(0f, y), Offset(size.width, y))
                }
                blockLocations.forEach { loc ->
                    drawCircle(CyberTheme.Danger.copy(alpha = 0.3f), 15f, loc)
                    drawCircle(CyberTheme.Danger, 4f, loc)
                }
            }
            Text("GLOBAL INTERCEPTIONS [SIM]", modifier = Modifier.padding(12.dp), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}
