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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
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
    val primaryColor = if (isEnabled) Color(0xFF00FFA3) else Color(0xFFFF4B4B)

    LaunchedEffect(isEnabled) {
        while (true) {
            logs = NetHealApp.database.threatLogDao().getAllLogs()
            delay(5000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .padding(20.dp)
    ) {
        Header(onNavigateToSettings)
        Spacer(modifier = Modifier.height(30.dp))

        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            ShieldPulse(isEnabled, primaryColor)
            Button(
                onClick = { isEnabled = !isEnabled; onToggleFirewall(isEnabled) },
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().border(2.dp, primaryColor, CircleShape).background(primaryColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = if (isEnabled) Icons.Default.Shield else Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(60.dp), tint = primaryColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        StatusIndicator(isEnabled, primaryColor)

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Shortcut
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onNavigateToFirewall,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = null, tint = Color(0xFF00FFA3))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Rules", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        CyberGraph(primaryColor)
        Spacer(modifier = Modifier.height(24.dp))
        Text("LIVE THREAT FEED", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(logs.take(15)) { log ->
                ThreatItem(log)
            }
        }
    }
}

@Composable
fun ThreatItem(log: ThreatLog) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF0D1117)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(">", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text("${log.action}: ${log.domain} | Score: ${log.riskScore}", color = Color.LightGray, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
fun Header(onNavigateToSettings: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("NETHEAL", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
            Text("MOBILE SECURITY ENGINE", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onNavigateToSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }
    }
}

@Composable
fun ShieldPulse(isEnabled: Boolean, color: Color) {
    if (!isEnabled) return
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.6f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart))
    val alpha by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart))
    Box(modifier = Modifier.size(140.dp).graphicsLayer(scaleX = scale, scaleY = scale).background(color.copy(alpha = alpha), CircleShape).border(1.dp, color.copy(alpha = alpha), CircleShape))
}

@Composable
fun StatusIndicator(isEnabled: Boolean, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF161B22)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape).shadow(10.dp, color, CircleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = if (isEnabled) "PROTECTION ACTIVE" else "SYSTEM VULNERABLE", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = if (isEnabled) "Analyzing packets in real-time" else "Tap shield to enable firewall", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun CyberGraph(color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        val path = Path()
        path.moveTo(0f, size.height); path.lineTo(size.width * 0.3f, size.height * 0.4f); path.lineTo(size.width * 0.5f, size.height * 0.2f); path.lineTo(size.width, size.height * 0.5f)
        drawPath(path = path, color = color, style = Stroke(width = 4f))
        drawPath(path = path, brush = Brush.verticalGradient(colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)))
    }
}
