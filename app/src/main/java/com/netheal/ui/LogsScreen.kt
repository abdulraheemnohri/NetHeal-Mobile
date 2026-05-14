package com.netheal.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(listOf<ThreatLog>()) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            logs = NetHealApp.database.netHealDao().getAllLogs()
            delay(5000)
        }
    }

    val filteredLogs = if (searchQuery.isEmpty()) logs else logs.filter {
        it.domain.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "THREAT ARCHIVE",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    "HISTORICAL CONNECTION BLOCKS",
                    color = Color(0xFF00FFA3),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                IconButton(onClick = {
                    val report = logs.take(20).joinToString("\n") {
                        "${it.domain} | Risk: ${it.riskScore} | ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))}"
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "NetHeal Security Report:\n$report")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Threat Report"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
                }
                IconButton(onClick = {
                    scope.launch {
                        NetHealApp.database.netHealDao().deleteAllLogs()
                        logs = emptyList()
                    }
                }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Filter by domain...", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00FFA3),
                unfocusedBorderColor = Color(0xFF161B22),
                focusedContainerColor = Color(0xFF161B22),
                unfocusedContainerColor = Color(0xFF161B22)
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Summary
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LogStatCard(modifier = Modifier.weight(1f), label = "CRITICAL", count = filteredLogs.count { it.riskScore > 80 }.toString(), color = Color.Red)
            LogStatCard(modifier = Modifier.weight(1f), label = "SUSPICIOUS", count = filteredLogs.count { it.riskScore in 50..80 }.toString(), color = Color.Yellow)
            LogStatCard(modifier = Modifier.weight(1f), label = "SCANNED", count = RustBridge.getScannedCount().toString(), color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isEmpty()) "Database is clean." else "No matching logs found.", color = Color.DarkGray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredLogs, key = { it.id }) { log ->
                    LogCard(log)
                }
            }
        }
    }
}

@Composable
fun LogStatCard(modifier: Modifier, label: String, count: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(count, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun LogCard(log: ThreatLog) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    val time = sdf.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 40.dp)
                    .background(
                        if (log.riskScore > 80) Color.Red else if (log.riskScore > 50) Color.Yellow else Color.Cyan,
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.domain, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(time, color = Color.Gray, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("RISK", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("${log.riskScore}", color = if (log.riskScore > 80) Color.Red else Color.Yellow, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
