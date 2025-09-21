package com.example.rma_projekt2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rma_projekt2.ui.auth.LoginScreen
import com.example.rma_projekt2.ui.auth.RegisterScreen
import com.example.rma_projekt2.ui.theme.RMA_projekt2Theme
import com.example.rma_projekt2.ui.home.HomeScreen
import com.example.rma_projekt2.ui.addcatch.AddCatchScreen
import com.example.rma_projekt2.ui.detailscreen.CatchDetailScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAuth.getInstance().signOut()
        enableEdgeToEdge()

        setContent {
            RMA_projekt2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val startDestination = if (currentUser == null) "login" else "home"
                    AppNavHost(navController, startDestination)
                }
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("login") { LoginScreen(navController = navController) }
        composable("register") { RegisterScreen(navController = navController) }
        composable("home") { HomeScreen(navController = navController) }
        composable("addCatch") { AddCatchScreen(navController = navController) }
        composable("catchDetail/{catchId}") { backStackEntry ->
            val catchId = backStackEntry.arguments?.getString("catchId") ?: ""
            CatchDetailScreen(navController = navController, catchId = catchId)
        }
    }
}
