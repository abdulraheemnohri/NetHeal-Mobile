package com.netheal.bridge

object RustBridge {
    init {
        System.loadLibrary("netheal")
    }

    external fun handlePacket(packet: ByteArray): Boolean
    external fun handlePacketWithApp(packet: ByteArray, appId: String): Boolean
    external fun recordIncoming(appId: String, bytes: Long)
    external fun recordHeartbeat()
    external fun setSecurityLevel(level: Int)
    external fun setProfile(profile: String)
    external fun setUpstreamDns(dns: String)
    external fun setPerformanceMode(enabled: Boolean)
    external fun setStealthMode(enabled: Boolean)
    external fun setDnsHardening(enabled: Boolean)
    external fun setLearningMode(enabled: Boolean)
    external fun setJulesActive(enabled: Boolean)
    external fun updateAiRisk(target: String, risk: Int)
    external fun setAppRule(appId: String, state: Int)
    external fun setAppBwLimit(appId: String, limit: Long)
    external fun getBlockedCount(): Long
    external fun getScannedCount(): Long
    external fun getSecurityScore(): Int
    external fun addWhitelist(valStr: String, isDomain: Boolean)
    external fun removeWhitelist(valStr: String, isDomain: Boolean)
    external fun addBlacklist(valStr: String, isDomain: Boolean)
    external fun removeBlacklist(valStr: String, isDomain: Boolean)
    external fun addGeoBlock(country: String)
    external fun removeGeoBlock(country: String)
    external fun addPortBlock(port: Int)
    external fun removePortBlock(port: Int)
    external fun killIp(ip: String)
    external fun heal()
    external fun resetStats()
    external fun getAnalytics(): ByteArray
    external fun runDiagnostics(): ByteArray
}
