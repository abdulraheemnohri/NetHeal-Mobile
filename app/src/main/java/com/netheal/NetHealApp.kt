package com.netheal

import android.app.Application
import androidx.room.Room
import com.netheal.data.AppDatabase

class NetHealApp : Application() {
    companion object {
        lateinit var database: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "netheal-db"
        ).build()
    }
}
