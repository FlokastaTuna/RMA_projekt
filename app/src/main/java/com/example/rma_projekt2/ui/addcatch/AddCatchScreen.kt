package com.example.rma_projekt2.ui.addcatch

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.rma_projekt2.R
import com.example.rma_projekt2.viewmodel.CatchViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddCatchScreen(
    navController: NavHostController,
    viewModel: CatchViewModel = viewModel()
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        LaunchedEffect(Unit) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
        return
    }

    var fishType by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val availablePhotos by viewModel.availablePhotos.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchAvailablePhotos()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = fishType,
            onValueChange = { fishType = it },
            label = { Text("Fish Type") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight (kg)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Select a picture:")

        if (availablePhotos.isEmpty()) {
            Text("No photos available", color = MaterialTheme.colorScheme.error)
        } else {
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(availablePhotos) { url ->
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = url,
                        ),
                        contentDescription = "Fish option",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(4.dp)
                            .clickable { selectedPhotoUrl = url }
                            .border(
                                width = if (selectedPhotoUrl == url) 3.dp else 0.dp,
                                color = if (selectedPhotoUrl == url)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Transparent
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val weightDouble = weight.toDoubleOrNull()
                val photo = selectedPhotoUrl
                val userId = currentUser.uid

                when {
                    fishType.isBlank() -> errorMessage = "Please enter a fish type"
                    weightDouble == null -> errorMessage = "Please enter a valid weight"
                    weightDouble <= 0 -> errorMessage = "Weight must be positive"
                    photo.isNullOrBlank() -> errorMessage = "Please select a photo"
                    else -> {
                        saving = true
                        val catchData = hashMapOf(
                            "fishType" to fishType,
                            "weight" to weightDouble,
                            "photoUrl" to photo,
                            "userID" to userId,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )

                        FirebaseFirestore.getInstance().collection("catches")
                            .add(catchData)
                            .addOnSuccessListener {
                                saving = false
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                saving = false
                                errorMessage = e.message
                            }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving
        ) {
            Text(if (saving) "Saving..." else "Save Catch")
        }
    }
}