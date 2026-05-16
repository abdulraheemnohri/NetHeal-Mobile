package com.netheal.bridge

object RustBridge {
    init {
        System.loadLibrary("netheal")
    }

    external fun startEngine(): Boolean
    external fun stopEngine()
    external fun getStats(): String
    external fun setSecurityLevel(level: Int)
    external fun setAppRule(packageName: String, rule: Int)
    external fun getAppRule(packageName: String): Int
    external fun addWhitelist(target: String, isDomain: Boolean)
    external fun removeWhitelist(target: String, isDomain: Boolean)
    external fun addBlacklist(target: String, isDomain: Boolean)
    external fun removeBlacklist(target: String, isDomain: Boolean)
    external fun addPortBlock(port: Int)
    external fun removePortBlock(port: Int)
    external fun addGeoBlock(countryIso: String)
    external fun removeGeoBlock(countryIso: String)

    // Core Control
    external fun setBooster(active: Boolean)
    external fun setBoosterActive(active: Boolean)
    external fun setMultipath(active: Boolean)
    external fun setMultipathActive(active: Boolean)
    external fun setObfuscation(active: Boolean)
    external fun setShapingMode(mode: Int)
    external fun setPerformanceMode(active: Boolean) // Changed back to Boolean for compatibility
    external fun setBufferSize(size: Int)
    external fun setBatterySafeguard(active: Boolean)

    // AI & Advanced
    external fun setJulesActive(active: Boolean)
    external fun setNeuralShield(active: Boolean)
    external fun updateAiRisk(packageName: String, score: Int)
    external fun applyDpiScript(pattern: String, action: String)
    external fun setHoneypotMode(active: Boolean)
    external fun setFingerprintMask(maskType: Int)

    // Telemetry & Stats
    external fun getScannedCount(): Long
    external fun getBlockedCount(): Long
    external fun getSecurityScore(): Int
    external fun getAnalytics(): ByteArray // Returns ByteArray as expected by NetHealApp
    external fun recordHeartbeat()
    external fun clearLogs()

    // VPN Engine
    external fun setUpstreamDns(dns: String)
    external fun handlePacket(packet: ByteArray): Boolean
    external fun heal(packet: ByteArray? = null): ByteArray? // Default null for empty heal() call
}
