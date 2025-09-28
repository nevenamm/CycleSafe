package com.cyclesafe.app.ui.screens.poi_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.model.Poi
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PoiListViewModel : ViewModel() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _pois = MutableStateFlow<List<Poi>>(emptyList())
    val pois = _pois.asStateFlow()

    init {
        fetchPois()
    }

    private fun fetchPois() {
        viewModelScope.launch {
            db.collection("pois")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        // Handle error
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        _pois.value = snapshot.toObjects(Poi::class.java)
                    }
                }
        }
    }
}