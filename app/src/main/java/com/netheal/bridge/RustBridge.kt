package com.netheal.bridge

object RustBridge {
    init {
        System.loadLibrary("netheal")
    }

    /**
     * Analyzes a connection attempt.
     * @return Risk score (0-100). Negative value means the connection is BLOCKED.
     */
    external fun analyze(target: String, isIp: Boolean, requests: Int, burst: Float): Int

    external fun setSecurityLevel(level: Byte)

    external fun setAppRule(appId: String, blocked: Boolean)

    external fun getBlockedCount(): Long

    external fun getSystemHealth(): Int

    external fun addWhitelist(domain: String)

    external fun addBlacklist(target: String)

    external fun heal()
}
