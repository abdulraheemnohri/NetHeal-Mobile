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

@Dao
interface ThreatLogDao {
    @Query("SELECT * FROM threat_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<ThreatLog>

    @Insert
    suspend fun insertLog(log: ThreatLog)

    @Query("DELETE FROM threat_logs")
    suspend fun deleteAllLogs()
}

@Database(entities = [ThreatLog::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun threatLogDao(): ThreatLogDao
}
