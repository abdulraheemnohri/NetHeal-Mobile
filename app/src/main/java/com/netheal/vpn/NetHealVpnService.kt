package com.netheal.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.netheal.bridge.RustBridge
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class NetHealVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var thread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NetHealVpn", "Service starting...")

        // Setup VPN interface
        val builder = Builder()
        builder.setSession("NetHealVpn")
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()

        if (vpnInterface != null) {
            Log.d("NetHealVpn", "VPN Interface established")
            thread = Thread {
                monitorTraffic(vpnInterface!!)
            }
            thread?.start()
        } else {
            Log.e("NetHealVpn", "Failed to establish VPN interface")
        }

        return START_STICKY
    }

    private fun monitorTraffic(descriptor: ParcelFileDescriptor) {
        val inputStream = FileInputStream(descriptor.fileDescriptor)
        val outputStream = FileOutputStream(descriptor.fileDescriptor)
        val packet = ByteBuffer.allocate(32767)

        Log.d("NetHealVpn", "Monitoring traffic...")

        try {
            while (!Thread.interrupted()) {
                // In a real implementation, we would read packets here:
                // val length = inputStream.read(packet.array())
                // But for simulation/demonstration, we'll log calls to the bridge

                // Simulate periodic domain check
                val simulatedDomain = "suspicious-tracker.com"
                val requests = 85
                val isSafe = RustBridge.analyze(simulatedDomain, requests)

                if (!isSafe) {
                    Log.w("NetHealVpn", "🚫 Blocked connection to $simulatedDomain")
                }

                Thread.sleep(5000) // Don't spam logs
            }
        } catch (e: Exception) {
            Log.e("NetHealVpn", "Error in traffic monitoring", e)
            RustBridge.heal() // Trigger self-healing
        } finally {
            descriptor.close()
        }
    }

    override fun onDestroy() {
        thread?.interrupt()
        vpnInterface?.close()
        super.onDestroy()
    }
}
