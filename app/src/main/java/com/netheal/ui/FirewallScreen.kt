package com.netheal.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(onBack: () -> Unit) {
    val apps = listOf("Chrome", "YouTube", "System Update", "SocialApp", "GameX")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firewall Rules", color = Color.White) },
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
            Text("Per-App Internet Control", color = Color(0xFF00FFA3), fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(apps) { app ->
                    AppRuleItem(app)
                }
            }
        }
    }
}

@Composable
fun AppRuleItem(appName: String) {
    var isBlocked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161B22))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(appName, color = Color.White, fontWeight = FontWeight.SemiBold)

        Switch(
            checked = !isBlocked,
            onCheckedChange = { isBlocked = !it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00FFA3),
                uncheckedThumbColor = Color.Red
            )
        )
    }
}
