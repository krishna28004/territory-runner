package com.example.territoryrunner.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "territory_prefs")

class TerritoryRepository(private val context: Context) {

    private val CAPTURED_CELLS_KEY = stringPreferencesKey("captured_cells")
    private val gson = Gson()

    suspend fun saveCapturedCells(cells: Set<String>) {
        withContext(Dispatchers.IO) {
            val jsonString = gson.toJson(cells)
            context.dataStore.edit { preferences ->
                // Avoid redundant writes by checking existing value
                if (preferences[CAPTURED_CELLS_KEY] != jsonString) {
                    preferences[CAPTURED_CELLS_KEY] = jsonString
                }
            }
        }
    }

    suspend fun loadCapturedCells(): Set<String> {
        return withContext(Dispatchers.IO) {
            val preferences = context.dataStore.data.first()
            val jsonString = preferences[CAPTURED_CELLS_KEY]
            
            if (jsonString.isNullOrEmpty()) {
                emptySet()
            } else {
                try {
                    val tokenType = object : TypeToken<Set<String>>() {}.type
                    gson.fromJson(jsonString, tokenType) ?: emptySet()
                } catch (e: Exception) {
                    emptySet()
                }
            }
        }
    }
}
