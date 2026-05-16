package com.netheal.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.bridge.RustBridge
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
fun Dashboard(onToggleShield: (Boolean) -> Unit) {
    var isShieldActive by remember { mutableStateOf(false) }
    var securityScore by remember { mutableIntStateOf(85) }
    var blockedCount by remember { mutableLongStateOf(0L) }
    var scannedCount by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("netheal_prefs", android.content.Context.MODE_PRIVATE)

    val isStealthActive = prefs.getBoolean("stealth_active", false)
    val isBoosterActive = prefs.getBoolean("booster_active", false)

    LaunchedEffect(Unit) {
        while (true) {
            try {
                blockedCount = RustBridge.getBlockedCount()
                scannedCount = RustBridge.getScannedCount()
                securityScore = RustBridge.getSecurityScore()
            } catch (e: Exception) {}
            delay(2000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CyberBackground()

        if (isShieldActive && isStealthActive && isBoosterActive) {
            QuantumOverlay()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Header()
            Spacer(modifier = Modifier.height(32.dp))

            // 3D Global Threat Core
            Box(
                modifier = Modifier.fillMaxWidth().height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                GlobalThreatCore(isShieldActive)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            isShieldActive = !isShieldActive
                            if (isShieldActive) RustBridge.startEngine() else RustBridge.stopEngine()
                            onToggleShield(isShieldActive)
                        },
                        modifier = Modifier.size(120.dp)
                    ) {
                        Icon(
                            if (isShieldActive) Icons.Default.Shield else Icons.Default.ShieldMoon,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = if (isShieldActive) CyberTheme.Primary else CyberTheme.Danger
                        )
                    }
                    Text(
                        if (isShieldActive) "CORE PROTECTION: ACTIVE" else "CORE PROTECTION: OFFLINE",
                        color = if (isShieldActive) CyberTheme.Primary else CyberTheme.Danger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tactical Metrics
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCard("TRAFFIC", scannedCount.toString(), Icons.Default.Radar, CyberTheme.Secondary)
                StatCard("THREATS", blockedCount.toString(), Icons.Default.Security, CyberTheme.Danger)
                StatCard("INTEGRITY", "${securityScore}%", Icons.Default.Favorite, CyberTheme.Primary)
            }

            Spacer(modifier = Modifier.height(24.dp))
            NeuroLinkInterface()
            Spacer(modifier = Modifier.height(24.dp))
            NeuralTerminal()
            Spacer(modifier = Modifier.height(24.dp))
            AdvisorySection()
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun NeuroLinkInterface() {
    var isListening by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "neurolink")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "wave"
    )

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                if (isListening) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(CyberTheme.Secondary.copy(alpha = 0.2f), radius = size.minDimension / 2 * waveScale)
                    }
                }
                IconButton(onClick = { isListening = !isListening }) {
                    Icon(if (isListening) Icons.Default.Mic else Icons.Default.MicNone, null, tint = if (isListening) CyberTheme.Secondary else Color.Gray)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("NEURO-LINK TACTICAL", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Text(if (isListening) "Listening for voice commands..." else "Tap to engage voice-link simulation", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun GlobalThreatCore(active: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "core")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(15000, easing = LinearEasing)),
        label = "core_rotate"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(3000), repeatMode = RepeatMode.Reverse),
        label = "core_scale"
    )

    Canvas(modifier = Modifier.size(240.dp).rotate(rotation)) {
        val color = if (active) CyberTheme.Primary else CyberTheme.Danger
        drawCircle(
            color = color.copy(alpha = 0.05f),
            radius = (size.minDimension / 2) * scale
        )
        drawCircle(
            color = color,
            radius = (size.minDimension / 2) * scale,
            style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 15f), 0f))
        )
        drawOval(
            color = color.copy(alpha = 0.3f),
            topLeft = center.copy(x = center.x - 100f * scale, y = center.y - 40f * scale),
            size = androidx.compose.ui.geometry.Size(200f * scale, 80f * scale),
            style = Stroke(width = 2f)
        )
        drawOval(
            color = CyberTheme.Secondary.copy(alpha = 0.2f),
            topLeft = center.copy(x = center.x - 40f * scale, y = center.y - 100f * scale),
            size = androidx.compose.ui.geometry.Size(80f * scale, 200f * scale),
            style = Stroke(width = 2f)
        )
    }
}

@Composable
fun QuantumOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "quantum")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(animation = tween(3000), repeatMode = RepeatMode.Reverse),
        label = "quantum_alpha"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(CyberTheme.Secondary.copy(alpha = alpha), Color.Transparent),
                center = center,
                radius = size.maxDimension / 1.5f
            )
        )
    }
}

@Composable
fun NeuralTerminal() {
    val logs = remember { mutableStateListOf<String>() }
    var commandInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val events = listOf(
            "KERNEL :: Thread isolation priority set to HIGH",
            "AI :: Analyzing suspicious TCP stream from PID 4201",
            "SHIELD :: Entropy threshold adjusted for deep packet inspection",
            "BOOSTER :: Link bonding optimized (WIFI + SIM-1)",
            "DPI :: Malicious signature detected - Action: DROPPED",
            "SYSTEM :: Omega engine health status nominal"
        )
        while (true) {
            logs.add(0, "> ${events.random()}")
            if (logs.size > 15) logs.removeLast()
            delay(4000)
        }
    }

    Column {
        Text("NEURAL COMMAND TERMINAL", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))
        GlassCard(modifier = Modifier.height(200.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs) { log ->
                        Text(
                            log,
                            color = CyberTheme.Primary.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
            Divider(color = CyberTheme.Border, modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("user@netheal:~$", color = CyberTheme.Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    cursorBrush = Brush.verticalGradient(listOf(CyberTheme.Primary, CyberTheme.Primary))
                )
                IconButton(onClick = {
                    if (commandInput.isNotBlank()) {
                        logs.add(0, "$ ${commandInput}")
                        if (commandInput == "/flush") RustBridge.clearLogs()
                        commandInput = ""
                    }
                }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = CyberTheme.Primary)
                }
            }
        }
    }
}

@Composable
fun AdvisorySection() {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(CyberTheme.Primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Info, null, tint = CyberTheme.Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("AI SECURITY ADVISORY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Zero-Day heuristics optimized for current network profile.", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.width(105.dp).height(110.dp),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface),
        border = BorderStroke(1.dp, CyberTheme.Border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
