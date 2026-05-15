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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.data.HourlyUsage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("NETHEAL", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF00FFA3), CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ABSOLUTE CORE ACTIVE", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        BadgedBox(badge = { Badge(containerColor = Color.Red) { Text("3") } }) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun DiagnosticTool(title: String, desc: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        border = BorderStroke(1.dp, Color(0xFF161B22))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF00FFA3))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable fun DnsToolDialog(onDismiss: () -> Unit) { var host by remember { mutableStateOf("google.com") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("DNS Lookup") }, text = { OutlinedTextField(value = host, onValueChange = { host = it }) }, confirmButton = { TextButton(onClick = onDismiss) { Text("RESOLVE") } }) }
@Composable fun PingToolDialog(onDismiss: () -> Unit) { var host by remember { mutableStateOf("1.1.1.1") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("ICMP Ping") }, text = { OutlinedTextField(value = host, onValueChange = { host = it }) }, confirmButton = { TextButton(onClick = onDismiss) { Text("PING") } }) }
@Composable fun WhoisToolDialog(onDismiss: () -> Unit) { var host by remember { mutableStateOf("github.com") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Whois Lookup") }, text = { OutlinedTextField(value = host, onValueChange = { host = it }) }, confirmButton = { TextButton(onClick = onDismiss) { Text("QUERY") } }) }
@Composable fun LanScannerDialog(onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("LAN Scanner") }, text = { Text("Scanning local network...") }, confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }) }

@Composable
fun HistoricalTrafficChart(history: List<HourlyUsage>) {
    Card(modifier = Modifier.fillMaxWidth().height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Traffic Data Visualizer [SIMULATED]", color = Color.DarkGray, fontSize = 10.sp)
            // In a real app, use a Canvas to draw the history list
        }
    }
}
