package com.example.rma_projekt2.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.rma_projekt2.R
import com.example.rma_projekt2.viewmodel.Catch
import com.example.rma_projekt2.viewmodel.CatchViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(
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

    val userName = currentUser.displayName ?: currentUser.email ?: "User"
    var logoutExpanded by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }
    var selectedField by remember { mutableStateOf("None") }
    var ascending by remember { mutableStateOf(true) }
    val catches by viewModel.catches.collectAsState()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser) {
        try {
            currentUser.uid.let { uid ->
                viewModel.fetchCatches(uid)
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    val filteredCatches = remember(catches, selectedField, ascending) {
        val sorted = when (selectedField) {
            "FishType" -> catches.sortedBy { it.fishType.lowercase() }
            "Weight" -> catches.sortedBy { it.weight }
            "Date" -> catches.sortedBy { it.createdAt }
            else -> catches
        }
        if (!ascending) sorted.reversed() else sorted
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Box {
                Text(
                    text = "Hi, $userName",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.clickable { logoutExpanded = true }
                )
                DropdownMenu(
                    expanded = logoutExpanded,
                    onDismissRequest = { logoutExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Logout") },
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            logoutExpanded = false
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    Button(onClick = { filterExpanded = true }) {
                        Text("Filter: $selectedField ${if (selectedField != "None") if (ascending) "↑" else "↓" else ""}")
                    }
                    DropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        listOf("None", "FishType", "Weight", "Date").forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field) },
                                onClick = {
                                    selectedField = field
                                    ascending = true
                                    filterExpanded = false
                                }
                            )
                        }
                        if (selectedField != "None") {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (ascending) "Ascending" else "Descending") },
                                onClick = { ascending = !ascending }
                            )
                        }
                    }
                }

                if (selectedField != "None") {
                    Button(onClick = {
                        selectedField = "None"
                        ascending = true
                    }) {
                        Text("Clear Filter")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("addCatch") },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Add Catch")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredCatches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No pictures added yet")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredCatches) { catch ->
                        CatchItem(catch, navController)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CatchItem(catch: Catch, navController: NavHostController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (catch.id.isNotEmpty()) {
                    navController.navigate("catchDetail/${catch.id}")
                } else {
                    println("Invalid catch ID: ${catch.id}") // Debug log
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (catch.photoUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = catch.photoUrl,
                    ),
                    contentDescription = "Fish photo",
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(catch.fishType, style = MaterialTheme.typography.bodyLarge)
            Text("Weight: ${catch.weight} kg", style = MaterialTheme.typography.bodyMedium)
        }
    }
}