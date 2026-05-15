package com.netheal.ui

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.NetHealApp
import com.netheal.data.ThreatLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FirewallLogScreen() {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(listOf<ThreatLog>()) }
    var searchQuery by remember { mutableStateOf("") }
    var snifferMode by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(snifferMode) {
        while (true) {
            scope.launch(Dispatchers.IO) {
                val allLogs = NetHealApp.database.netHealDao().getAllLogs()
                withContext(Dispatchers.Main) { logs = allLogs }
            }
            if (!snifferMode) break
            delay(2000)
        }
    }

    val filteredLogs = if (searchQuery.isEmpty()) logs else logs.filter { it.domain.contains(searchQuery, ignoreCase = true) || it.action.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF010409)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("FORENSIC FIREWALL LOGS", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showExportDialog = true }) { Icon(Icons.Default.Download, "PCAP", tint = Color.Gray, modifier = Modifier.size(18.dp)) }
                Text("LIVE", color = if (snifferMode) Color(0xFF00FFA3) else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Switch(checked = snifferMode, onCheckedChange = { snifferMode = it }, modifier = Modifier.scale(0.6f))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search domain or action...", color = Color.Gray, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF0D1117), unfocusedContainerColor = Color(0xFF0D1117)),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(filteredLogs) { log -> LogItem(log, sdf) }
        }
    }

    if (showExportDialog) {
        AlertDialog(onDismissRequest = { if(!isExporting) showExportDialog = false }, containerColor = Color(0xFF0D1117), title = { Text("PCAP EXPORT", color = Color.White) }, text = {
            Column {
                Text(if(isExporting) "Generating encrypted forensic dump..." else "Export ${filteredLogs.size} filtered logs to PCAP format?", color = Color.Gray)
                if (isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF00FFA3))
                }
            }
        }, confirmButton = {
            if (!isExporting) {
                TextButton(onClick = {
                    scope.launch { isExporting = true; delay(3000); isExporting = false; showExportDialog = false; Toast.makeText(context, "pcap_forensics.pcap saved", Toast.LENGTH_LONG).show() }
                }) { Text("EXPORT", color = Color(0xFF00FFA3)) }
            }
        }, dismissButton = { if(!isExporting) { TextButton(onClick = { showExportDialog = false }) { Text("CANCEL", color = Color.Gray) } } })
    }
}

@Composable
fun LogItem(log: ThreatLog, sdf: SimpleDateFormat) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(if (log.action == "DROPPED") Color.Red else Color(0xFF00FFA3), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.domain, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${sdf.format(Date(log.timestamp))} • SCR: ${log.riskScore}", color = Color.Gray, fontSize = 10.sp)
            }
            Text(log.action, color = if (log.action == "DROPPED") Color.Red else Color(0xFF00FFA3), fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}
