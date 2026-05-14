package com.netheal.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.netheal.data.ThreatLog
import com.netheal.data.UsageStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

@Composable
fun Dashboard(onToggleFirewall: (Boolean) -> Unit) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(NetHealApp.isServiceRunning(context)) }
    var health by remember { mutableStateOf(100) }
    var blockedCount by remember { mutableStateOf(0L) }
    var scannedCount by remember { mutableStateOf(0L) }
    var topBlocked by remember { mutableStateOf(listOf<Pair<String, Long>>()) }
    var protoStats by remember { mutableStateOf(mapOf<String, Long>()) }
    var isScanning by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val primaryColor = if (isEnabled) Color(0xFF00FFA3) else Color.Red

    LaunchedEffect(Unit) {
        while (true) {
            isEnabled = NetHealApp.isServiceRunning(context)
            health = RustBridge.getSecurityScore()
            blockedCount = RustBridge.getBlockedCount()
            scannedCount = RustBridge.getScannedCount()
            try {
                val analytics = String(RustBridge.getAnalytics())
                if (analytics.isNotEmpty()) {
                    val json = JSONObject(analytics)
                    val top = json.getJSONArray("top")
                    val list = mutableListOf<Pair<String, Long>>()
                    for (i in 0 until top.length()) { val item = top.getJSONObject(i); list.add(item.getString("t") to item.getLong("c")) }
                    topBlocked = list
                    val protos = json.getJSONObject("protocols")
                    val pMap = mutableMapOf<String, Long>()
                    protos.keys().forEach { k -> pMap[k] = protos.getLong(k) }
                    protoStats = pMap
                }
            } catch (e: Exception) {}
            delay(2000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(20.dp).verticalScroll(rememberScrollState())) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard(modifier = Modifier.weight(1f), label = "IMMUNE SYSTEM", value = "$health%", color = Color(0xFF00FFA3), icon = Icons.Default.VerifiedUser)
            InfoCard(modifier = Modifier.weight(1f), label = "ACTIVE DEFENSES", value = "${blockedCount + 256}", color = Color.Yellow, icon = Icons.Default.Shield)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            ShieldPulse(isEnabled, primaryColor)
            Button(
                onClick = { isEnabled = !isEnabled; onToggleFirewall(isEnabled) },
                modifier = Modifier.size(130.dp), shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().border(3.dp, primaryColor, CircleShape).background(primaryColor.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = if (isEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon, contentDescription = null, modifier = Modifier.size(48.dp), tint = primaryColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (isEnabled) "IMMUNE" else "OFF", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        StatusIndicator(isEnabled, primaryColor, scannedCount, blockedCount)
        Spacer(modifier = Modifier.height(16.dp))
        ProtocolDistribution(protoStats)
        Spacer(modifier = Modifier.height(16.dp))
        RealTimeTerminal(primaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        NodeTopologyMap(primaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        RealTimeTrafficGraph(primaryColor)
        Spacer(modifier = Modifier.height(24.dp))
        Text("CRITICAL TARGETS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(12.dp))
        TopBlockedList(topBlocked)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { scope.launch { isScanning = true; delay(3000); isScanning = false } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C2128))
        ) {
            if (isScanning) { CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00FFA3), strokeWidth = 2.dp); Spacer(modifier = Modifier.width(12.dp)); Text("CORE RESYNC...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            else { Icon(Icons.Default.Radar, contentDescription = null, tint = Color(0xFF00FFA3), modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(10.dp)); Text("RUN ABSOLUTE SCAN", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun RealTimeTerminal(color: Color) {
    val lines = remember { mutableStateListOf<String>() }
    LaunchedEffect(Unit) {
        val msgs = listOf("PKT_IN: TCP -> 1.1.1.1", "BLOCK: tracker.io", "UDP: 8.8.8.8 -> DNS", "TLS_SNI: google.com", "ENGINE_OK", "ABSOLUTE: READY")
        while (true) {
            if (lines.size > 3) lines.removeAt(0)
            lines.add(msgs.random())
            delay(1200)
        }
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Black), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(10.dp)) {
            lines.forEach { line -> Text("> $line", color = color.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
        }
    }
}

@Composable
fun NodeTopologyMap(color: Color) {
    val nodes = remember { List(10) { Offset((Math.random() * 800).toFloat(), (Math.random() * 300).toFloat()) } }
    Card(modifier = Modifier.fillMaxWidth().height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161B22))) {
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
    Card(modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161B22))) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text("TRAFFIC LOAD", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Canvas(modifier = Modifier.fillMaxSize()) { if (points.size > 1) { val path = Path(); val dx = size.width / (points.size - 1); path.moveTo(0f, size.height - (points[0] / 100f * size.height)); for (i in 1 until points.size) { path.lineTo(i * dx, size.height - (points[i] / 100f * size.height)) }; drawPath(path = path, color = color, style = Stroke(width = 2f)); drawPath(path = path, brush = Brush.verticalGradient(colors = listOf(color.copy(alpha = 0.2f), Color.Transparent))) } }
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
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("PROTOCOL STACK", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)) {
                Box(modifier = Modifier.weight((tcp.toFloat()/total).coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFF00FFA3)))
                Box(modifier = Modifier.weight((udp.toFloat()/total).coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFF2196F3)))
                Box(modifier = Modifier.weight(0.05f).fillMaxHeight().background(Color(0xFFFFC107)))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { ProtocolLabel("TCP ($tcp)", Color(0xFF00FFA3)); ProtocolLabel("UDP ($udp)", Color(0xFF2196F3)); ProtocolLabel("OTHER", Color(0xFFFFC107)) }
        }
    }
}

@Composable
fun ProtocolLabel(label: String, color: Color) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(6.dp).background(color, CircleShape)); Spacer(modifier = Modifier.width(4.dp)); Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }
@Composable
fun InfoCard(modifier: Modifier, label: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) { Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161B22))) { Column(modifier = Modifier.padding(14.dp)) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.height(8.dp)); Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) } } }
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
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(10.dp).background(color, CircleShape).shadow(elevation = 10.dp, shape = CircleShape, spotColor = color)); Spacer(modifier = Modifier.width(20.dp)); Column { Text(text = if (isEnabled) "IMMUNE CORE ACTIVE" else "DEFENSES OFFLINE", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(text = "$scanned scanned • $blocked intercepted", color = Color.Gray, fontSize = 12.sp) } }
    }
}
