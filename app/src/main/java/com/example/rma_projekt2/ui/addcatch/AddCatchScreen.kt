package com.example.rma_projekt2.ui.addcatch

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.rma_projekt2.ui.notification.NotificationEvent
import com.example.rma_projekt2.viewmodel.CatchViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*

@Composable
fun AddCatchScreen(
    navController: NavHostController,
    viewModel: CatchViewModel = viewModel()
) {
    val context = LocalContext.current
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
    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Photos
    val availablePhotos by viewModel.availablePhotos.collectAsState()
    LaunchedEffect(Unit) { viewModel.fetchAvailablePhotos() }

    // Map
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.8150, 15.9819), 6f)
    }
    var isLocationEnabled by remember { mutableStateOf(false) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Launcher for location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                isLocationEnabled = true
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        markerPosition = userLatLng
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(userLatLng, 12f)
                    }
                }
            }
        }
    )

    // Request/check permission when screen opens
    LaunchedEffect(Unit) {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            isLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    markerPosition = userLatLng
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(userLatLng, 12f)
                }
            }
        } else {
            locationPermissionLauncher.launch(permission)
        }
    }

    Column (modifier = Modifier.fillMaxSize().
            padding(horizontal = 16.dp,vertical = 32.dp)) {
        Text(
            text = "Add fish",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(12.dp))
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
                        painter = rememberAsyncImagePainter(url),
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

        Text("Tap on the map to set location:")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            GoogleMap(
                modifier = Modifier.matchParentSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = isLocationEnabled),
                onMapClick = { latLng ->
                    markerPosition = latLng
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(latLng, 12f)
                }
            ) {
                markerPosition?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Catch Location"
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
                    markerPosition == null -> errorMessage = "Please select a location on the map"
                    else -> {
                        saving = true
                        val catchData = hashMapOf(
                            "fishType" to fishType,
                            "weight" to weightDouble,
                            "photoUrl" to photo,
                            "userID" to userId,
                            "createdAt" to com.google.firebase.Timestamp.now(),
                            "latitude" to markerPosition!!.latitude,
                            "longitude" to markerPosition!!.longitude
                        )

                        FirebaseFirestore.getInstance().collection("catches")
                            .add(catchData)
                            .addOnSuccessListener {
                                saving = false
                                NotificationEvent.showCatchAdded(context)
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
