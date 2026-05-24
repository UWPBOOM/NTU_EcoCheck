package com.ntu.electricity

import android.app.Application
import com.ntu.electricity.data.local.AppDatabase
import com.ntu.electricity.data.local.UserPreferences
import com.ntu.electricity.network.NtuHttpClient
import com.ntu.electricity.network.RoomDataLoader

class EcoCheckApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var httpClient: NtuHttpClient
        private set
    lateinit var roomDataLoader: RoomDataLoader
        private set
    lateinit var userPreferences: UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        httpClient = NtuHttpClient(this)
        roomDataLoader = RoomDataLoader(this)
        userPreferences = UserPreferences(this)
    }
}
