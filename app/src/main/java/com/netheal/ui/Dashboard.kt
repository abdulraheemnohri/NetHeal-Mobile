package com.netheal.ui

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.NetHealApp
import com.netheal.bridge.RustBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

data class Connection(val target: String, val appId: String)

@Composable
fun Dashboard(onToggleVpn: (Boolean) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("netheal_prefs", 0)
    var isEnabled by remember { mutableStateOf(false) }
    var health by remember { mutableIntStateOf(100) }
    var blockedCount by remember { mutableLongStateOf(0) }
    var scannedCount by remember { mutableLongStateOf(0) }
    var topBlocked by remember { mutableStateOf(listOf<Pair<String, Long>>()) }
    var protoStats by remember { mutableStateOf(mapOf<String, Long>()) }
    var appUsage by remember { mutableStateOf(mapOf<String, UsageInfo>()) }
    var activeConnections by remember { mutableStateOf(listOf<Connection>()) }
    var observedCount by remember { mutableIntStateOf(0) }
    var isLockdown by remember { mutableStateOf(prefs.getBoolean("lockdown_mode", false)) }
    var performanceMode by remember { mutableStateOf(prefs.getBoolean("performance_mode", false)) }
    var boosterActive by remember { mutableStateOf(prefs.getBoolean("booster_active", false)) }
    var multipathActive by remember { mutableStateOf(prefs.getBoolean("multipath_active", false)) }
    var julesActive by remember { mutableStateOf(prefs.getBoolean("jules_api_active", false)) }

    val primaryColor = if (isEnabled) (if (isLockdown) Color.Yellow else Color(0xFF00FFA3)) else Color.Red

    LaunchedEffect(Unit) {
        while (true) {
            isEnabled = NetHealApp.isServiceRunning(context)
            health = RustBridge.getSecurityScore()
            blockedCount = RustBridge.getBlockedCount()
            scannedCount = RustBridge.getScannedCount()
            isLockdown = prefs.getBoolean("lockdown_mode", false)
            performanceMode = prefs.getBoolean("performance_mode", false)
            boosterActive = prefs.getBoolean("booster_active", false)
            multipathActive = prefs.getBoolean("multipath_active", false)
            julesActive = prefs.getBoolean("jules_api_active", false)
            try {
                val analytics = String(RustBridge.getAnalytics())
                if (analytics.isNotEmpty()) {
                    val json = JSONObject(analytics)
                    topBlocked = json.getJSONArray("top").let { arr -> List(arr.length()) { i -> arr.getJSONObject(i).let { it.getString("t") to it.getLong("c") } } }
                    protoStats = json.getJSONObject("protocols").let { obj -> obj.keys().asSequence().associateWith { obj.getLong(it) } }
                    appUsage = json.getJSONObject("usage").let { obj -> obj.keys().asSequence().associateWith { k -> obj.getJSONObject(k).let { UsageInfo(it.getLong("s"), it.getLong("r"), it.getLong("p")) } } }
                    activeConnections = json.getJSONArray("conns").let { arr -> List(arr.length()) { i -> arr.getJSONObject(i).let { Connection(it.getString("t"), it.getString("a")) } } }
                    observedCount = json.optInt("observed", 0)
                }
            } catch (e: Exception) {}
            delay(2000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(20.dp).verticalScroll(rememberScrollState())) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))

        if (julesActive) {
            JulesAdvisorySection()
            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard(modifier = Modifier.weight(1f), label = "DEFENSE CORE", value = "$health%", color = Color(0xFF00FFA3), icon = Icons.Default.VerifiedUser)
            InfoCard(modifier = Modifier.weight(1f), label = "OPTIMIZATION", value = if (performanceMode) "ULTRA" else "PEAK", color = Color.Cyan, icon = Icons.Default.Bolt)
        }

        if (boosterActive || multipathActive) {
            Spacer(modifier = Modifier.height(12.dp))
            BoosterStatusRow(boosterActive, multipathActive)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            ShieldPulse(isEnabled, primaryColor)
            Box(modifier = Modifier.size(110.dp).background(primaryColor.copy(alpha = 0.1f), CircleShape).border(2.dp, primaryColor, CircleShape).clickable { onToggleVpn(!isEnabled) }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (isEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(40.dp))
                    Text(if (isEnabled) "SECURE" else "OFFLINE", color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (isEnabled) {
                IconButton(onClick = { RustBridge.setSecurityLevel(0); Toast.makeText(context, "Master Protection Deactivated", Toast.LENGTH_SHORT).show() }, modifier = Modifier.align(Alignment.TopEnd)) {
                   Icon(Icons.Default.PowerSettingsNew, contentDescription = "Deactivate", tint = Color.Gray.copy(alpha = 0.5f))
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        StatusIndicator(isEnabled, primaryColor, scannedCount, blockedCount)
        Spacer(modifier = Modifier.height(24.dp))
        BandwidthGauge(appUsage)
        Spacer(modifier = Modifier.height(24.dp))
        Text("ACTIVE TELEMETRY NODES", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        NodeTopologyMap(primaryColor)
        Spacer(modifier = Modifier.height(24.dp))
        Text("TOP THREAT VECTORS", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        TopBlockedList(topBlocked)
        Spacer(modifier = Modifier.height(24.dp))
        ProtocolDistribution(protoStats)
        Spacer(modifier = Modifier.height(24.dp))
        RealTimeTrafficGraph(primaryColor)
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun JulesAdvisorySection() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF00FFA3).copy(alpha = 0.05f)), border = BorderStroke(1.dp, Color(0xFF00FFA3).copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF00FFA3), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("JULES AI ADVISORY", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("System health optimal. Analyzing active streams.", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun BoosterStatusRow(booster: Boolean, multipath: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (booster) BoosterBadge("OMEGA BOOST", Color.Cyan)
        if (multipath) BoosterBadge("LINK BONDING", Color(0xFF00FFA3))
    }
}

@Composable
fun BoosterBadge(label: String, color: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.1f)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BandwidthGauge(usage: Map<String, UsageInfo>) {
    val totalSent = usage.values.sumOf { it.sent }
    val totalRecv = usage.values.sumOf { it.recv }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("DATA FLOW GAUGES", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Gauge(modifier = Modifier.weight(1f), label = "UPLOAD", value = formatSize(totalSent), color = Color(0xFF00FFA3))
                Gauge(modifier = Modifier.weight(1f), label = "DOWNLOAD", value = formatSize(totalRecv), color = Color(0xFF2196F3))
            }
        }
    }
}

@Composable
fun Gauge(modifier: Modifier, label: String, value: String, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = 0.6f, modifier = Modifier.fillMaxSize(), color = color, strokeWidth = 4.dp, trackColor = Color(0xFF161B22))
            Text(value.split(" ").first(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NodeTopologyMap(color: Color) {
    val nodes = remember { List(10) { Offset((Math.random() * 800).toFloat(), (Math.random() * 300).toFloat()) } }
    Card(modifier = Modifier.fillMaxWidth().height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text("NODE TOPOLOGY", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Canvas(modifier = Modifier.fillMaxSize()) {
                nodes.forEach { node -> drawCircle(color = color.copy(alpha = 0.2f), radius = 4f, center = node) }
                for (i in 0 until nodes.size - 1) { drawLine(color = color.copy(alpha = 0.1f), start = nodes[i], end = nodes[i+1], strokeWidth = 1f) }
            }
        }
    }
}

@Composable
fun RealTimeTrafficGraph(color: Color) {
    val points = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) { while (true) { if (points.size > 20) points.removeAt(0); points.add((Math.random() * 100).toFloat()); delay(500) } }
    Card(modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text("TRAFFIC LOAD", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.size > 1) {
                    val path = Path()
                    val dx = size.width / (points.size - 1)
                    path.moveTo(0f, size.height - (points[0] / 100f * size.height))
                    for (i in 1 until points.size) { path.lineTo(i * dx, size.height - (points[i] / 100f * size.height)) }
                    drawPath(path = path, color = color, style = Stroke(width = 2f))
                    drawPath(path = path, brush = Brush.verticalGradient(colors = listOf(color.copy(alpha = 0.2f), Color.Transparent)))
                }
            }
        }
    }
}

@Composable
fun TopBlockedList(targets: List<Pair<String, Long>>) {
    if (targets.isEmpty()) { Text("No threats captured yet.", color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.padding(8.dp)); return }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        targets.forEach { (name, count) ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF161B22)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f)); Text("$count BLOCKS", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProtocolDistribution(stats: Map<String, Long>) {
    val tcp = stats["6"] ?: 0L
    val udp = stats["17"] ?: 0L
    val total = (tcp + udp).coerceAtLeast(1L)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("PROTOCOL STACK", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)) {
                Box(modifier = Modifier.weight((tcp.toFloat()/total).coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFF00FFA3)))
                Box(modifier = Modifier.weight((udp.toFloat()/total).coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFF2196F3)))
                Box(modifier = Modifier.weight(0.05f).fillMaxHeight().background(Color(0xFFFFC107)))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ProtocolLabel("TCP ($tcp)", Color(0xFF00FFA3))
                ProtocolLabel("UDP ($udp)", Color(0xFF2196F3))
                ProtocolLabel("OTHER", Color(0xFFFFC107))
            }
        }
    }
}

@Composable
fun ProtocolLabel(label: String, color: Color) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(6.dp).background(color, CircleShape)); Spacer(modifier = Modifier.width(4.dp)); Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }

@Composable
fun InfoCard(modifier: Modifier, label: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun Header() { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column { Text("NETHEAL", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp); Text("ABSOLUTE SECURITY CORE", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold) } } }

@Composable
fun ShieldPulse(isEnabled: Boolean, color: Color) {
    if (!isEnabled) return
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.6f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "scale")
    val alpha by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "alpha")
    Box(modifier = Modifier.size(130.dp).graphicsLayer(scaleX = scale, scaleY = scale).background(color.copy(alpha = alpha), CircleShape).border(1.dp, color.copy(alpha = alpha), CircleShape))
}

@Composable
fun StatusIndicator(isEnabled: Boolean, color: Color, scanned: Long, blocked: Long) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(color, CircleShape).shadow(elevation = 10.dp, shape = CircleShape, spotColor = color))
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = if (isEnabled) "IMMUNE CORE ACTIVE" else "DEFENSES OFFLINE", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "$scanned scanned • $blocked intercepted", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
