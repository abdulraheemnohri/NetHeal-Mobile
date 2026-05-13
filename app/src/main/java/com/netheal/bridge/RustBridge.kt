package com.netheal.bridge

object RustBridge {
    init {
        System.loadLibrary("netheal")
    }

    external fun analyze(domain: String, requests: Int, burst: Float): Boolean
    external fun setSecurityLevel(level: Byte)
    external fun setAppRule(appId: String, blocked: Boolean)
    external fun getSystemHealth(): Int
    external fun heal()
}
