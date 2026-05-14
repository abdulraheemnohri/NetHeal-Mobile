package com.netheal.bridge

object RustBridge {
    init {
        System.loadLibrary("netheal")
    }

    external fun analyze(domain: String, requests: Int): Boolean
    external fun handlePacket(packet: ByteArray): Boolean
    external fun setSecurityLevel(level: Int)
    external fun setAppRule(appId: String, state: Int)
    external fun getBlockedCount(): Long
    external fun getScannedCount(): Long
    external fun getSecurityScore(): Int
    external fun addWhitelist(ip: String)
    external fun removeWhitelist(ip: String)
    external fun addBlacklist(ip: String)
    external fun removeBlacklist(ip: String)
    external fun heal()
    external fun resetStats()
    external fun getAnalytics(): ByteArray
    external fun runDiagnostics(): ByteArray
}
