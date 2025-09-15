package com.example.rma_projekt2.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class User(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = ""
)
class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()


    private val _usersList = MutableStateFlow<List<User>>(emptyList())
    val usersList: StateFlow<List<User>> = _usersList

    // Login function (unchanged)
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.message)
            }
    }
    fun fetchAllUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.map { doc ->
                    User(
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        email = doc.getString("email") ?: ""
                    )
                }
                _usersList.value = users
            }
            .addOnFailureListener {
                _usersList.value = emptyList()
            }
    }
    // Register function
    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        // Step 1: Create user in FirebaseAuth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Step 2: Write user info to Firestore under /users/{uid}
                        val userData = hashMapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "email" to email,
                            "createdAt" to Timestamp.now()
                        )

                        db.collection("users").document(user.uid)
                            .set(userData)
                            .addOnCompleteListener { firestoreTask ->
                                if (firestoreTask.isSuccessful) {
                                    onResult(true, null)
                                } else {
                                    // If Firestore write fails, delete the created FirebaseAuth user
                                    user.delete()
                                    onResult(false, firestoreTask.exception?.message)
                                }
                            }
                    } else {
                        onResult(false, "User is null after registration")
                    }
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

}