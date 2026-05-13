package com.netheal.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.netheal.MainActivity
import com.netheal.NetHealApp
import com.netheal.bridge.RustBridge
import com.netheal.data.ThreatLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class NetHealVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var thread: Thread? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NetHealVpn", "Service starting...")

        val builder = Builder()
        builder.setSession("NetHealVpn")
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()

        if (vpnInterface != null) {
            startForeground(1, createNotification("Firewall Protection Active"))
            thread = Thread { monitorTraffic(vpnInterface!!) }
            thread?.start()
        }

        return START_STICKY
    }

    private fun monitorTraffic(descriptor: ParcelFileDescriptor) {
        try {
            while (!Thread.interrupted()) {
                // Simulate domain analysis
                val simulatedDomain = "malicious-site-v2.net"
                val requests = 300
                val burst = 18.0f

                val isSafe = RustBridge.analyze(simulatedDomain, requests, burst)

                if (!isSafe) {
                    Log.w("NetHealVpn", "🚫 Blocked connection to $simulatedDomain")
                    serviceScope.launch {
                        NetHealApp.database.threatLogDao().insertLog(
                            ThreatLog(domain = simulatedDomain, riskScore = 100, action = "BLOCKED")
                        )
                    }
                    showThreatNotification(simulatedDomain)
                }

                // Simulate IP block check
                val simulatedIp = "185.244.25.1"
                if (RustBridge.isIpBlocked(simulatedIp)) {
                     Log.w("NetHealVpn", "🚫 Blocked blacklisted IP: $simulatedIp")
                }

                Thread.sleep(15000)
            }
        } catch (e: Exception) {
            RustBridge.heal()
        } finally {
            descriptor.close()
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NetHealApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("NetHeal Mobile")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showThreatNotification(domain: String) {
        val notification = NotificationCompat.Builder(this, NetHealApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Threat Blocked")
            .setContentText("Access to $domain was restricted.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        thread?.interrupt()
        vpnInterface?.close()
        super.onDestroy()
    }
}
