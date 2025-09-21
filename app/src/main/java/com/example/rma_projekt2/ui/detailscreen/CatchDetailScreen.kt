package com.example.rma_projekt2.ui.detailscreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.rma_projekt2.R
import com.example.rma_projekt2.viewmodel.Catch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CatchDetailScreen(
    navController: NavHostController,
    catchId: String
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

    var catch by remember { mutableStateOf<Catch?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(catchId) {
        if (catchId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("catches").document(catchId).get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        catch = Catch(
                            id = doc.id,
                            fishType = doc.getString("fishType") ?: "",
                            weight = doc.getDouble("weight") ?: 0.0,
                            photoUrl = doc.getString("photoUrl") ?: "",
                            userID = doc.getString("userID") ?: "",
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            latitude = doc.getDouble("latitude"),
                            longitude = doc.getDouble("longitude")
                        )
                    } else {
                        error = "Catch not found"
                    }
                    loading = false
                }
                .addOnFailureListener { e ->
                    error = e.message
                    loading = false
                }
        } else {
            error = "Invalid catch ID"
            loading = false
        }
    }

    when {
        loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
            }
        }
        catch != null -> {
            val c = catch!!
            Column (modifier = Modifier.fillMaxSize().
                    padding(horizontal = 16.dp,
                    vertical = 32.dp)) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (c.photoUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = c.photoUrl,
                        ),
                        contentDescription = "Fish photo",
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(c.fishType, style = MaterialTheme.typography.headlineMedium)
                Text("Weight: ${c.weight} kg", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Date: ${
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(c.createdAt))
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (c.latitude != null && c.longitude != null) {
                    val context = LocalContext.current
                    val lat = c.latitude
                    val lng = c.longitude

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Open catch location in Google Maps",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable {
                            val uri = Uri.Builder()
                                .scheme("geo")
                                .path("0,0")
                                .appendQueryParameter(
                                    "q",
                                    String.format(java.util.Locale.US, "%.6f,%.6f(Catch)", lat, lng)
                                )
                                .build()

                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (currentUser.uid == c.userID) {
                    Button(
                        onClick = {
                            FirebaseFirestore.getInstance().collection("catches")
                                .document(c.id)
                                .delete()
                                .addOnSuccessListener { navController.popBackStack() }
                                .addOnFailureListener { e -> error = e.message }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Catch", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}