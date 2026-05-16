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
import androidx.compose.ui.graphics.TileMode
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
    val HoloV4 = listOf(Color(0xFF00FFA3), Color(0xFF00E5FF), Color(0xFF7000FF), Color(0xFF00FFA3))
}

@Composable
fun CyberBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 120f,
        animationSpec = infiniteRepeatable(animation = tween(5000, easing = LinearEasing)),
        label = "offset"
    )
    Box(modifier = Modifier.fillMaxSize().background(CyberTheme.Background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStep = 80f
            for (x in 0..(size.width / gridStep).toInt()) {
                val posX = x * gridStep + (offset % gridStep)
                drawLine(CyberTheme.Border.copy(alpha = 0.05f), Offset(posX, 0f), Offset(posX, size.height))
            }
            for (y in 0..(size.height / gridStep).toInt()) {
                val posY = y * gridStep + (offset % gridStep)
                drawLine(CyberTheme.Border.copy(alpha = 0.05f), Offset(0f, posY), Offset(size.width, posY))
            }
        }
        // V4 Prism Refraction
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(
            colors = listOf(CyberTheme.AccentV3.copy(alpha = 0.02f), Color.Transparent, CyberTheme.Primary.copy(alpha = 0.02f)),
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f),
            tileMode = TileMode.Mirror
        )))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header() {
    val infiniteTransition = rememberInfiniteTransition(label = "holo")
    val holoOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing)),
        label = "holo_offset"
    )

    val holoBrush = Brush.linearGradient(
        colors = CyberTheme.HoloV4,
        start = Offset(holoOffset, holoOffset),
        end = Offset(holoOffset + 500f, holoOffset + 500f),
        tileMode = TileMode.Mirror
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "NETHEAL OMEGA v4",
                style = androidx.compose.ui.text.TextStyle(
                    brush = holoBrush,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.shadow(8.dp, CircleShape, spotColor = CyberTheme.Secondary)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CyberTheme.Primary))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "ULTIMATE SECURITY ACTIVE",
                    color = CyberTheme.Primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.WifiTethering, null, tint = CyberTheme.Primary.copy(alpha = 0.8f))
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
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) { content() }
    }
}

@Composable
fun HoloCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "holo_card")
    val holoOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 400f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing)),
        label = "holo_card_offset"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = CyberTheme.HoloV4,
                    start = Offset(holoOffset, 0f),
                    end = Offset(holoOffset + 200f, 200f),
                    tileMode = TileMode.Mirror
                ),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
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
        modifier = modifier.height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDanger) CyberTheme.Danger.copy(alpha = 0.15f) else Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDanger) CyberTheme.Danger else CyberTheme.Primary.copy(alpha = 0.7f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                if (!isDanger) Brush.horizontalGradient(listOf(CyberTheme.Primary.copy(alpha = 0.1f), Color.Transparent)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = if (isDanger) CyberTheme.Danger else CyberTheme.Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun DiagnosticTool(title: String, desc: String, icon: ImageVector, onClick: () -> Unit) {
    HoloCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberTheme.Secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = CyberTheme.Secondary, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
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
    HoloCard {
        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Future implementation: Real path rendering
            }
            Text("PULSE TELEMETRY ANALYTICS", color = Color.Gray.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        }
    }
}
