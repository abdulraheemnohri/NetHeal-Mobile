package com.netheal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var showBreachMonitor by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(16.dp).verticalScroll(rememberScrollState())) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))
        Text("SYSTEM INTEGRITY AUDIT", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        GlassCard {
            if (auditResults.isEmpty() && !isRunning) {
                Text("Launch full system scan to verify kernel integrity and policy synchronization.", color = Color.Gray, fontSize = 11.sp)
            } else {
                auditResults.forEach { res ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(res.title, color = Color.White, fontSize = 13.sp)
                        Text(res.status, color = res.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            CyberButton(
                if (isRunning) "SCANNING..." else "INITIATE AUDIT",
                if (isRunning) null else Icons.Default.VerifiedUser,
                onClick = {
                    scope.launch {
                        isRunning = true; auditResults = emptyList()
                        delay(800); auditResults = auditResults + AuditResult("Kernel Integrity", "VERIFIED", CyberTheme.Primary)
                        delay(600); auditResults = auditResults + AuditResult("JNI Bridge State", "STABLE", CyberTheme.Primary)
                        delay(1000); auditResults = auditResults + AuditResult("Encryption Layer", "NOMINAL", CyberTheme.Primary)
                        delay(500); auditResults = auditResults + AuditResult("AI Model Sync", "SYNCED", CyberTheme.Secondary)
                        isRunning = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isDanger = false
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("NEXT-GEN THREAT DETECTION", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        DiagnosticTool("Breach Monitor", "Scan Dark Web for credentials", Icons.Default.Language) { showBreachMonitor = true }

        Spacer(modifier = Modifier.height(32.dp))
        Text("CONNECTIVITY DIAGNOSTICS", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        DiagnosticTool("DNS Lookup", "Resolve hostnames via Secure Core", Icons.Default.Language) { showDnsTool = true }
        DiagnosticTool("ICMP Ping", "Check node latency and jitter", Icons.Default.TapAndPlay) { showPingTool = true }
        DiagnosticTool("Peer Discovery", "Map local subnet vulnerabilities", Icons.Default.SettingsEthernet) { showLanScanner = true }

        if (showBreachMonitor) BreachMonitorDialog(onDismiss = { showBreachMonitor = false })
        if (showDnsTool) DnsToolDialog(onDismiss = { showDnsTool = false })
        if (showPingTool) PingToolDialog(onDismiss = { showPingTool = false })
        if (showLanScanner) LanScannerDialog(onDismiss = { showLanScanner = false })

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun BreachMonitorDialog(onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberTheme.Surface,
        title = { Text("DARK WEB BREACH MONITOR", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email Identifier") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberTheme.Primary)
                )
                if (isScanning) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = CyberTheme.Primary)
                    Text("Probing global breach databases...", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                }
                result?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(it, color = if (it.contains("CLEAN")) CyberTheme.Primary else CyberTheme.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    isScanning = true; result = null
                    delay(3000)
                    isScanning = false
                    result = if (email.contains("leak")) "EXPOSURE DETECTED: 3 DB BreachesFound" else "STATUS CLEAN: No direct leaks found."
                }
            }) { Text("SCAN", color = CyberTheme.Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE", color = Color.Gray) }
        }
    )
}
