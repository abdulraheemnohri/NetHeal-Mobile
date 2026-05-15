package com.netheal.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
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

    LaunchedEffect(Unit) {
        while (true) {
            val statsBytes = RustBridge.getAnalytics()
            if (statsBytes.isNotEmpty()) {
                try {
                    blockedCount = RustBridge.getBlockedCount()
                    scannedCount = RustBridge.getScannedCount()
                    securityScore = RustBridge.getSecurityScore()
                } catch (e: Exception) {}
            }
            delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010409))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))

        // Master Shield
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            ShieldHexagon(isShieldActive)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        isShieldActive = !isShieldActive
                        if (isShieldActive) RustBridge.startEngine() else RustBridge.stopEngine()
                        onToggleShield(isShieldActive)
                    },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = if (isShieldActive) Color(0xFF00FFA3) else Color.Red
                    )
                }
                Text(
                    if (isShieldActive) "SYSTEM PROTECTED" else "SYSTEM VULNERABLE",
                    color = if (isShieldActive) Color(0xFF00FFA3) else Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Tactical Stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCard("SCANNED", scannedCount.toString(), Icons.Default.Radar, Color.Cyan)
            StatCard("BLOCKED", blockedCount.toString(), Icons.Default.Security, Color.Red)
            StatCard("HEALTH", "$securityScore%", Icons.Default.Favorite, Color(0xFF00FFA3))
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

@Composable
fun NeuralTerminal() {
    val logs = remember { mutableStateListOf<String>() }
    var commandInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val events = listOf(
            "Kernel: Initializing memory protection...",
            "AI: Evaluating traffic heuristics.",
            "Engine: PSH optimization applied.",
            "Shield: Adaptive entropy check passed.",
            "DPI: Match signature detected in port 443.",
            "NetHeal: OMEGA speed engine synchronized."
        )
        while (true) {
            logs.add(0, "> ${events.random()}")
            if (logs.size > 20) logs.removeLast()
            delay(3000)
        }
    }

    Column {
        Text("NEURAL TERMINAL", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
            border = BorderStroke(1.dp, Color(0xFF161B22))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(logs) { log ->
                            Text(
                                log,
                                color = Color(0xFF00FFA3).copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$", color = Color(0xFF00FFA3), modifier = Modifier.padding(end = 8.dp))
                    BasicTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        cursorBrush = Brush.verticalGradient(listOf(Color(0xFF00FFA3), Color(0xFF00FFA3)))
                    )
                    IconButton(onClick = {
                        if (commandInput.startsWith("/")) {
                            logs.add(0, "> Executing $commandInput...")
                            when (commandInput) {
                                "/flush" -> RustBridge.clearLogs()
                                "/stats" -> logs.add(0, "Stats: Scanned ${RustBridge.getScannedCount()} Blocked ${RustBridge.getBlockedCount()}")
                            }
                        }
                        commandInput = ""
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Send, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AdvisorySection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF1C2128))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = Color(0xFF00FFA3))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("AI SECURITY ADVISORY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Zero-Day protection is currently active. 3 potential threats neutralized in the last hour.", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier
            .width(105.dp)
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        border = BorderStroke(1.dp, Color(0xFF161B22))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ShieldHexagon(active: Boolean) {
    Canvas(modifier = Modifier.size(180.dp)) {
        val path = Path().apply {
            val radius = size.minDimension / 2
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
            color = if (active) Color(0xFF00FFA3).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
        )
        drawPath(
            path = path,
            color = if (active) Color(0xFF00FFA3) else Color.Red,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
