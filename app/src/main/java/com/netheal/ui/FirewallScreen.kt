package com.netheal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FirewallScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E14))
            .padding(24.dp)
    ) {
        Text(
            text = "Firewall Rules",
            color = Color(0xFF00FFA3),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Manage per-app and global domain blocking rules.", color = Color.Gray)

        // Placeholder for rules list
        Spacer(modifier = Modifier.height(32.dp))

        Text("No custom rules defined.", color = Color.White)
    }
}
