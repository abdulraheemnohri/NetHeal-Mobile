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

@Dao
interface NetHealDao {
    @Query("SELECT * FROM threat_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<ThreatLog>
    @Insert
    suspend fun insertLog(log: ThreatLog)
    @Query("DELETE FROM threat_logs")
    suspend fun deleteAllLogs()

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
}

@Database(entities = [ThreatLog::class, FirewallRule::class, WhitelistEntry::class, BlacklistEntry::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun netHealDao(): NetHealDao
}
