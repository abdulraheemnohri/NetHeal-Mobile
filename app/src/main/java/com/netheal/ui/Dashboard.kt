package com.netheal.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.delay

@Composable
fun Dashboard(
    onToggleFirewall: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFirewall: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf<ThreatLog>()) }
    var health by remember { mutableIntStateOf(100) }
    var blockedCount by remember { mutableLongStateOf(0L) }
    val primaryColor = if (isEnabled) Color(0xFF00FFA3) else Color(0xFFFF4B4B)

    LaunchedEffect(isEnabled) {
        while (true) {
            logs = NetHealApp.database.netHealDao().getAllLogs()
            health = RustBridge.getSystemHealth()
            blockedCount = RustBridge.getBlockedCount()
            delay(3000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .padding(20.dp)
    ) {
        Header(onNavigateToSettings)

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) { HealthIndicator(health) }
            Box(modifier = Modifier.weight(1f)) { BlockedCounter(blockedCount) }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            ShieldPulse(isEnabled, primaryColor)
            Button(
                onClick = { isEnabled = !isEnabled; onToggleFirewall(isEnabled) },
                modifier = Modifier.size(130.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().border(2.dp, primaryColor, CircleShape).background(primaryColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.Shield else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = primaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        StatusIndicator(isEnabled, primaryColor)

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onNavigateToFirewall,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = null, tint = Color(0xFF00FFA3), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Firewall Rules", color = Color.White, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        CyberGraph(primaryColor)

        Spacer(modifier = Modifier.height(10.dp))
        Text("LIVE THREAT FEED", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs.take(20)) { log ->
                ThreatItem(log)
            }
        }
    }
}

@Composable
fun HealthIndicator(health: Int) {
    Column {
        Text("ENGINE INTEGRITY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { health / 100f },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = if (health > 80) Color(0xFF00FFA3) else Color.Yellow,
            trackColor = Color(0xFF161B22)
        )
    }
}

@Composable
fun BlockedCounter(count: Long) {
    Column(horizontalAlignment = Alignment.End) {
        Text("BLOCKED ATTEMPTS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("$count", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun ThreatItem(log: ThreatLog) {
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
        Column {
            Text(log.domain, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Risk Score: ${log.riskScore} | Action: ${log.action}", color = Color.Gray, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}

@Composable
fun Header(onNavigateToSettings: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("NETHEAL", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
            Text("MOBILE SECURITY CORE", color = Color(0xFF00FFA3), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onNavigateToSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }
    }
}

@Composable
fun ShieldPulse(isEnabled: Boolean, color: Color) {
    if (!isEnabled) return
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "alpha"
    )
    Box(modifier = Modifier.size(130.dp).graphicsLayer(scaleX = scale, scaleY = scale).background(color.copy(alpha = alpha), CircleShape).border(1.dp, color.copy(alpha = alpha), CircleShape))
}

@Composable
fun StatusIndicator(isEnabled: Boolean, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161B22))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape).shadow(10.dp, color, CircleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = if (isEnabled) "PROTECTION ACTIVE" else "VULNERABLE", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = if (isEnabled) "Intercepting local traffic" else "Firewall is currently idle", color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
fun CyberGraph(color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(50.dp)) {
        val path = Path()
        path.moveTo(0f, size.height); path.lineTo(size.width * 0.1f, size.height * 0.8f); path.lineTo(size.width * 0.2f, size.height * 0.9f); path.lineTo(size.width * 0.3f, size.height * 0.4f); path.lineTo(size.width * 0.4f, size.height * 0.5f); path.lineTo(size.width * 0.5f, size.height * 0.2f); path.lineTo(size.width * 0.6f, size.height * 0.6f); path.lineTo(size.width * 0.7f, size.height * 0.3f); path.lineTo(size.width, size.height * 0.5f)
        drawPath(path = path, color = color, style = Stroke(width = 3f))
        drawPath(path = path, brush = Brush.verticalGradient(colors = listOf(color.copy(alpha = 0.2f), Color.Transparent)))
    }
}
