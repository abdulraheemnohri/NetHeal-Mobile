package com.netheal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var autoHeal by remember { mutableStateOf(true) }
    var notificationLevel by remember { mutableStateOf("High") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
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
            Text("General", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Auto-Healing", color = Color.White)
                Switch(
                    checked = autoHeal,
                    onCheckedChange = { autoHeal = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFA3))
                )
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(20.dp))

            Text("Notifications", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Threat Alert Level", color = Color.White)
                Text(notificationLevel, color = Color.Gray)
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))
        }
    }
}
