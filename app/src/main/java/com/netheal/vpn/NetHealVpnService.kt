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
import java.util.Random

class NetHealVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var thread: Thread? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val random = Random()

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
                // Simulation of traffic analysis
                val targets = listOf("api.tracker-network.com", "185.122.1.5", "telemetry.os.android", "ads.social-service.io", "legit-site.com")
                val target = targets[random.nextInt(targets.size)]
                val isIp = target.first().isDigit()

                // In a real implementation, we would extract the app context here
                val result = RustBridge.analyze(target, isIp, random.nextInt(400), random.nextFloat() * 10)

                if (result < 0) { // Blocked
                    val score = -result
                    Log.w("NetHealVpn", "🚫 Blocked connection to $target | Risk: $score")
                    serviceScope.launch {
                        NetHealApp.database.netHealDao().insertLog(
                            ThreatLog(domain = target, riskScore = score, action = "BLOCKED")
                        )
                    }
                    if (score > 90) showThreatNotification(target)
                }

                Thread.sleep(10000)
            }
        } catch (e: Exception) {
            Log.e("NetHealVpn", "Traffic monitoring interrupted", e)
            RustBridge.heal()
        } finally {
            try {
                descriptor.close()
            } catch (e: Exception) {
                // Ignore
            }
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
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showThreatNotification(domain: String) {
        val notification = NotificationCompat.Builder(this, NetHealApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Critical Threat Blocked")
            .setContentText("Automatic block: $domain")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.e("NetHealVpn", "Notification permission missing")
        }
    }

    override fun onDestroy() {
        thread?.interrupt()
        vpnInterface?.close()
        super.onDestroy()
    }
}
