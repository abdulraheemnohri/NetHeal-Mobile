package com.netheal.ui

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.data.HourlyUsage

object CyberTheme {
    val Primary = Color(0xFF00FFA3)
    val Secondary = Color(0xFF00E5FF)
    val Background = Color(0xFF010409)
    val Surface = Color(0xFF0D1117)
    val Border = Color(0xFF30363D)
    val Danger = Color(0xFFFF3B30)
    val Warning = Color(0xFFFFD60A)
    val AccentV3 = Color(0xFF7000FF)
    val CardGradient = Brush.verticalGradient(listOf(Color(0xFF161B22), Color(0xFF0D1117)))
}

@Composable
fun CyberBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing)),
        label = "offset"
    )
    Box(modifier = Modifier.fillMaxSize().background(CyberTheme.Background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStep = 60f
            for (x in 0..(size.width / gridStep).toInt()) {
                val posX = x * gridStep + (offset % gridStep)
                drawLine(CyberTheme.Border.copy(alpha = 0.08f), Offset(posX, 0f), Offset(posX, size.height))
            }
            for (y in 0..(size.height / gridStep).toInt()) {
                val posY = y * gridStep + (offset % gridStep)
                drawLine(CyberTheme.Border.copy(alpha = 0.08f), Offset(0f, posY), Offset(size.width, posY))
            }
        }
        // V3 Refraction Glow
        Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(
            colors = listOf(CyberTheme.AccentV3.copy(alpha = 0.03f), Color.Transparent),
            center = Offset(100f, 100f),
            radius = 1000f
        )))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header() {
    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val syncAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "sync_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "NETHEAL v3",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                modifier = Modifier.shadow(12.dp, CircleShape, spotColor = CyberTheme.Primary)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CyberTheme.Primary.copy(alpha = syncAlpha)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "OMEGA CORE SYNCHRONIZED",
                    color = CyberTheme.Primary.copy(alpha = syncAlpha),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
        Row {
            IconButton(onClick = {}) { Icon(Icons.Default.Tune, null, tint = Color.Gray) }
            BadgedBox(badge = { Badge(containerColor = CyberTheme.Danger) { Text("5") } }) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberTheme.Border.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
fun CyberButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDanger) CyberTheme.Danger.copy(alpha = 0.15f) else CyberTheme.Primary.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isDanger) CyberTheme.Danger else CyberTheme.Primary.copy(alpha = 0.6f))
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (isDanger) CyberTheme.Danger else CyberTheme.Primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}

@Composable
fun DiagnosticTool(title: String, desc: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, CyberTheme.Border.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberTheme.Secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = CyberTheme.Secondary, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(desc, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable fun DnsToolDialog(onDismiss: () -> Unit) { var host by remember { mutableStateOf("google.com") }; AlertDialog(onDismissRequest = onDismiss, containerColor = CyberTheme.Surface, title = { Text("DNS RESOLVER", color = Color.White, fontWeight = FontWeight.Bold) }, text = { OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Hostname") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }, confirmButton = { TextButton(onClick = onDismiss) { Text("RESOLVE", color = CyberTheme.Primary) } }) }
@Composable fun PingToolDialog(onDismiss: () -> Unit) { var host by remember { mutableStateOf("1.1.1.1") }; AlertDialog(onDismissRequest = onDismiss, containerColor = CyberTheme.Surface, title = { Text("PING UTILITY", color = Color.White, fontWeight = FontWeight.Bold) }, text = { OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("IP Address") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }, confirmButton = { TextButton(onClick = onDismiss) { Text("PING", color = CyberTheme.Primary) } }) }
@Composable fun WhoisToolDialog(onDismiss: () -> Unit) { var host by remember { mutableStateOf("github.com") }; AlertDialog(onDismissRequest = onDismiss, containerColor = CyberTheme.Surface, title = { Text("WHOIS PROBE", color = Color.White, fontWeight = FontWeight.Bold) }, text = { OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Domain") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }, confirmButton = { TextButton(onClick = onDismiss) { Text("QUERY", color = CyberTheme.Primary) } }) }
@Composable fun LanScannerDialog(onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, containerColor = CyberTheme.Surface, title = { Text("LAN DISCOVERY", color = Color.White, fontWeight = FontWeight.Bold) }, text = { Text("Mapping local network nodes...", color = Color.Gray) }, confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = CyberTheme.Primary) } }) }

@Composable
fun HistoricalTrafficChart(history: List<HourlyUsage>) {
    GlassCard {
        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Future implementation: Real path rendering
            }
            Text("ENGINE PULSE ANALYTICS", color = Color.Gray.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        }
    }
}
