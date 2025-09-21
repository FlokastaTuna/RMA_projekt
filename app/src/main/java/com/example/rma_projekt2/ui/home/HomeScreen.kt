package com.example.rma_projekt2.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
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
    var menuExpanded by remember { mutableStateOf(false) }

    var filterExpanded by remember { mutableStateOf(false) } //za sortiranje dropdown
    var selectedField by remember { mutableStateOf("None") } //
    var ascending by remember { mutableStateOf(true) } //za sortiranje
    val catches by viewModel.catches.collectAsState() // uzima listu iz viewmodela
    var loading by remember { mutableStateOf(true) } //
    var error by remember { mutableStateOf<String?>(null) } //za error poruku

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
            "Fish" -> catches.sortedBy { it.fishType.lowercase() }
            "Weight" -> catches.sortedBy { it.weight }
            "Date" -> catches.sortedBy { it.createdAt }
            else -> catches
        }
        if (!ascending) sorted.reversed() else sorted
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp)) {

        // gornji dio, izbornik
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.headlineMedium
            )

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Catch") },
                        onClick = {
                            menuExpanded = false
                            navController.navigate("addCatch")
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(userName) },
                        onClick = {} // samo pokazi usera
                    )
                    DropdownMenuItem(
                        text = { Text("Logout") },
                        onClick = {
                            FirebaseAuth.getInstance().signOut() //log out
                            menuExpanded = false
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // glavni dio
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // sortiranje
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
                        listOf("None", "Fish", "Weight", "Date").forEach { field ->
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

            // prikazi listu ako je ima, ukoliko ne tekst koji kaze da nema nista
            if (filteredCatches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No catches yet, start fishing!", color = Color.White)
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

//kompozable za prikaz dodanih riba na homescreenu
@Composable
fun CatchItem(catch: Catch, navController: NavHostController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (catch.id.isNotEmpty()) {
                    navController.navigate("catchDetail/${catch.id}")
                } else {
                    println("Invalid catch ID: ${catch.id}")
                }
            },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (catch.photoUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(catch.photoUrl),
                    contentDescription = "Fish photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium),
                    alignment = Alignment.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = catch.fishType,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "️Weight: ${catch.weight} kg",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
