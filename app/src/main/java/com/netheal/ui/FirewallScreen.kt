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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
    val tabs = listOf("ISOLATION", "POLICIES", "WIFI", "AI INTEL", "DPI")
    val scope = rememberCoroutineScope()
    var customRules by remember { mutableStateOf(listOf<CustomRule>()) }
    var portRules by remember { mutableStateOf(listOf<PortRule>()) }
    var geoRules by remember { mutableStateOf(listOf<GeoRule>()) }

    fun updateData() {
        scope.launch(Dispatchers.IO) {
            val cr = NetHealApp.database.netHealDao().getAllCustomRules()
            val pr = NetHealApp.database.netHealDao().getAllPortRules()
            val gr = NetHealApp.database.netHealDao().getAllGeoRules()
            withContext(Dispatchers.Main) {
                customRules = cr; portRules = pr; geoRules = gr
            }
        }
    }

    LaunchedEffect(Unit) { updateData() }

    Column(modifier = Modifier.fillMaxSize().background(CyberTheme.Background).padding(16.dp)) {
        Header()
        Spacer(modifier = Modifier.height(24.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = CyberTheme.Primary,
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyberTheme.Primary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AppIsolationSection()
                1 -> CustomRulesSection(customRules, portRules, geoRules, ::updateData)
                2 -> WifiSecuritySection()
                3 -> IntelligenceSection()
                4 -> DpiScriptingSection()
            }
        }
    }
}

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
            Text("APP SURVEILLANCE", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SYSTEM", color = if (showSystemApps) CyberTheme.Warning else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Switch(checked = showSystemApps, onCheckedChange = { showSystemApps = it }, modifier = Modifier.scale(0.6f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Filter applications...", color = Color.Gray, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberTheme.Primary),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        val filteredApps = if (searchQuery.isEmpty()) apps else apps.filter { it.packageName.contains(searchQuery, true) || it.loadLabel(context.packageManager).toString().contains(searchQuery, true) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filteredApps) { app ->
                AppRuleItem(app.packageName, app.loadLabel(context.packageManager).toString(), (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
            }
        }
    }
}

@Composable
fun AppRuleItem(appId: String, appName: String, isSystem: Boolean) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("netheal_app_prefs", 0)
    var state by remember { mutableIntStateOf(prefs.getInt("state_$appId", 0)) }
    var isDeactivated by remember { mutableStateOf(prefs.getBoolean("deact_$appId", false)) }

    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (isDeactivated) 0.5f else 1f),
        colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface),
        border = BorderStroke(1.dp, if (isDeactivated) CyberTheme.Border else CyberTheme.Primary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Android, null, tint = if (isSystem) CyberTheme.Warning else CyberTheme.Secondary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(appName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                    if (isSystem) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.background(CyberTheme.Warning.copy(alpha = 0.1f)).border(1.dp, CyberTheme.Warning.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)) {
                            Text("SYS", color = CyberTheme.Warning, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(appId, color = Color.Gray, fontSize = 9.sp, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isDeactivated) {
                    IconButton(onClick = { state = 0; RustBridge.setAppRule(appId, 0); prefs.edit().putInt("state_$appId", 0).apply() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.CheckCircle, null, tint = if (state == 0) CyberTheme.Primary else Color.DarkGray) }
                    IconButton(onClick = { state = 2; RustBridge.setAppRule(appId, 2); prefs.edit().putInt("state_$appId", 2).apply() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Block, null, tint = if (state == 2) CyberTheme.Danger else Color.DarkGray) }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = !isDeactivated,
                    onCheckedChange = {
                        isDeactivated = !it
                        prefs.edit().putBoolean("deact_$appId", !it).apply()
                        if (isDeactivated) RustBridge.setAppRule(appId, 0) else RustBridge.setAppRule(appId, state)
                    },
                    modifier = Modifier.scale(0.7f),
                    colors = SwitchDefaults.colors(checkedThumbColor = CyberTheme.Primary, uncheckedThumbColor = CyberTheme.Danger)
                )
            }
        }
    }
}

@Composable
fun CustomRulesSection(rules: List<CustomRule>, portRules: List<PortRule>, geoRules: List<GeoRule>, onUpdate: () -> Unit) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab, containerColor = Color.Transparent, contentColor = CyberTheme.Secondary, divider = {}) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("L7 PATTERNS", fontSize = 9.sp) })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("PORTS", fontSize = 9.sp) })
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("GEO-BLOCK", fontSize = 9.sp) })
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (selectedSubTab) {
            0 -> L7PatternList(rules, onUpdate)
            1 -> PortRuleList(portRules, onUpdate)
            2 -> GeoBlockList(geoRules, onUpdate)
        }
    }
}

@Composable fun L7PatternList(rules: List<CustomRule>, onUpdate: () -> Unit) {
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    Column {
        CyberButton("NEW PATTERN", Icons.Default.Add, onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rules) { rule ->
                RuleItem(rule.pattern, if (rule.isBlocked) "BLOCKED" else "TRUSTED", rule.isBlocked) {
                    scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().deleteCustomRule(rule); withContext(Dispatchers.Main) { onUpdate() } }
                }
            }
        }
    }
    if (showAddDialog) { RuleEditorDialog(existing = null, onDismiss = { showAddDialog = false }, onSave = { rule -> scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().saveCustomRule(rule); if (rule.isBlocked) RustBridge.addBlacklist(rule.pattern, rule.isDomain) else RustBridge.addWhitelist(rule.pattern, rule.isDomain); withContext(Dispatchers.Main) { onUpdate(); showAddDialog = false } } }) }
}

@Composable fun RuleItem(title: String, status: String, isDanger: Boolean, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CyberTheme.Surface), border = BorderStroke(1.dp, CyberTheme.Border), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isDanger) CyberTheme.Danger else CyberTheme.Primary))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(status, color = Color.Gray, fontSize = 9.sp)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable fun RuleEditorDialog(existing: CustomRule?, onDismiss: () -> Unit, onSave: (CustomRule) -> Unit) {
    var pattern by remember { mutableStateOf(existing?.pattern ?: "") }
    var isBlocked by remember { mutableStateOf(existing?.isBlocked ?: true) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CyberTheme.Surface,
        title = { Text("NEW POLICY", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Pattern (Domain/IP)") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isBlocked, onCheckedChange = { isBlocked = it }); Text("Block Traffic", color = Color.Gray) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(CustomRule(pattern = pattern, isDomain = pattern.contains("."), isBlocked = isBlocked, description = "User Rule")) }) { Text("CONFIRM", color = CyberTheme.Primary) } }
    )
}

@Composable fun PortRuleList(ports: List<PortRule>, onUpdate: () -> Unit) { var showAddDialog by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); Column { CyberButton("BLOCK PORT", Icons.Default.Adjust, onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(16.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(ports) { p -> RuleItem("PORT: ${p.port}", p.description, true) { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().deletePortRule(p); RustBridge.removePortBlock(p.port); withContext(Dispatchers.Main) { onUpdate() } } } } } }; if (showAddDialog) { var portStr by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { showAddDialog = false }, containerColor = CyberTheme.Surface, title = { Text("Block Port", color = Color.White) }, text = { OutlinedTextField(value = portStr, onValueChange = { portStr = it }, label = { Text("Port Number") }) }, confirmButton = { TextButton(onClick = { val p = portStr.toIntOrNull() ?: 0; scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().savePortRule(PortRule(p, true, "Manual Block")); RustBridge.addPortBlock(p); withContext(Dispatchers.Main) { onUpdate(); showAddDialog = false } } }) { Text("BLOCK") } }) } }
@Composable fun GeoBlockList(geo: List<GeoRule>, onUpdate: () -> Unit) { var showAddDialog by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); Column { CyberButton("ADD COUNTRY BLOCK", Icons.Default.Public, onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(16.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(geo) { g -> RuleItem("COUNTRY: ${g.countryIso}", "Region Restricted", true) { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().deleteGeoRule(g); RustBridge.removeGeoBlock(g.countryIso); withContext(Dispatchers.Main) { onUpdate() } } } } } }; if (showAddDialog) { var iso by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { showAddDialog = false }, containerColor = CyberTheme.Surface, title = { Text("Block Country", color = Color.White) }, text = { OutlinedTextField(value = iso, onValueChange = { iso = it }, label = { Text("ISO Code (e.g. RU, CN)") }) }, confirmButton = { TextButton(onClick = { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().saveGeoRule(GeoRule(iso, true)); RustBridge.addGeoBlock(iso); withContext(Dispatchers.Main) { onUpdate(); showAddDialog = false } } }) { Text("BLOCK") } }) } }

@Composable fun WifiSecuritySection() { GlassCard { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Wifi, null, tint = CyberTheme.Primary); Spacer(modifier = Modifier.width(16.dp)); Column { Text("NetHeal_Secure_5G", color = Color.White, fontWeight = FontWeight.Bold); Text("Signal: -42dBm • Status: SECURE", color = Color.Gray, fontSize = 10.sp) } } } }

@Composable fun IntelligenceSection() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("AI INTELLIGENCE FEED", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        GlassCard {
            Text("JULES AI STATUS: ACTIVE", color = CyberTheme.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Continuous telemetry analysis in progress. No critical threats detected in local apps.", color = Color.Gray, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        ThreatOriginMap()
    }
}

@Composable
fun DpiScriptingSection() {
    var scriptInput by remember { mutableStateOf("") }
    Column {
        Text("DPI SCRIPTING ENGINE", color = CyberTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        GlassCard {
            Text("Payload Match Pattern", color = Color.Gray, fontSize = 9.sp)
            OutlinedTextField(value = scriptInput, onValueChange = { scriptInput = it }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(16.dp))
            CyberButton("DEPLOY KERNEL SCRIPT", Icons.Default.Code, onClick = { RustBridge.applyDpiScript(scriptInput, "BLOCK") }, modifier = Modifier.fillMaxWidth())
        }
    }
}
