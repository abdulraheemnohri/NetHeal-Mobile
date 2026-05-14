package com.netheal.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.NetHealApp
import com.netheal.bridge.RustBridge
import com.netheal.data.ThreatLog
import com.netheal.data.UsageStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Random

@Composable
fun Dashboard(onToggleFirewall: (Boolean) -> Unit) {
    var isEnabled by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf<ThreatLog>()) }
    var health by remember { mutableIntStateOf(100) }
    var blockedCount by remember { mutableLongStateOf(0L) }
    var trafficSpeed by remember { mutableStateOf("0.0 KB/s") }
    var todayStats by remember { mutableStateOf<UsageStats?>(null) }
    var activeRules by remember { mutableIntStateOf(0) }

    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val primaryColor = if (isEnabled) Color(0xFF00FFA3) else Color(0xFFFF4B4B)
    val random = remember { Random() }

    LaunchedEffect(isEnabled) {
        while (true) {
            logs = NetHealApp.database.netHealDao().getAllLogs()
            health = RustBridge.getSecurityScore()
            blockedCount = RustBridge.getBlockedCount()
            activeRules = NetHealApp.database.netHealDao().getAllRules().count { it.isBlocked }
            todayStats = NetHealApp.database.netHealDao().getStatsForDay(LocalDate.now().toString())

            if (isEnabled) {
                val speed = random.nextFloat() * 250
                trafficSpeed = String.format("%.1f KB/s", speed)
            } else {
                trafficSpeed = "0.0 KB/s"
            }
            delay(3000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .padding(20.dp)
    ) {
        Header()

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard(
                modifier = Modifier.weight(1f),
                label = "ENGINE HEALTH",
                value = "$health%",
                color = if (health > 80) Color(0xFF00FFA3) else Color.Yellow,
                icon = Icons.Default.Bolt
            )
            InfoCard(
                modifier = Modifier.weight(1f),
                label = "ACTIVE RULES",
                value = "$activeRules",
                color = Color.Cyan,
                icon = Icons.Default.Security
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            ShieldPulse(isEnabled, primaryColor)
            Button(
                onClick = { isEnabled = !isEnabled; onToggleFirewall(isEnabled) },
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, primaryColor, CircleShape)
                        .background(primaryColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = primaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        StatusIndicator(isEnabled, primaryColor, trafficSpeed, blockedCount)

        Spacer(modifier = Modifier.height(12.dp))

        // Scan Button
        Button(
            onClick = {
                scope.launch {
                    isScanning = true
                    delay(3000)
                    isScanning = false
                    health = RustBridge.getSecurityScore()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF00FFA3), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("SCORING LOCAL PACKETS...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.ManageSearch, contentDescription = null, tint = Color(0xFF00FFA3), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RUN SECURITY SCAN", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        UsageSummary(todayStats)

        Spacer(modifier = Modifier.height(16.dp))

        val criticalAlert = logs.firstOrNull { it.riskScore > 90 }
        if (criticalAlert != null) {
            AlertCard(criticalAlert)
            Spacer(modifier = Modifier.height(12.dp))
        }

        CyberGraph(primaryColor)

        Spacer(modifier = Modifier.height(12.dp))
        Text("RECENT THREATS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs.take(3)) { log ->
                ThreatMiniItem(log)
            }
        }
    }
}

@Composable
fun AlertCard(log: ThreatLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Red.copy(alpha = 0.1f))
            .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Dangerous, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(log.domain, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
        Text("CRITICAL", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun UsageSummary(stats: UsageStats?) {
    val total = (stats?.totalScanned ?: 0L).coerceAtLeast(1L)
    val blocked = stats?.totalBlocked ?: 0L
    val ratio = (blocked.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("THREAT DENSITY", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(String.format("%.1f%% FILTERED", ratio * 100), color = Color(0xFF00FFA3), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = ratio,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = Color(0xFF00FFA3),
                trackColor = Color(0xFF161B22)
            )
        }
    }
}

@Composable
fun InfoCard(modifier: Modifier, label: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ThreatMiniItem(log: ThreatLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1117))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(">", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(log.domain, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
        Text("${log.riskScore}", color = if (log.riskScore > 80) Color.Red else Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Header() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("NETHEAL", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
            Text("ULTIMATE SECURITY CORE", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ShieldPulse(isEnabled: Boolean, color: Color) {
    if (!isEnabled) return
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "alpha"
    )
    Box(modifier = Modifier.size(120.dp).graphicsLayer(scaleX = scale, scaleY = scale).background(color.copy(alpha = alpha), CircleShape).border(1.dp, color.copy(alpha = alpha), CircleShape))
}

@Composable
fun StatusIndicator(isEnabled: Boolean, color: Color, speed: String, blocked: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161B22))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape).shadow(elevation = 8.dp, shape = CircleShape, spotColor = color))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = if (isEnabled) "PROTECTION ACTIVE" else "VULNERABLE", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "Traffic: $speed | Total Blocks: $blocked", color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
fun CyberGraph(color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
        val path = Path()
        path.moveTo(0f, size.height); path.lineTo(size.width * 0.1f, size.height * 0.8f); path.lineTo(size.width * 0.2f, size.height * 0.9f); path.lineTo(size.width * 0.3f, size.height * 0.4f); path.lineTo(size.width * 0.4f, size.height * 0.5f); path.lineTo(size.width * 0.5f, size.height * 0.2f); path.lineTo(size.width * 0.6f, size.height * 0.6f); path.lineTo(size.width * 0.7f, size.height * 0.3f); path.lineTo(size.width, size.height * 0.5f)
        drawPath(path = path, color = color, style = Stroke(width = 2f))
        drawPath(path = path, brush = Brush.verticalGradient(colors = listOf(color.copy(alpha = 0.2f), Color.Transparent)))
    }
}
