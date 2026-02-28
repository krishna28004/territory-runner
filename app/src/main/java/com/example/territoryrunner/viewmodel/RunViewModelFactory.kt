package com.example.territoryrunner.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.territoryrunner.repository.TerritoryRepository

class RunViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunViewModel::class.java)) {
            val repository = TerritoryRepository(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return RunViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
