package com.netheal.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            apps = installedApps.filter {
                (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }.map {
                it.packageName to pm.getApplicationLabel(it).toString()
            }.sortedBy { it.second }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Control", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E14))
            )
        },
        containerColor = Color(0xFF05070A)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text("RESTRICT APP ACCESS", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)

            Spacer(modifier = Modifier.height(16.dp))

            if (apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00FFA3))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(apps) { (packageName, label) ->
                        AppRuleItem(packageName, label)
                    }
                }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161B22))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(appName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(appId, color = Color.Gray, fontSize = 10.sp)
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
