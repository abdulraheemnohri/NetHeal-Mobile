package com.netheal.data

import androidx.room.*

@Entity(tableName = "threat_logs")
data class ThreatLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val domain: String,
    val riskScore: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String
)

@Entity(tableName = "incidents")
data class Incident(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val severity: String, // CRITICAL, WARNING, INFO
    val timestamp: Long = System.currentTimeMillis(),
    val sourceApp: String? = null
)

@Entity(tableName = "firewall_rules")
data class FirewallRule(
    @PrimaryKey val appId: String,
    val state: Int, // 0: Allowed, 1: WiFi-Only, 2: Blocked
    val bwLimit: Long = 0
)

@Entity(tableName = "ssid_rules")
data class SsidRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ssid: String,
    val appId: String,
    val blockOnSsid: Boolean
)

@Entity(tableName = "bypass_apps")
data class BypassApp(
    @PrimaryKey val appId: String
)

@Entity(tableName = "whitelist")
data class WhitelistEntry(
    @PrimaryKey val domain: String
)

@Entity(tableName = "blacklist")
data class BlacklistEntry(
    @PrimaryKey val target: String
)

@Entity(tableName = "custom_rules")
data class CustomRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pattern: String,
    val isDomain: Boolean,
    val isBlocked: Boolean,
    val description: String = ""
)

@Entity(tableName = "port_rules")
data class PortRule(
    @PrimaryKey val port: Int,
    val isBlocked: Boolean,
    val description: String = ""
)

@Entity(tableName = "geo_rules")
data class GeoRule(
    @PrimaryKey val countryIso: String,
    val isBlocked: Boolean
)

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startTime: String,
    val endTime: String,
    val profileLevel: Int,
    val isActive: Boolean = true
)

@Entity(tableName = "hourly_usage")
data class HourlyUsage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Long,
    val sent: Long,
    val recv: Long
)

@Entity(tableName = "usage_stats")
data class UsageStats(
    @PrimaryKey val day: String,
    val totalScanned: Long,
    val totalBlocked: Long
)

@Dao
interface NetHealDao {
    @Query("SELECT * FROM threat_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<ThreatLog>
    @Insert
    suspend fun insertLog(log: ThreatLog)
    @Query("DELETE FROM threat_logs")
    suspend fun deleteAllLogs()

    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    suspend fun getAllIncidents(): List<Incident>
    @Insert
    suspend fun insertIncident(incident: Incident)
    @Query("DELETE FROM incidents")
    suspend fun deleteAllIncidents()

    @Query("SELECT * FROM firewall_rules")
    suspend fun getAllRules(): List<FirewallRule>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRule(rule: FirewallRule)
    @Query("SELECT * FROM firewall_rules WHERE appId = :appId")
    suspend fun getRule(appId: String): FirewallRule?

    @Query("SELECT * FROM ssid_rules")
    suspend fun getAllSsidRules(): List<SsidRule>
    @Query("SELECT * FROM ssid_rules WHERE ssid = :ssid")
    suspend fun getRulesForSsid(ssid: String): List<SsidRule>
    @Insert
    suspend fun insertSsidRule(rule: SsidRule)
    @Delete
    suspend fun deleteSsidRule(rule: SsidRule)

    @Query("SELECT * FROM port_rules")
    suspend fun getAllPortRules(): List<PortRule>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePortRule(rule: PortRule)
    @Delete
    suspend fun deletePortRule(rule: PortRule)

    @Query("SELECT * FROM geo_rules")
    suspend fun getAllGeoRules(): List<GeoRule>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGeoRule(rule: GeoRule)
    @Delete
    suspend fun deleteGeoRule(rule: GeoRule)

    @Query("SELECT * FROM whitelist")
    suspend fun getWhitelist(): List<WhitelistEntry>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWhitelist(entry: WhitelistEntry)
    @Delete
    suspend fun removeFromWhitelist(entry: WhitelistEntry)

    @Query("SELECT * FROM blacklist")
    suspend fun getBlacklist(): List<BlacklistEntry>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToBlacklist(entry: BlacklistEntry)
    @Delete
    suspend fun removeFromBlacklist(entry: BlacklistEntry)

    @Query("SELECT * FROM custom_rules")
    suspend fun getAllCustomRules(): List<CustomRule>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomRule(rule: CustomRule)
    @Delete
    suspend fun deleteCustomRule(rule: CustomRule)

    @Query("SELECT * FROM schedules")
    suspend fun getAllSchedules(): List<Schedule>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSchedule(s: Schedule)
    @Delete
    suspend fun deleteSchedule(s: Schedule)

    @Query("SELECT * FROM hourly_usage ORDER BY hour DESC LIMIT 24")
    suspend fun getRecentHourly(): List<HourlyUsage>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateHourly(usage: HourlyUsage)

    @Query("SELECT * FROM usage_stats WHERE day = :day")
    suspend fun getStatsForDay(day: String): UsageStats?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStats(stats: UsageStats)

    @Query("SELECT * FROM bypass_apps")
    suspend fun getBypassApps(): List<BypassApp>
}

@Database(entities = [ThreatLog::class, Incident::class, FirewallRule::class, BypassApp::class, WhitelistEntry::class, BlacklistEntry::class, CustomRule::class, Schedule::class, HourlyUsage::class, UsageStats::class, SsidRule::class, PortRule::class, GeoRule::class], version = 14)
abstract class AppDatabase : RoomDatabase() {
    abstract fun netHealDao(): NetHealDao
}
