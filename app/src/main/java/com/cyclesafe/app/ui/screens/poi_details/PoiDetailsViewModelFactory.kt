package com.cyclesafe.app.ui.screens.poi_details

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PoiDetailsViewModelFactory(private val application: Application, private val poiId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PoiDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PoiDetailsViewModel(application, poiId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}