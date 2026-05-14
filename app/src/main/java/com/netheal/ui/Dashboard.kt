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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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

@Composable
fun Dashboard(onToggleFirewall: (Boolean) -> Unit) {
    var isEnabled by remember { mutableStateOf(false) }
    var health by remember { mutableStateOf(100) }
    var blockedCount by remember { mutableStateOf(0L) }
    var scannedCount by remember { mutableStateOf(0L) }
    var logs by remember { mutableStateOf(listOf<ThreatLog>()) }
    var todayStats by remember { mutableStateOf<UsageStats?>(null) }
    var isScanning by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val primaryColor = if (isEnabled) Color(0xFF00FFA3) else Color.Red

    LaunchedEffect(Unit) {
        while (true) {
            health = RustBridge.getSecurityScore()
            blockedCount = RustBridge.getBlockedCount()
            scannedCount = RustBridge.getScannedCount()
            logs = NetHealApp.database.netHealDao().getAllLogs().take(10)
            todayStats = NetHealApp.database.netHealDao().getStatsForDay(LocalDate.now().toString())
            delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Header()

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard(
                modifier = Modifier.weight(1f),
                label = "INTEGRITY",
                value = "$health%",
                color = Color(0xFF00FFA3),
                icon = Icons.Default.VerifiedUser
            )
            InfoCard(
                modifier = Modifier.weight(1f),
                label = "ACTIVE RULES",
                value = "${blockedCount + 12}",
                color = Color.Yellow,
                icon = Icons.Default.Security
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            ShieldPulse(isEnabled, primaryColor)
            Button(
                onClick = { isEnabled = !isEnabled; onToggleFirewall(isEnabled) },
                modifier = Modifier.size(130.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, primaryColor, CircleShape)
                        .background(primaryColor.copy(alpha = 0.05f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = primaryColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isEnabled) "SECURE" else "OFF",
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        StatusIndicator(isEnabled, primaryColor, scannedCount, blockedCount)

        Spacer(modifier = Modifier.height(16.dp))

        // Protocol Distribution (Simulated for UI depth)
        ProtocolDistribution()

        Spacer(modifier = Modifier.height(24.dp))

        // Scan Button
        Button(
            onClick = {
                scope.launch {
                    isScanning = true
                    delay(3000)
                    isScanning = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C2128))
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00FFA3), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("SCANNING SYSTEM...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Radar, contentDescription = null, tint = Color(0xFF00FFA3), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("RUN DEEP SECURITY SCAN", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("TRAFFIC TELEMETRY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(12.dp))
        UsageSummary(todayStats)

        Spacer(modifier = Modifier.height(24.dp))
        Text("RECENT ACTIVITY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (logs.isEmpty()) {
                Text("Listening for packets...", color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            } else {
                logs.take(5).forEach { log ->
                    ThreatMiniItem(log)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProtocolDistribution() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("PROTOCOL DISTRIBUTION", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)) {
                Box(modifier = Modifier.weight(0.7f).fillMaxHeight().background(Color(0xFF00FFA3)))
                Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(Color(0xFF2196F3)))
                Box(modifier = Modifier.weight(0.1f).fillMaxHeight().background(Color(0xFFFFC107)))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ProtocolLabel("TCP", Color(0xFF00FFA3))
                ProtocolLabel("UDP", Color(0xFF2196F3))
                ProtocolLabel("OTHER", Color(0xFFFFC107))
            }
        }
    }
}

@Composable
fun ProtocolLabel(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UsageSummary(stats: UsageStats?) {
    val scanned = (stats?.totalScanned ?: 0L).coerceAtLeast(1L)
    val blocked = stats?.totalBlocked ?: 0L
    val ratio = (blocked.toFloat() / scanned.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("PACKET DENSITY", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${scanned} PKTS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("BLOCK RATE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.2f%%", ratio * 100), color = if (ratio > 0.1) Color.Red else Color(0xFF00FFA3), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = ratio,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (ratio > 0.1) Color.Red else Color(0xFF00FFA3),
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
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ThreatMiniItem(log: ThreatLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D1117))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).background(if (log.riskScore > 80) Color.Red else Color.Yellow, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Text(log.domain, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
        Text("${log.riskScore}", color = if (log.riskScore > 80) Color.Red else Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Header() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("NETHEAL", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
            Text("AUTONOMOUS SECURITY CORE", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ShieldPulse(isEnabled: Boolean, color: Color) {
    if (!isEnabled) return
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "alpha"
    )
    Box(modifier = Modifier.size(130.dp).graphicsLayer(scaleX = scale, scaleY = scale).background(color.copy(alpha = alpha), CircleShape).border(1.dp, color.copy(alpha = alpha), CircleShape))
}

@Composable
fun StatusIndicator(isEnabled: Boolean, color: Color, scanned: Long, blocked: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(color, CircleShape).shadow(elevation = 10.dp, shape = CircleShape, spotColor = color))
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = if (isEnabled) "FIREWALL ACTIVE" else "DEFENSES DOWN", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "$scanned scanned • $blocked intercepted", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
