package com.netheal.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.bridge.RustBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun TacticalCommandDeck() {
    var packets by remember { mutableStateOf(listOf<PacketFlow>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val analytics = String(RustBridge.getAnalytics())
                if (analytics.isNotEmpty()) {
                    val json = JSONObject(analytics)
                    val conns = json.getJSONArray("conns")
                    val newList = mutableListOf<PacketFlow>()
                    for (i in 0 until conns.length()) {
                        val obj = conns.getJSONObject(i)
                        newList.add(PacketFlow(obj.getString("t"), obj.getString("a"), (Math.random() * 1500).toInt(), "TCP"))
                    }
                    packets = (newList + packets).take(20)
                }
            } catch (e: Exception) {}
            delay(1000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF010409)).padding(16.dp)) {
        Text("TACTICAL COMMAND DECK", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth().height(200.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
            Box(modifier = Modifier.padding(16.dp)) {
                Text("GLOBAL ATTACK GLOBE", color = Color.Gray, fontSize = 8.sp)
                AttackGlobe()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("LIVE PACKET STREAM (3D DEPTH)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(packets) { index, packet ->
                PacketStreamItem(packet, index)
            }
        }
    }
}

@Composable
fun AttackGlobe() {
    val infiniteTransition = rememberInfiniteTransition(label = "globe")
    val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(20000, easing = LinearEasing)), label = "rotation")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = center
        val radius = size.height * 0.4f
        drawCircle(color = Color.Cyan.copy(alpha = 0.1f), radius = radius, center = center, style = Stroke(width = 2f))

        // Grid lines
        for (i in 0 until 18) {
            val angle = i * 20f
            drawArc(color = Color.Gray.copy(alpha = 0.1f), startAngle = angle, sweepAngle = 2f, useCenter = true, style = Stroke(width = 1f))
        }
    }
}

@Composable
fun PacketStreamItem(p: PacketFlow, index: Int) {
    val alpha = (1f - (index * 0.05f)).coerceAtLeast(0.2f)
    val scale = (1f - (index * 0.02f)).coerceAtLeast(0.8f)

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF00FFA3).copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(Color(0xFF00FFA3), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(p.target, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${p.appId} • ${p.protocol}", color = Color.Gray, fontSize = 9.sp)
            }
            Text("${p.size}B", color = Color.Cyan, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}

data class PacketFlow(val target: String, val appId: String, val size: Int, val protocol: String)
