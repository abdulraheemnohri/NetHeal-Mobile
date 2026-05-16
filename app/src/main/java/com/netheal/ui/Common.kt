package com.netheal.ui

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
    val CardGradient = Brush.verticalGradient(listOf(Color(0xFF161B22), Color(0xFF0D1117)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "NETHEAL",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                modifier = Modifier.shadow(8.dp, CircleShape, spotColor = CyberTheme.Primary)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(CyberTheme.Primary, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "ABSOLUTE CORE ACTIVE",
                    color = CyberTheme.Primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        BadgedBox(badge = { Badge(containerColor = CyberTheme.Danger) { Text("3") } }) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
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
            .border(1.dp, CyberTheme.Border.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp)
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
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDanger) CyberTheme.Danger.copy(alpha = 0.1f) else Color(0xFF161B22)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isDanger) CyberTheme.Danger else CyberTheme.Primary.copy(alpha = 0.5f))
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (isDanger) CyberTheme.Danger else CyberTheme.Primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DiagnosticTool(title: String, desc: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface),
        border = BorderStroke(1.dp, CyberTheme.Border),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberTheme.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = CyberTheme.Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable fun DnsToolDialog(onDismiss: () -> Unit) { var host by remember { mutableStateOf("google.com") }; AlertDialog(onDismissRequest = onDismiss, containerColor = CyberTheme.Surface, title = { Text("DNS Resolver", color = Color.White) }, text = { OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Hostname") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }, confirmButton = { TextButton(onClick = onDismiss) { Text("RESOLVE", color = CyberTheme.Primary) } }) }
@Composable fun PingToolDialog(onDismiss: () -> Unit) { var host by remember { mutableStateOf("1.1.1.1") }; AlertDialog(onDismissRequest = onDismiss, containerColor = CyberTheme.Surface, title = { Text("Ping Utility", color = Color.White) }, text = { OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("IP Address") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }, confirmButton = { TextButton(onClick = onDismiss) { Text("PING", color = CyberTheme.Primary) } }) }
@Composable fun WhoisToolDialog(onDismiss: () -> Unit) { var host by remember { mutableStateOf("github.com") }; AlertDialog(onDismissRequest = onDismiss, containerColor = CyberTheme.Surface, title = { Text("Whois Probe", color = Color.White) }, text = { OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Domain") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }, confirmButton = { TextButton(onClick = onDismiss) { Text("QUERY", color = CyberTheme.Primary) } }) }
@Composable fun LanScannerDialog(onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, containerColor = CyberTheme.Surface, title = { Text("LAN Discovery", color = Color.White) }, text = { Text("Mapping local network nodes...", color = Color.Gray) }, confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = CyberTheme.Primary) } }) }

@Composable
fun HistoricalTrafficChart(history: List<HourlyUsage>) {
    GlassCard {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Simplified graph placeholder
                val points = history.takeLast(10)
                if (points.isNotEmpty()) {
                    // Draw grid lines
                    // Draw path
                }
            }
            Text("Network Pulse Visualizer", color = Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
