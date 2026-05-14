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

@Entity(tableName = "firewall_rules")
data class FirewallRule(
    @PrimaryKey val appId: String,
    val isBlocked: Boolean
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
    @Query("DELETE FROM threat_logs WHERE timestamp < :timestamp")
    suspend fun deleteLogsOlderThan(timestamp: Long)

    @Query("SELECT * FROM firewall_rules")
    suspend fun getAllRules(): List<FirewallRule>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRule(rule: FirewallRule)
    @Query("SELECT isBlocked FROM firewall_rules WHERE appId = :appId")
    suspend fun isAppBlocked(appId: String): Boolean?

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
    @Query("DELETE FROM custom_rules")
    suspend fun deleteAllCustomRules()

    @Query("SELECT * FROM usage_stats WHERE day = :day")
    suspend fun getStatsForDay(day: String): UsageStats?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStats(stats: UsageStats)
}

@Database(entities = [ThreatLog::class, FirewallRule::class, WhitelistEntry::class, BlacklistEntry::class, CustomRule::class, UsageStats::class], version = 7)
abstract class AppDatabase : RoomDatabase() {
    abstract fun netHealDao(): NetHealDao
}
