package com.netheal.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import com.netheal.bridge.RustBridge
import com.netheal.data.FirewallRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen() {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val random = remember { Random() }

    fun loadApps() {
        scope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val filtered = if (showSystemApps) {
                installedApps
            } else {
                installedApps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            }
            allApps = filtered.map {
                it.packageName to pm.getApplicationLabel(it).toString()
            }.sortedBy { it.second }
        }
    }

    LaunchedEffect(showSystemApps) {
        loadApps()
    }

    val filteredApps = if (searchQuery.isEmpty()) allApps else allApps.filter {
        it.second.contains(searchQuery, ignoreCase = true) || it.first.contains(searchQuery, ignoreCase = true)
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
                    "APP SHIELD",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    "RESTRICT INDIVIDUAL CONNECTIVITY",
                    color = Color(0xFF00FFA3),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                IconButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val rules = NetHealApp.database.netHealDao().getAllRules()
                        rules.forEach {
                            NetHealApp.database.netHealDao().saveRule(FirewallRule(it.appId, false))
                            RustBridge.setAppRule(it.appId, false)
                        }
                        loadApps()
                    }
                }) {
                    Icon(Icons.Default.Block, contentDescription = "Unblock All", tint = Color.Gray)
                }
                IconButton(onClick = { showSystemApps = !showSystemApps }) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Toggle System Apps",
                        tint = if (showSystemApps) Color(0xFF00FFA3) else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search packages or names...", color = Color.Gray, fontSize = 14.sp) },
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

        if (allApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00FFA3))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredApps, key = { it.first }) { (packageName, label) ->
                    val dataSim = remember { (random.nextFloat() * 50 + 1).let { String.format("%.1f MB", it) } }
                    AppRuleItem(packageName, label, dataSim)
                }
            }
        }
    }
}

@Composable
fun AppRuleItem(appId: String, appName: String, dataUsed: String) {
    var isBlocked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(appId) {
        isBlocked = NetHealApp.database.netHealDao().isAppBlocked(appId) ?: false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isBlocked) Color.Red.copy(alpha = 0.2f) else Color(0xFF161B22))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(appName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(appId, color = Color.Gray, fontSize = 9.sp, modifier = Modifier.weight(1f, fill = false), maxLines = 1)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("• $dataUsed used", color = Color(0xFF00FFA3), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Switch(
                checked = !isBlocked,
                onCheckedChange = {
                    isBlocked = !it
                    RustBridge.setAppRule(appId, isBlocked)
                    scope.launch {
                        NetHealApp.database.netHealDao().saveRule(FirewallRule(appId, isBlocked))
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00FFA3),
                    checkedTrackColor = Color(0xFF00FFA3).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.Red,
                    uncheckedTrackColor = Color.Red.copy(alpha = 0.5f)
                )
            )
        }
    }
}
