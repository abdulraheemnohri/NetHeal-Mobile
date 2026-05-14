package com.netheal.bridge

object RustBridge {
    init {
        System.loadLibrary("netheal")
    }

    external fun analyze(target: String, isIp: Boolean, requests: Int, burst: Float): Int
    external fun setSecurityLevel(level: Byte)
    external fun setAppRule(appId: String, blocked: Boolean)
    external fun getBlockedCount(): Long
    external fun getScannedCount(): Long
    external fun getSystemHealth(): Int
    external fun addWhitelist(domain: String)
    external fun removeWhitelist(domain: String)
    external fun addBlacklist(target: String)
    external fun removeBlacklist(target: String)
    external fun heal()
}
