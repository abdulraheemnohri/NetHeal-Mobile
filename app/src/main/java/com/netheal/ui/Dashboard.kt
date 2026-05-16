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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        // Quantum Stealth Layer
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

            // Advanced Master Shield
            Box(
                modifier = Modifier.fillMaxWidth().height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                ShieldPulseEffect(isShieldActive)
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
                        if (isShieldActive) "SYSTEM STATUS: SECURE" else "SYSTEM STATUS: VULNERABLE",
                        color = if (isShieldActive) CyberTheme.Primary else CyberTheme.Danger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    if (isStealthActive && isShieldActive) {
                        Text("QUANTUM-SAFE STEALTH ENABLED", color = CyberTheme.Secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
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

            // Neural Terminal
            NeuralTerminal()

            Spacer(modifier = Modifier.height(24.dp))

            // Advisory
            AdvisorySection()

            Spacer(modifier = Modifier.height(40.dp))
        }
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
fun ShieldPulseEffect(active: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(2000), repeatMode = RepeatMode.Reverse),
        label = "pulse_scale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing)),
        label = "pulse_rotate"
    )

    Canvas(modifier = Modifier.size(220.dp).rotate(rotation)) {
        val path = Path().apply {
            val radius = (size.minDimension / 2) * scale
            val centerX = size.width / 2
            val centerY = size.height / 2
            for (i in 0..5) {
                val angle = Math.toRadians(60.0 * i - 30.0)
                val x = centerX + radius * Math.cos(angle).toFloat()
                val y = centerY + radius * Math.sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(
            path = path,
            color = if (active) CyberTheme.Primary.copy(alpha = 0.05f) else CyberTheme.Danger.copy(alpha = 0.05f)
        )
        drawPath(
            path = path,
            color = if (active) CyberTheme.Primary else CyberTheme.Danger,
            style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
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
