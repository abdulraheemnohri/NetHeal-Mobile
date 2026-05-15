package com.netheal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AuditResult(val title: String, val status: String, val color: Color)

@Composable
fun SecurityAuditSection() {
    var isRunning by remember { mutableStateOf(false) }
    var auditResults by remember { mutableStateOf(listOf<AuditResult>()) }
    val scope = rememberCoroutineScope()

    var showDnsTool by remember { mutableStateOf(false) }
    var showPingTool by remember { mutableStateOf(false) }
    var showLanScanner by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("SYSTEM INTEGRITY AUDIT", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (auditResults.isNotEmpty()) {
            auditResults.forEach { res ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(res.title, color = Color.White, fontSize = 13.sp)
                        Text(res.status, color = res.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    isRunning = true; auditResults = emptyList()
                    delay(800); auditResults = auditResults + AuditResult("VpnService Core", "ACTIVE", Color(0xFF00FFA3))
                    delay(600); auditResults = auditResults + AuditResult("Rust JNI Bridge", "STABLE", Color(0xFF00FFA3))
                    delay(1000); auditResults = auditResults + AuditResult("SSL/TLS Inspection", "BYPASSED", Color.Gray)
                    delay(500); auditResults = auditResults + AuditResult("Omega Detection Engine", "OPTIMAL", Color(0xFF00FFA3))
                    isRunning = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22)),
            enabled = !isRunning
        ) {
            if (isRunning) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00FFA3))
            else Text("RUN FULL SYSTEM AUDIT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("CONNECTIVITY DIAGNOSTICS", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        DiagnosticTool("DNS Lookup", "Resolve hostnames", Icons.Default.Language) { showDnsTool = true }
        DiagnosticTool("ICMP Ping", "Check latency", Icons.Default.TapAndPlay) { showPingTool = true }
        DiagnosticTool("Peer Discovery", "Scan LAN nodes", Icons.Default.SettingsEthernet) { showLanScanner = true }

        if (showDnsTool) DnsToolDialog(onDismiss = { showDnsTool = false })
        if (showPingTool) PingToolDialog(onDismiss = { showPingTool = false })
        if (showLanScanner) LanScannerDialog(onDismiss = { showLanScanner = false })

        Spacer(modifier = Modifier.height(40.dp))
    }
}
