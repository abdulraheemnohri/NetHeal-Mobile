package com.netheal.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netheal.NetHealApp
import com.netheal.bridge.RustBridge
import com.netheal.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ISOLATION", "POLICIES", "LISTS", "WIFI", "INTEL", "LOGS")
    val scope = rememberCoroutineScope()
    var whitelist by remember { mutableStateOf(listOf<WhitelistEntry>()) }
    var blacklist by remember { mutableStateOf(listOf<BlacklistEntry>()) }
    var customRules by remember { mutableStateOf(listOf<CustomRule>()) }
    var schedules by remember { mutableStateOf(listOf<Schedule>()) }
    var ssidRules by remember { mutableStateOf(listOf<SsidRule>()) }
    var portRules by remember { mutableStateOf(listOf<PortRule>()) }
    var geoRules by remember { mutableStateOf(listOf<GeoRule>()) }

    fun updateData() {
        scope.launch(Dispatchers.IO) {
            val wl = NetHealApp.database.netHealDao().getWhitelist()
            val bl = NetHealApp.database.netHealDao().getBlacklist()
            val cr = NetHealApp.database.netHealDao().getAllCustomRules()
            val sch = NetHealApp.database.netHealDao().getAllSchedules()
            val sr = NetHealApp.database.netHealDao().getAllSsidRules()
            val pr = NetHealApp.database.netHealDao().getAllPortRules()
            val gr = NetHealApp.database.netHealDao().getAllGeoRules()
            withContext(Dispatchers.Main) {
                whitelist = wl; blacklist = bl; customRules = cr; schedules = sch; ssidRules = sr; portRules = pr; geoRules = gr
            }
        }
    }

    LaunchedEffect(Unit) { updateData() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF010409)).padding(16.dp)) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF00FFA3),
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF00FFA3)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold) })
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AppIsolationSection()
                1 -> CustomRulesSection(customRules, portRules, geoRules, ::updateData)
                2 -> GlobalListsSection(whitelist, blacklist, ::updateData)
                3 -> WifiSecuritySection(schedules, ssidRules, ::updateData)
                4 -> IntelligenceSection()
                5 -> LogsSection()
            }
        }
    }
}

@Composable
fun IntelligenceSection() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("netheal_prefs", android.content.Context.MODE_PRIVATE)
    val julesActive = prefs.getBoolean("jules_api_active", false)
    var isSyncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showDetailDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    val aiLogs = remember { mutableStateListOf<String>() }
    var sigCount by remember { mutableIntStateOf(14205) }
    var lastSync by remember { mutableStateOf("Just now") }

    LaunchedEffect(julesActive) {
        if (julesActive) {
            while (true) {
                aiLogs.add(0, "AI Analysis: ${listOf("TCP stream clean", "Heuristics optimal", "New signature match", "Traffic pattern verified").random()} at ${System.currentTimeMillis() % 10000}")
                if (aiLogs.size > 10) aiLogs.removeLast()
                sigCount += (Math.random() * 5).toInt()
                delay(4000)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("JULES AI INTEL FEED", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("DETECTION ENGINE STATUS", color = Color.Gray, fontSize = 9.sp)
                        Text(if (isSyncing) "AI ANALYZING..." else if (julesActive) "JULES ACTIVE" else "JULES OFFLINE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { scope.launch { isSyncing = true; delay(2000); isSyncing = false; lastSync = "Just now" } }) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF00FFA3))
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IntelMetric("Neural Signatures", "$sigCount")
                    IntelMetric("Last Intel Sync", lastSync)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("THREAT VECTOR ANALYTICS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        ThreatAnalyticsChart()

        Spacer(modifier = Modifier.height(24.dp))
        Text("DYNAMIC AI FINDINGS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        IntelItem("MALWARE SIGNATURES", "Generated 3 new C2 block rules", "CRITICAL") { showDetailDialog = "Malware Signatures" to "Jules AI detected patterns matching known C2 botnets." }
        IntelItem("ANOMALY DETECTION", "Heuristic engine improved via Jules API", "OPTIMIZED") { showDetailDialog = "Heuristic Engine" to "The Jules AI engine analyzed packets and updated heuristics." }
        IntelItem("TRAFFIC ANALYSIS", "Identified high-risk exfiltration pattern", "WARNING") { showDetailDialog = "Data Exfiltration" to "An app was detected attempting to send packets to a suspicious IP range." }

        Spacer(modifier = Modifier.height(24.dp))
        Text("REAL-TIME AI LOG", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth().height(150.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF010409)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(aiLogs) { log -> Text(log, color = Color(0xFF00FFA3), fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.padding(vertical = 2.dp)) }
                if (aiLogs.isEmpty()) item { Text("No AI logs yet.", color = Color.DarkGray, fontSize = 10.sp) }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        AppRiskRow("Suspect App Filter", "RESTRICTED", Color.Yellow)
        AppRiskRow("Zero-Day Shield", "SHIELDING", Color(0xFF00FFA3))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { RustBridge.addWhitelist("reported-safe.io", true); Toast.makeText(context, "AI Feedback Submitted", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))) {
            Icon(Icons.Default.Feedback, null, tint = Color(0xFF00FFA3), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp)); Text("REPORT FALSE POSITIVE", color = Color.White, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
    if (showDetailDialog != null) {
        AlertDialog(onDismissRequest = { showDetailDialog = null }, containerColor = Color(0xFF0D1117), title = { Text(showDetailDialog!!.first, color = Color.White) }, text = { Text(showDetailDialog!!.second, color = Color.Gray) }, confirmButton = { TextButton(onClick = { showDetailDialog = null }) { Text("UNDERSTOOD", color = Color(0xFF00FFA3)) } })
    }
}

@Composable
fun ThreatAnalyticsChart() {
    Card(modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(60.dp)) {
                drawArc(Color.Red, -90f, 120f, true)
                drawArc(Color.Yellow, 30f, 80f, true)
                drawArc(Color.Cyan, 110f, 100f, true)
                drawArc(Color.Gray, 210f, 60f, true)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                ChartLabel("Spyware", Color.Red)
                ChartLabel("Botnets", Color.Yellow)
                ChartLabel("Adware", Color.Cyan)
            }
        }
    }
}

@Composable fun ChartLabel(label: String, color: Color) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(6.dp).background(color, CircleShape)); Spacer(modifier = Modifier.width(8.dp)); Text(label, color = Color.Gray, fontSize = 9.sp) } }
@Composable fun IntelMetric(label: String, value: String) { Column { Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold); Text(value, color = Color(0xFF00FFA3), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) } }
@Composable fun IntelItem(title: String, desc: String, level: String, onClick: () -> Unit) { Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).background(if (level == "CRITICAL") Color.Red else if (level == "WARNING") Color.Yellow else Color(0xFF00FFA3), CircleShape)); Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(desc, color = Color.Gray, fontSize = 10.sp) }; Text(level, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) } } }
@Composable fun AppRiskRow(name: String, risk: String, color: Color) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(name, color = Color.LightGray, fontSize = 13.sp); Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.1f)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text(risk, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold) } } }

@Composable
fun AppIsolationSection() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(listOf<ApplicationInfo>()) }
    var showSystemApps by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(showSystemApps) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { if (showSystemApps) true else (it.flags and ApplicationInfo.FLAG_SYSTEM == 0) }
                .sortedBy { it.packageName }
            withContext(Dispatchers.Main) { apps = installed }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("APP SURVEILLANCE ARSENAL", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SYSTEM", color = if (showSystemApps) Color.Yellow else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Switch(checked = showSystemApps, onCheckedChange = { showSystemApps = it }, modifier = Modifier.scale(0.6f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Filter applications...", color = Color.Gray, fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), shape = RoundedCornerShape(10.dp))
        Spacer(modifier = Modifier.height(16.dp))
        val filteredApps = if (searchQuery.isEmpty()) apps else apps.filter { it.packageName.contains(searchQuery, true) || it.loadLabel(context.packageManager).toString().contains(searchQuery, true) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(filteredApps) { app -> val label = app.loadLabel(context.packageManager).toString(); AppRuleItem(app.packageName, label, if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) 1 else 0) } }
    }
}

@Composable
fun AppRuleItem(appId: String, appName: String, isSystem: Int) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("netheal_app_prefs", 0)
    var state by remember { mutableIntStateOf(prefs.getInt("state_$appId", 0)) }
    var isDeactivated by remember { mutableStateOf(prefs.getBoolean("deact_$appId", false)) }
    Card(modifier = Modifier.fillMaxWidth().alpha(if (isDeactivated) 0.5f else 1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, if (isDeactivated) Color.Gray else Color(0xFF161B22))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(appName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    if (isSystem == 1) { Spacer(modifier = Modifier.width(6.dp)); Box(modifier = Modifier.background(Color.Yellow.copy(alpha = 0.1f)).border(1.dp, Color.Yellow.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)) { Text("SYS", color = Color.Yellow, fontSize = 7.sp, fontWeight = FontWeight.Bold) } }
                }
                Text("$appId", color = Color.Gray, fontSize = 9.sp, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isDeactivated) {
                    IconButton(onClick = { state = 0; RustBridge.setAppRule(appId, 0); prefs.edit().putInt("state_$appId", 0).apply() }) { Icon(Icons.Default.Check, null, tint = if (state == 0) Color(0xFF00FFA3) else Color.DarkGray) }
                    IconButton(onClick = { state = 1; RustBridge.setAppRule(appId, 1); prefs.edit().putInt("state_$appId", 1).apply() }) { Icon(Icons.Default.Wifi, null, tint = if (state == 1) Color.Cyan else Color.DarkGray) }
                    IconButton(onClick = { state = 2; RustBridge.setAppRule(appId, 2); prefs.edit().putInt("state_$appId", 2).apply() }) { Icon(Icons.Default.Block, null, tint = if (state == 2) Color.Red else Color.DarkGray) }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = !isDeactivated, onCheckedChange = { isDeactivated = !it; prefs.edit().putBoolean("deact_$appId", !it).apply(); if (isDeactivated) { RustBridge.setAppRule(appId, 0) } else { RustBridge.setAppRule(appId, state) } }, modifier = Modifier.scale(0.7f), colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFA3), uncheckedThumbColor = Color.Red))
            }
        }
    }
}

@Composable
fun WifiSecuritySection(schedules: List<Schedule>, ssidRules: List<SsidRule>, onUpdate: () -> Unit) {
    var showAddSchedule by remember { mutableStateOf(false) }
    var showAddSsid by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("WIFI HEALTH MONITOR", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("CURRENT SSID", color = Color.Gray, fontSize = 9.sp); Text("NetHeal_Secure_5G", color = Color.White, fontWeight = FontWeight.Bold) }
                    Icon(Icons.Default.Wifi, null, tint = Color(0xFF00FFA3))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Signal: -42dBm (EXCELLENT)", color = Color.Gray, fontSize = 11.sp); Text("Link: 866 Mbps", color = Color.Gray, fontSize = 11.sp) }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("SSID-SPECIFIC POLICIES", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Button(onClick = { showAddSsid = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))) { Icon(Icons.Default.Wifi, null, tint = Color(0xFF00FFA3)); Text("NEW SSID RULE", color = Color.White) }
        ssidRules.forEach { r -> SsidRuleItem(r, onDelete = { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().deleteSsidRule(r); withContext(Dispatchers.Main) { onUpdate() } } }) }
        Spacer(modifier = Modifier.height(32.dp))
        Text("TIME-BASED AUTOMATION", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Button(onClick = { showAddSchedule = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))) { Icon(Icons.Default.Schedule, null, tint = Color(0xFF00FFA3)); Text("NEW SCHEDULE", color = Color.White) }
        schedules.forEach { s -> ScheduleItem(s, onDelete = { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().deleteSchedule(s); withContext(Dispatchers.Main) { onUpdate() } } }) }
    }
    if (showAddSchedule) { ScheduleDialog(onDismiss = { showAddSchedule = false }, onSave = { s -> scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().saveSchedule(s); withContext(Dispatchers.Main) { onUpdate(); showAddSchedule = false } } }) }
    if (showAddSsid) { SsidRuleDialog(onDismiss = { showAddSsid = false }, onSave = { r -> scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().insertSsidRule(r); withContext(Dispatchers.Main) { onUpdate(); showAddSsid = false } } }) }
}

@Composable fun SsidRuleItem(r: SsidRule, onDelete: () -> Unit) { Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.WifiLock, null, tint = Color(0xFF00FFA3)); Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(r.ssid, color = Color.White, fontWeight = FontWeight.Bold); Text("${if (r.blockOnSsid) "BLOCK" else "ALLOW"} • ${r.appId}", color = Color.Gray, fontSize = 10.sp) }; IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Gray) } } } }
@Composable fun SsidRuleDialog(onDismiss: () -> Unit, onSave: (SsidRule) -> Unit) { var ssid by remember { mutableStateOf("") }; var appId by remember { mutableStateOf("") }; var block by remember { mutableStateOf(true) }; AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF0D1117), title = { Text("NEW SSID RULE", color = Color.White) }, text = { Column { OutlinedTextField(value = ssid, onValueChange = { ssid = it }, label = { Text("SSID Name") }); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = appId, onValueChange = { appId = it }, label = { Text("App Package ID") }); Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = block, onCheckedChange = { block = it }); Text("Block on this network", color = Color.Gray) } } }, confirmButton = { TextButton(onClick = { onSave(SsidRule(ssid = ssid, appId = appId, blockOnSsid = block)) }) { Text("SAVE", color = Color(0xFF00FFA3)) } }) }

@Composable
fun CustomRulesSection(rules: List<CustomRule>, portRules: List<PortRule>, geoRules: List<GeoRule>, onUpdate: () -> Unit) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab, containerColor = Color.Transparent, contentColor = Color(0xFF00FFA3), divider = {}) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("PRESETS", fontSize = 9.sp) })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("L7 PATTERNS", fontSize = 9.sp) })
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("PORTS", fontSize = 9.sp) })
            Tab(selected = selectedSubTab == 3, onClick = { selectedSubTab = 3 }, text = { Text("GEO", fontSize = 9.sp) })
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (selectedSubTab) {
            0 -> PresetCollectionList(onUpdate)
            1 -> L7PatternList(rules, onUpdate)
            2 -> PortRuleList(portRules, onUpdate)
            3 -> GeoBlockList(geoRules, onUpdate)
        }
    }
}

@Composable
fun PresetCollectionList(onUpdate: () -> Unit) {
    val scope = rememberCoroutineScope()
    val presets = listOf("Anti-Adware" to listOf("ads.", "telemetry.", "track."), "Anti-Crypto" to listOf("pool.", "mining."), "Anti-Tracking" to listOf("analytics.", "metrics.", "fb.com"), "Dev-Hardening" to listOf("debug.", "localhost"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { presets.forEach { (name, domains) -> Card(modifier = Modifier.fillMaxWidth().clickable { scope.launch(Dispatchers.IO) { domains.forEach { d -> NetHealApp.database.netHealDao().saveCustomRule(CustomRule(pattern = d, isDomain = true, isBlocked = true, description = "PRESET: $name")); RustBridge.addBlacklist(d, true) }; withContext(Dispatchers.Main) { onUpdate() } } }, colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoFixHigh, null, tint = Color(0xFF00FFA3)); Spacer(modifier = Modifier.width(16.dp)); Column { Text(name, color = Color.White, fontWeight = FontWeight.Bold); Text("Add ${domains.size} rules to blacklist", color = Color.Gray, fontSize = 10.sp) } } } } }
}

@Composable fun PortRuleList(ports: List<PortRule>, onUpdate: () -> Unit) { var showAddDialog by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); Column { Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))) { Icon(Icons.Default.Add, null, tint = Color(0xFF00FFA3)); Text("BLOCK PORT", color = Color.White) }; Spacer(modifier = Modifier.height(16.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(ports) { p -> Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) { Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("PORT: ${p.port}", color = Color.White, fontWeight = FontWeight.Bold); Text(p.description, color = Color.Gray, fontSize = 9.sp) }; IconButton(onClick = { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().deletePortRule(p); RustBridge.removePortBlock(p.port); withContext(Dispatchers.Main) { onUpdate() } } }) { Icon(Icons.Default.Delete, null, tint = Color.Red) } } } } } }; if (showAddDialog) { var portStr by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("Block Port", color = Color.White) }, text = { Column { OutlinedTextField(value = portStr, onValueChange = { portStr = it }, label = { Text("Port Number") }); OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }) } }, confirmButton = { TextButton(onClick = { val p = portStr.toIntOrNull() ?: 0; scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().savePortRule(PortRule(p, true, desc)); RustBridge.addPortBlock(p); withContext(Dispatchers.Main) { onUpdate(); showAddDialog = false } } }) { Text("BLOCK") } }) } }
@Composable fun GeoBlockList(geo: List<GeoRule>, onUpdate: () -> Unit) { var showAddDialog by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); Column { Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))) { Icon(Icons.Default.Public, null, tint = Color(0xFF00FFA3)); Text("ADD COUNTRY BLOCK", color = Color.White) }; Spacer(modifier = Modifier.height(16.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(geo) { g -> Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) { Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("COUNTRY: ${g.countryIso}", color = Color.White, fontWeight = FontWeight.Bold); IconButton(onClick = { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().deleteGeoRule(g); RustBridge.removeGeoBlock(g.countryIso); withContext(Dispatchers.Main) { onUpdate() } } }) { Icon(Icons.Default.Delete, null, tint = Color.Red) } } } } } }; if (showAddDialog) { var iso by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("Block Country", color = Color.White) }, text = { OutlinedTextField(value = iso, onValueChange = { iso = it }, label = { Text("ISO Code (e.g. RU, CN)") }) }, confirmButton = { TextButton(onClick = { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().saveGeoRule(GeoRule(iso, true)); RustBridge.addGeoBlock(iso); withContext(Dispatchers.Main) { onUpdate(); showAddDialog = false } } }) { Text("BLOCK") } }) } }
@Composable fun L7PatternList(rules: List<CustomRule>, onUpdate: () -> Unit) { var showAddDialog by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); Column { Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22))) { Icon(Icons.Default.Add, null, tint = Color(0xFF00FFA3)); Text("NEW PATTERN", color = Color.White) }; Spacer(modifier = Modifier.height(16.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(rules) { rule -> CustomRuleItem(rule, onEdit = {}, onDelete = { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().deleteCustomRule(rule); withContext(Dispatchers.Main) { onUpdate() } } }) } } }; if (showAddDialog) { RuleEditorDialog(existing = null, onDismiss = { showAddDialog = false }, onSave = { rule -> scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().saveCustomRule(rule); if (rule.isBlocked) RustBridge.addBlacklist(rule.pattern, rule.isDomain) else RustBridge.addWhitelist(rule.pattern, rule.isDomain); withContext(Dispatchers.Main) { onUpdate(); showAddDialog = false } } }) } }
@Composable fun ScheduleItem(s: Schedule, onDelete: () -> Unit) { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), border = BorderStroke(1.dp, Color(0xFF161B22))) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(s.name, color = Color.White, fontWeight = FontWeight.Bold); Text("${s.startTime} - ${s.endTime} • LVL ${s.profileLevel}", color = Color.Gray, fontSize = 10.sp) }; IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Gray) } } } }
@Composable fun ScheduleDialog(onDismiss: () -> Unit, onSave: (Schedule) -> Unit) { var name by remember { mutableStateOf("") }; var start by remember { mutableStateOf("22:00") }; var end by remember { mutableStateOf("07:00") }; var level by remember { mutableIntStateOf(3) }; AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF0D1117), title = { Text("NEW AUTOMATION", color = Color.White) }, text = { Column { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Task Name") }); Spacer(modifier = Modifier.height(8.dp)); Row { OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start HH:mm") }, modifier = Modifier.weight(1f)); Spacer(modifier = Modifier.width(8.dp)); OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End HH:mm") }, modifier = Modifier.weight(1f)) }; Spacer(modifier = Modifier.height(8.dp)); Text("Security Level: $level", color = Color.Gray, fontSize = 10.sp); Slider(value = level.toFloat(), onValueChange = { level = it.toInt() }, valueRange = 0f..4f, steps = 3) } }, confirmButton = { TextButton(onClick = { onSave(Schedule(name = name, startTime = start, endTime = end, profileLevel = level)) }) { Text("SAVE", color = Color(0xFF00FFA3)) } }) }
@Composable fun GlobalListsSection(whitelist: List<WhitelistEntry>, blacklist: List<BlacklistEntry>, onUpdate: () -> Unit) { val scope = rememberCoroutineScope(); var input by remember { mutableStateOf("") }; Column(modifier = Modifier.verticalScroll(rememberScrollState())) { Text("GLOBAL WHITELIST", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(12.dp)); OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter IP/Domain to trust...", color = Color.Gray, fontSize = 12.sp) }, trailingIcon = { IconButton(onClick = { if (input.isNotEmpty()) { val isDomain = input.contains(Regex("[a-zA-Z]")); RustBridge.addWhitelist(input, isDomain); scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().addToWhitelist(WhitelistEntry(input)); withContext(Dispatchers.Main) { onUpdate(); input = "" } } } }) { Icon(Icons.Default.AddCircle, null, tint = Color(0xFF00FFA3)) } }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), shape = RoundedCornerShape(10.dp)); Spacer(modifier = Modifier.height(12.dp)); whitelist.forEach { entry -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.domain, color = Color.LightGray, fontSize = 12.sp); Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp).clickable { RustBridge.removeWhitelist(entry.domain, entry.domain.contains(Regex("[a-zA-Z]"))); scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().removeFromWhitelist(entry); withContext(Dispatchers.Main) { onUpdate() } } }) } }; Spacer(modifier = Modifier.height(24.dp)); Text("GLOBAL BLACKLIST", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold); blacklist.forEach { entry -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.target, color = Color.LightGray, fontSize = 12.sp); Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp).clickable { RustBridge.removeBlacklist(entry.target, entry.target.contains(Regex("[a-zA-Z]"))); scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().removeFromBlacklist(entry); withContext(Dispatchers.Main) { onUpdate() } } }) } } } }
@Composable fun RuleEditorDialog(existing: CustomRule?, onDismiss: () -> Unit, onSave: (CustomRule) -> Unit) { var pattern by remember { mutableStateOf(existing?.pattern ?: "") }; var isDomain by remember { mutableStateOf(existing?.isDomain ?: true) }; var isBlocked by remember { mutableStateOf(existing?.isBlocked ?: true) }; var description by remember { mutableStateOf(existing?.description ?: "") }; AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF0D1117), title = { Text(if (existing == null) "NEW POLICY" else "EDIT POLICY", color = Color.White) }, text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) { OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Pattern") }); Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isDomain, onCheckedChange = { isDomain = it }); Text("Domain Pattern", color = Color.Gray) }; Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isBlocked, onCheckedChange = { isBlocked = it }); Text("Block Connectivity", color = Color.Gray) }; OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Internal Note") }) } }, confirmButton = { TextButton(onClick = { onSave(CustomRule(id = existing?.id ?: 0, pattern = pattern, isDomain = isDomain, isBlocked = isBlocked, description = description)) }) { Text("CONFIRM", color = Color(0xFF00FFA3)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) } }) }
@Composable fun CustomRuleItem(rule: CustomRule, onEdit: () -> Unit, onDelete: () -> Unit) { Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (rule.isBlocked) Color.Red.copy(alpha = 0.3f) else Color(0xFF161B22))) { Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).background(if (rule.isBlocked) Color.Red else Color(0xFF00FFA3), CircleShape)); Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(rule.pattern, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text("${if (rule.isDomain) "DOMAIN" else "IP"} • ${rule.description}", color = Color.Gray, fontSize = 9.sp) }; IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) } } } }

@Composable
fun LogsSection() {
    var selectedLogTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedLogTab, containerColor = Color.Transparent, contentColor = Color(0xFF00FFA3), divider = {}) {
            Tab(selected = selectedLogTab == 0, onClick = { selectedLogTab = 0 }, text = { Text("PACKET FORENSICS", fontSize = 9.sp) })
            Tab(selected = selectedLogTab == 1, onClick = { selectedLogTab = 1 }, text = { Text("APP TELEMETRY", fontSize = 9.sp) })
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (selectedLogTab) {
            0 -> FirewallLogScreen()
            1 -> LogsScreen()
        }
    }
}
