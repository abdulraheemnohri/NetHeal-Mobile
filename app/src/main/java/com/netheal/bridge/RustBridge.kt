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
    external fun addWhitelist(valStr: String, isDomain: Boolean)
    external fun removeWhitelist(valStr: String, isDomain: Boolean)
    external fun addBlacklist(valStr: String, isDomain: Boolean)
    external fun removeBlacklist(valStr: String, isDomain: Boolean)
    external fun heal()
    external fun resetStats()
    external fun getAnalytics(): ByteArray
    external fun runDiagnostics(): ByteArray
}
