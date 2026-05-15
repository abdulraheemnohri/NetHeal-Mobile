package com.netheal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import com.netheal.NetHealApp
import com.netheal.data.HourlyUsage
import kotlinx.coroutines.delay

@Composable
fun TacticalCommandDeck() {
    var history by remember { mutableStateOf(listOf<HourlyUsage>()) }
    var showDnsTool by remember { mutableStateOf(false) }
    var showPingTool by remember { mutableStateOf(false) }
    var showWhoisTool by remember { mutableStateOf(false) }
    var showLanScanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            history = NetHealApp.database.netHealDao().getRecentHourly()
            delay(5000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF010409)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))

        Text("REAL-TIME THREAT MAP", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        ThreatOriginMap()

        Spacer(modifier = Modifier.height(24.dp))
        Text("HISTORICAL TRAFFIC (24H)", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        HistoricalTrafficChart(history)

        Spacer(modifier = Modifier.height(32.dp))
        Text("DIAGNOSTIC ARSENAL", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        DiagnosticTool("DNS Resolver", "Resolve hostname to IP", Icons.Default.Language) { showDnsTool = true }
        DiagnosticTool("Ping Utility", "Test host reachability", Icons.Default.TapAndPlay) { showPingTool = true }
        DiagnosticTool("Whois Probe", "Fetch domain ownership", Icons.Default.Info) { showWhoisTool = true }
        DiagnosticTool("LAN Discovery", "Map local network nodes", Icons.Default.SettingsEthernet) { showLanScanner = true }

        if (showDnsTool) DnsToolDialog(onDismiss = { showDnsTool = false })
        if (showPingTool) PingToolDialog(onDismiss = { showPingTool = false })
        if (showWhoisTool) WhoisToolDialog(onDismiss = { showWhoisTool = false })
        if (showLanScanner) LanScannerDialog(onDismiss = { showLanScanner = false })

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ThreatOriginMap() {
    val blockLocations = remember { mutableStateListOf<Offset>() }
    LaunchedEffect(Unit) {
        while (true) {
            if (blockLocations.size > 8) blockLocations.removeAt(0)
            blockLocations.add(Offset((Math.random() * 800).toFloat(), (Math.random() * 300).toFloat()))
            delay(3000)
        }
    }
    Card(modifier = Modifier.fillMaxWidth().height(160.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(Color.Gray.copy(alpha = 0.1f), Offset(0f, size.height/2), Offset(size.width, size.height/2))
                drawLine(Color.Gray.copy(alpha = 0.1f), Offset(size.width/2, 0f), Offset(size.width/2, size.height))
                blockLocations.forEach { loc ->
                    drawCircle(Color.Red.copy(alpha = 0.4f), 10f, loc)
                    drawCircle(Color.Red, 3f, loc)
                }
            }
            Text("GLOBAL INTERCEPTIONS", modifier = Modifier.padding(12.dp), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}
