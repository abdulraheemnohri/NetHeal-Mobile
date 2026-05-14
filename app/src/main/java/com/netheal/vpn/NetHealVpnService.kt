package com.netheal.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NetHealVpn", "Service starting...")

        val builder = Builder()
        builder.setSession("NetHealVpn")
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)
        builder.setMtu(1500)

        // Block IPv6 to force IPv4 (simplifies demo)
        builder.addAddress("fd00::2", 128)
        builder.addRoute("::", 0)

        vpnInterface = builder.establish()

        if (vpnInterface != null) {
            startForeground(1, createNotification("Firewall Protection Active"))
            thread = Thread { runVpnLoop(vpnInterface!!) }
            thread?.start()
        }

        return START_STICKY
    }

    private fun runVpnLoop(descriptor: ParcelFileDescriptor) {
        val inputStream = FileInputStream(descriptor.fileDescriptor)
        val outputStream = FileOutputStream(descriptor.fileDescriptor)
        val packet = ByteBuffer.allocate(32768) // Larger buffer for safety

        try {
            while (!Thread.interrupted()) {
                val length = inputStream.read(packet.array())
                if (length > 0) {
                    val data = ByteArray(length)
                    System.arraycopy(packet.array(), 0, data, 0, length)

                    val allowed = RustBridge.handlePacket(data)

                    if (allowed) {
                        outputStream.write(data, 0, length)
                    } else {
                        // Log block event
                        serviceScope.launch {
                            NetHealApp.database.netHealDao().insertLog(
                                ThreatLog(domain = "PACKET_DROPPED", riskScore = 100, action = "BLOCKED")
                            )
                        }
                    }
                }
                packet.clear()
            }
        } catch (e: Exception) {
            Log.e("NetHealVpn", "VPN Loop error", e)
            RustBridge.heal()
        } finally {
            try {
                descriptor.close()
                inputStream.close()
                outputStream.close()
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

    override fun onDestroy() {
        thread?.interrupt()
        vpnInterface?.close()
        super.onDestroy()
    }
}
