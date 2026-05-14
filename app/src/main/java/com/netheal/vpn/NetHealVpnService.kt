package com.netheal.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
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
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }
        acquireWakeLock()
        val prefs = getSharedPreferences("netheal_prefs", MODE_PRIVATE)
        val dns = prefs.getString("upstream_dns", "Cloudflare") ?: "Cloudflare"
        val dnsIp = if (dns == "Cloudflare") "1.1.1.1" else if (dns == "AdGuard") "94.140.14.14" else "8.8.8.8"
        RustBridge.setUpstreamDns(dnsIp)
        setupVpn()
        return START_STICKY
    }

    private fun setupVpn() {
        try {
            val builder = Builder()
            builder.setSession("NetHealVpn")
            builder.addAddress("10.0.0.2", 24)
            builder.addRoute("0.0.0.0", 0)
            builder.setMtu(1500)
            builder.addDnsServer("10.0.0.2")
            builder.setBlocking(false)

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                startForeground(1, createNotification("Absolute Core Active"))
                thread = Thread { runVpnLoop(vpnInterface!!) }
                thread?.start()
            }
        } catch (e: Exception) { Log.e("NetHealVpn", "Establish failed", e) }
    }

    private fun runVpnLoop(descriptor: ParcelFileDescriptor) {
        val inputStream = FileInputStream(descriptor.fileDescriptor)
        val outputStream = FileOutputStream(descriptor.fileDescriptor)
        val packet = ByteBuffer.allocate(32768)
        try {
            while (!Thread.interrupted()) {
                val length = inputStream.read(packet.array())
                if (length > 0) {
                    val data = ByteArray(length)
                    System.arraycopy(packet.array(), 0, data, 0, length)
                    // Real traffic attribution: map outgoing packet to package (simulated)
                    val allowed = RustBridge.handlePacket(data)
                    if (allowed) { outputStream.write(data, 0, length) }
                    else {
                        serviceScope.launch { NetHealApp.database.netHealDao().insertLog(ThreatLog(domain = "ABSOLUTE_FILTER_BLOCK", riskScore = 100, action = "DROPPED")) }
                    }
                }
                packet.clear()
            }
        } catch (e: Exception) { Log.e("NetHealVpn", "Loop error", e); RustBridge.heal() }
        finally { cleanup() }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NetHeal::VpnWakeLock")
        wakeLock?.acquire()
    }

    private fun cleanup() {
        try {
            vpnInterface?.close()
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {}
    }

    private fun stopVpn() {
        thread?.interrupt()
        cleanup()
        vpnInterface = null
        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, NetHealApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("NetHeal Absolute")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() { stopVpn(); super.onDestroy() }
}
