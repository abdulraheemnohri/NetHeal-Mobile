package com.netheal.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.netheal.bridge.RustBridge
import com.netheal.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen() {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var customRules by remember { mutableStateOf(listOf<CustomRule>()) }
    var whitelist by remember { mutableStateOf(listOf<WhitelistEntry>()) }
    var blacklist by remember { mutableStateOf(listOf<BlacklistEntry>()) }
    var bypassApps by remember { mutableStateOf(setOf<String>()) }

    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val filtered = if (showSystemApps) installedApps else installedApps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            allApps = filtered.map { it.packageName to pm.getApplicationLabel(it).toString() }.sortedBy { it.second }
            customRules = NetHealApp.database.netHealDao().getAllCustomRules()
            whitelist = NetHealApp.database.netHealDao().getWhitelist()
            blacklist = NetHealApp.database.netHealDao().getBlacklist()
            bypassApps = NetHealApp.database.netHealDao().getBypassApps().map { it.appId }.toSet()
        }
    }

    LaunchedEffect(showSystemApps) { loadData() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070A)).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("POLICY COMMAND", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text("ABSOLUTE TRAFFIC CONTROL", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { loadData() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Gray) }
        }

        Spacer(modifier = Modifier.height(20.dp))

        ScrollableTabRow(
            selectedTabIndex = activeTab, containerColor = Color.Transparent, contentColor = Color(0xFF00FFA3), edgePadding = 0.dp, divider = {},
            indicator = { tabPositions -> TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]), color = Color(0xFF00FFA3)) }
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) { Text("APP SHIELD", modifier = Modifier.padding(16.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) { Text("POLICIES", modifier = Modifier.padding(16.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) { Text("GLOBAL LISTS", modifier = Modifier.padding(16.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) { Text("BYPASS LIST", modifier = Modifier.padding(16.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (activeTab) {
            0 -> AppShieldSection(searchQuery, { searchQuery = it }, allApps, showSystemApps, { showSystemApps = it })
            1 -> CustomRulesSection(customRules, { loadData() })
            2 -> GlobalListsSection(whitelist, blacklist, { loadData() })
            3 -> BypassSection(allApps, bypassApps, { loadData() })
        }
    }
}

@Composable
fun AppShieldSection(query: String, onQueryChange: (String) -> Unit, apps: List<Pair<String, String>>, showSystem: Boolean, onToggleSystem: (Boolean) -> Unit) {
    val filteredApps = if (query.isEmpty()) apps else apps.filter { it.second.contains(query, ignoreCase = true) || it.first.contains(query, ignoreCase = true) }
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(
                value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps...", color = Color.Gray, fontSize = 14.sp) },
                trailingIcon = { IconButton(onClick = { onToggleSystem(!showSystem) }) { Icon(Icons.Default.Tune, contentDescription = null, tint = if (showSystem) Color(0xFF00FFA3) else Color.Gray) } },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFA3), unfocusedBorderColor = Color(0xFF161B22), focusedContainerColor = Color(0xFF161B22), unfocusedContainerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(filteredApps, key = { it.first }) { (packageName, label) -> AppRuleItem(packageName, label) }
    }
}

@Composable
fun BypassSection(allApps: List<Pair<String, String>>, bypass: Set<String>, onUpdate: () -> Unit) {
    val scope = rememberCoroutineScope()
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(allApps) { (pkg, label) ->
            val isBypassed = bypass.contains(pkg)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(pkg, color = Color.Gray, fontSize = 9.sp, maxLines = 1)
                    }
                    Switch(checked = isBypassed, onCheckedChange = {
                        scope.launch(Dispatchers.IO) {
                            if (it) NetHealApp.database.netHealDao().addBypassApp(BypassApp(pkg))
                            else NetHealApp.database.netHealDao().removeBypassApp(BypassApp(pkg))
                            withContext(Dispatchers.Main) { onUpdate() }
                        }
                    }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFA3)))
                }
            }
        }
    }
}

@Composable
fun GlobalListsSection(whitelist: List<WhitelistEntry>, blacklist: List<BlacklistEntry>, onUpdate: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("GLOBAL WHITELIST", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter IP/Domain to trust...", color = Color.Gray, fontSize = 12.sp) },
            trailingIcon = { IconButton(onClick = { if (input.isNotEmpty()) { val isDomain = input.contains(Regex("[a-zA-Z]")); RustBridge.addWhitelist(input, isDomain); scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().addToWhitelist(WhitelistEntry(input)); withContext(Dispatchers.Main) { onUpdate(); input = "" } } } }) { Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF00FFA3)) } },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFA3), unfocusedBorderColor = Color(0xFF161B22), focusedContainerColor = Color(0xFF0D1117), unfocusedContainerColor = Color(0xFF0D1117)),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        whitelist.forEach { entry -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.domain, color = Color.LightGray, fontSize = 12.sp); Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp).clickable { RustBridge.removeWhitelist(entry.domain, entry.domain.contains(Regex("[a-zA-Z]"))); scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().removeFromWhitelist(entry); withContext(Dispatchers.Main) { onUpdate() } } }) } }
        Spacer(modifier = Modifier.height(24.dp))
        Text("GLOBAL BLACKLIST", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        blacklist.forEach { entry -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.target, color = Color.LightGray, fontSize = 12.sp); Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp).clickable { RustBridge.removeBlacklist(entry.target, entry.target.contains(Regex("[a-zA-Z]"))); scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().removeFromBlacklist(entry); withContext(Dispatchers.Main) { onUpdate() } } }) } }
    }
}

@Composable
fun CustomRulesSection(rules: List<CustomRule>, onUpdate: () -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CustomRule?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22)), shape = RoundedCornerShape(10.dp)) { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00FFA3)); Spacer(modifier = Modifier.width(8.dp)); Text("ADD POLICY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(rules, key = { it.id }) { rule -> CustomRuleItem(rule, onEdit = { editingRule = rule; showAddDialog = true }, onDelete = { scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().deleteCustomRule(rule); withContext(Dispatchers.Main) { onUpdate() } } }) } }
    }
    if (showAddDialog) { RuleEditorDialog(existing = editingRule, onDismiss = { showAddDialog = false; editingRule = null }, onSave = { rule -> scope.launch(Dispatchers.IO) { NetHealApp.database.netHealDao().saveCustomRule(rule); if (rule.isBlocked) RustBridge.addBlacklist(rule.pattern, rule.isDomain) else RustBridge.addWhitelist(rule.pattern, rule.isDomain); withContext(Dispatchers.Main) { onUpdate(); showAddDialog = false; editingRule = null } } }) }
}

@Composable
fun RuleEditorDialog(existing: CustomRule?, onDismiss: () -> Unit, onSave: (CustomRule) -> Unit) {
    var pattern by remember { mutableStateOf(existing?.pattern ?: "") }
    var isDomain by remember { mutableStateOf(existing?.isDomain ?: true) }
    var isBlocked by remember { mutableStateOf(existing?.isBlocked ?: true) }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF0D1117), title = { Text(if (existing == null) "NEW POLICY" else "EDIT POLICY", color = Color.White) }, text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Pattern") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isDomain, onCheckedChange = { isDomain = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00FFA3))); Text("Domain Pattern", color = Color.Gray) }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isBlocked, onCheckedChange = { isBlocked = it }, colors = CheckboxDefaults.colors(checkedColor = Color.Red)); Text("Block Connectivity", color = Color.Gray) }
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Internal Note") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        }
    }, confirmButton = { TextButton(onClick = { onSave(CustomRule(id = existing?.id ?: 0, pattern = pattern, isDomain = isDomain, isBlocked = isBlocked, description = description)) }) { Text("CONFIRM", color = Color(0xFF00FFA3)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) } })
}

@Composable
fun CustomRuleItem(rule: CustomRule, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (rule.isBlocked) Color.Red.copy(alpha = 0.3f) else Color(0xFF161B22))) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).background(if (rule.isBlocked) Color.Red else Color(0xFF00FFA3), CircleShape)); Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(rule.pattern, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text("${if (rule.isDomain) "DOMAIN" else "IP"} • ${rule.description}", color = Color.Gray, fontSize = 9.sp) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun AppRuleItem(appId: String, appName: String) {
    var state by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(appId) { state = NetHealApp.database.netHealDao().getAppState(appId) ?: 0 }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (state == 2) Color.Red.copy(alpha = 0.2f) else if (state == 1) Color.Yellow.copy(alpha = 0.2f) else Color(0xFF161B22))) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(appName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(appId, color = Color.Gray, fontSize = 9.sp, maxLines = 1) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state = if (state == 1) 0 else 1; RustBridge.setAppRule(appId, state); scope.launch { NetHealApp.database.netHealDao().saveRule(FirewallRule(appId, state)) } }) { Icon(Icons.Default.Wifi, contentDescription = null, tint = if (state == 1) Color.Yellow else Color.Gray, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = { state = if (state == 2) 0 else 2; RustBridge.setAppRule(appId, state); scope.launch { NetHealApp.database.netHealDao().saveRule(FirewallRule(appId, state)) } }) { Icon(Icons.Default.Block, contentDescription = null, tint = if (state == 2) Color.Red else Color.Gray, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}
