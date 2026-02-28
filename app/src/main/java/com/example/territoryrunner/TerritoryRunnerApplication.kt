package com.example.territoryrunner

import android.app.Application
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration

class TerritoryRunnerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Optimize OSMDroid configuration map load speed
        val context = applicationContext
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        
        // Additional caching optimization (Part 3)
        Configuration.getInstance().cacheMapTileCount = 12
        Configuration.getInstance().cacheMapTileOvershoot = 12
    }
}
