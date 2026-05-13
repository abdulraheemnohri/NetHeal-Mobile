package com.netheal.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
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
            thread = Thread { monitorTraffic(vpnInterface!!) }
            thread?.start()
        }

        return START_STICKY
    }

    private fun monitorTraffic(descriptor: ParcelFileDescriptor) {
        try {
            while (!Thread.interrupted()) {
                val simulatedDomain = "suspicious-tracker.com"
                val requests = 85
                val isSafe = RustBridge.analyze(simulatedDomain, requests, 1.0f)

                if (!isSafe) {
                    Log.w("NetHealVpn", "🚫 Blocked connection to $simulatedDomain")
                    serviceScope.launch {
                        NetHealApp.database.threatLogDao().insertLog(
                            ThreatLog(domain = simulatedDomain, riskScore = 85, action = "BLOCKED")
                        )
                    }
                }
                Thread.sleep(10000)
            }
        } catch (e: Exception) {
            RustBridge.heal()
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
