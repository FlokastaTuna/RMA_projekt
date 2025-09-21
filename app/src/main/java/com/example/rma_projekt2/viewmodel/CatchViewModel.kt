package com.example.rma_projekt2.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Catch(
    val id: String = "",
    val fishType: String = "",
    val weight: Double = 0.0,
    val photoUrl: String = "",
    val userID: String = "",
    val createdAt: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null
)

class CatchViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _catches = MutableStateFlow<List<Catch>>(emptyList())
    val catches: StateFlow<List<Catch>> = _catches

    private val _availablePhotos = MutableStateFlow<List<String>>(emptyList())
    val availablePhotos: StateFlow<List<String>> = _availablePhotos

    fun fetchCatches(userId: String) {
        db.collection("catches")
            .whereEqualTo("userID", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        Catch(
                            id = doc.id,
                            fishType = doc.getString("fishType") ?: "",
                            weight = doc.getDouble("weight") ?: 0.0,
                            photoUrl = doc.getString("photoUrl") ?: "",
                            userID = doc.getString("userID") ?: "",
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            latitude = doc.getDouble("latitude"),
                            longitude = doc.getDouble("longitude")
                        )
                    } catch (e: Exception) {
                        null // Skip invalid documents
                    }
                }
                _catches.value = items
            }
            .addOnFailureListener {
                _catches.value = emptyList() // Handle failure gracefully
            }
    }

    fun fetchAvailablePhotos() {
        db.collection("availablePhotos")
            .get()
            .addOnSuccessListener { snapshot ->
                val photos = snapshot.documents.mapNotNull { it.getString("url") }
                _availablePhotos.value = photos
            }
            .addOnFailureListener {
                _availablePhotos.value = emptyList() // Handle failure gracefully
            }
    }
}