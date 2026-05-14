package com.netheal.bridge

object RustBridge {
    init {
        System.loadLibrary("netheal")
    }

    external fun handlePacket(packet: ByteArray): Boolean
    external fun handlePacketWithApp(packet: ByteArray, appId: String): Boolean
    external fun recordIncoming(appId: String, bytes: Long)
    external fun setSecurityLevel(level: Int)
    external fun setAppRule(appId: String, state: Int)
    external fun getBlockedCount(): Long
    external fun getScannedCount(): Long
    external fun getSecurityScore(): Int
    external fun addWhitelist(valStr: String, isDomain: Boolean)
    external fun removeWhitelist(valStr: String, isDomain: Boolean)
    external fun addBlacklist(valStr: String, isDomain: Boolean)
    external fun removeBlacklist(valStr: String, isDomain: Boolean)
    external fun heal()
    external fun resetStats()
    external fun setUpstreamDns(dns: String)
    external fun getAnalytics(): ByteArray
    external fun runDiagnostics(): ByteArray
}
