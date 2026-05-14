package com.netheal.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.netheal.data.CustomRule
import com.netheal.data.FirewallRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen() {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var customRules by remember { mutableStateOf(listOf<CustomRule>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Apps, 1: Custom Rules

    val scope = rememberCoroutineScope()

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

    fun loadCustomRules() {
        scope.launch(Dispatchers.IO) {
            customRules = NetHealApp.database.netHealDao().getAllCustomRules()
        }
    }

    LaunchedEffect(showSystemApps) { loadApps() }
    LaunchedEffect(Unit) { loadCustomRules() }

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
                Text("FIREWALL CORE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text("POLICY & TRAFFIC CONTROL", color = Color(0xFF00FFA3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    if (activeTab == 0) {
                        val rules = NetHealApp.database.netHealDao().getAllRules()
                        rules.forEach {
                            NetHealApp.database.netHealDao().saveRule(FirewallRule(it.appId, false))
                            RustBridge.setAppRule(it.appId, false)
                        }
                        loadApps()
                    } else {
                        NetHealApp.database.netHealDao().deleteAllCustomRules()
                        loadCustomRules()
                    }
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF161B22))
        ) {
            TabItem(modifier = Modifier.weight(1f), label = "APP SHIELD", isSelected = activeTab == 0) { activeTab = 0 }
            TabItem(modifier = Modifier.weight(1f), label = "CUSTOM RULES", isSelected = activeTab == 1) { activeTab = 1 }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (activeTab == 0) {
            AppShieldSection(searchQuery, { searchQuery = it }, allApps, showSystemApps, { showSystemApps = it })
        } else {
            CustomRulesSection(customRules, { loadCustomRules() })
        }
    }
}

@Composable
fun TabItem(modifier: Modifier, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isSelected) Color(0xFF00FFA3) else Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppShieldSection(
    query: String,
    onQueryChange: (String) -> Unit,
    apps: List<Pair<String, String>>,
    showSystem: Boolean,
    onToggleSystem: (Boolean) -> Unit
) {
    val filteredApps = if (query.isEmpty()) apps else apps.filter {
        it.second.contains(query, ignoreCase = true) || it.first.contains(query, ignoreCase = true)
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search apps...", color = Color.Gray, fontSize = 14.sp) },
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
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onToggleSystem(!showSystem) }) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = if (showSystem) Color(0xFF00FFA3) else Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (apps.isEmpty()) {
            CircularProgressIndicator(color = Color(0xFF00FFA3), modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredApps, key = { it.first }) { (packageName, label) ->
                    AppRuleItem(packageName, label)
                }
            }
        }
    }
}

@Composable
fun CustomRulesSection(rules: List<CustomRule>, onUpdate: () -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CustomRule?>(null) }

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00FFA3))
            Spacer(modifier = Modifier.width(8.dp))
            Text("NEW NETWORK RULE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (rules.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No custom rules defined.", color = Color.DarkGray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rules, key = { it.id }) { rule ->
                    CustomRuleItem(rule,
                        onEdit = { editingRule = rule; showAddDialog = true },
                        onDelete = {
                            scope.launch(Dispatchers.IO) {
                                NetHealApp.database.netHealDao().deleteCustomRule(rule)
                                withContext(Dispatchers.Main) { onUpdate() }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        RuleEditorDialog(
            existing = editingRule,
            onDismiss = { showAddDialog = false; editingRule = null },
            onSave = { rule ->
                scope.launch(Dispatchers.IO) {
                    NetHealApp.database.netHealDao().saveCustomRule(rule)
                    // If blocked, sync with engine (simplified)
                    if (rule.isBlocked) {
                        if (rule.isDomain) RustBridge.addBlacklist(rule.pattern)
                        else RustBridge.addBlacklist(rule.pattern)
                    } else {
                        RustBridge.addWhitelist(rule.pattern)
                    }
                    withContext(Dispatchers.Main) {
                        onUpdate()
                        showAddDialog = false
                        editingRule = null
                    }
                }
            }
        )
    }
}

@Composable
fun RuleEditorDialog(existing: CustomRule?, onDismiss: () -> Unit, onSave: (CustomRule) -> Unit) {
    var pattern by remember { mutableStateOf(existing?.pattern ?: "") }
    var isDomain by remember { mutableStateOf(existing?.isDomain ?: true) }
    var isBlocked by remember { mutableStateOf(existing?.isBlocked ?: true) }
    var description by remember { mutableStateOf(existing?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1117),
        title = { Text(if (existing == null) "ADD RULE" else "EDIT RULE", color = Color.White) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Pattern (IP or Domain)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDomain, onCheckedChange = { isDomain = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00FFA3)))
                    Text("Domain Pattern", color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isBlocked, onCheckedChange = { isBlocked = it }, colors = CheckboxDefaults.colors(checkedColor = Color.Red))
                    Text("Block Connectivity", color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Internal Note") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(CustomRule(id = existing?.id ?: 0, pattern = pattern, isDomain = isDomain, isBlocked = isBlocked, description = description)) }) {
                Text("SAVE POLICY", color = Color(0xFF00FFA3))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
        }
    )
}

@Composable
fun CustomRuleItem(rule: CustomRule, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (rule.isBlocked) Color.Red.copy(alpha = 0.3f) else Color(0xFF161B22))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(if (rule.isBlocked) Color.Red else Color(0xFF00FFA3), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.pattern, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${if (rule.isDomain) "DOMAIN" else "IP"} • ${rule.description}", color = Color.Gray, fontSize = 9.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AppRuleItem(appId: String, appName: String) {
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
                Text(appId, color = Color.Gray, fontSize = 9.sp, maxLines = 1)
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
