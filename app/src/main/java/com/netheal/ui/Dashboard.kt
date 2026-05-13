package com.netheal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Dashboard(onToggleFirewall: (Boolean) -> Unit) {
    var isEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E14))
            .padding(24.dp)
    ) {
        Text(
            text = "🔥 NetHeal Mobile",
            color = Color(0xFF00FFA3),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Security Status", color = Color.Gray, fontSize = 14.sp)
                Text(
                    text = if (isEnabled) "ACTIVE & SECURE" else "FIREWALL DISABLED",
                    color = if (isEnabled) Color(0xFF00FFA3) else Color.Red,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Button(
            onClick = {
                isEnabled = !isEnabled
                onToggleFirewall(isEnabled)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) Color.Red else Color(0xFF00FFA3),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isEnabled) "Disable Protection" else "Enable Protection")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Live Threat Feed", color = Color.White, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(5) { i ->
                ThreatItem(i)
            }
        }
    }
}

@Composable
fun ThreatItem(index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2128))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Red, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Blocked connection to untrusted domain", color = Color.White, fontSize = 12.sp)
                Text("threat-server-$index.net | Risk: 85/100", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}
