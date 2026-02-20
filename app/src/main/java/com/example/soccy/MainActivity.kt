package com.example.soccy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.soccy.ui.LoginScreen
import com.example.soccy.ui.MainNavigation
import com.example.soccy.ui.RegisterScreen
import com.example.soccy.ui.theme.SoccyTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            SoccyTheme {
                val navController = rememberNavController()


                var role by rememberSaveable { mutableStateOf("") }
                var login by rememberSaveable { mutableStateOf("") }
                var userId by rememberSaveable { mutableStateOf("") }

                NavHost(
                    navController = navController,
                    startDestination = if (userId.isBlank()) "login" else "main"
                ) {
                    composable("login") {
                        LoginScreen(
                            navController = navController,
                            onLoginSuccess = { loggedRole, loggedLogin, loggedUserId ->
                                role = loggedRole
                                login = loggedLogin
                                userId = loggedUserId

                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable("register") {
                        RegisterScreen(navController)
                    }

                    composable("main") {
                        MainNavigation(
                            role = role,
                            login = login,
                            userId = userId,
                            onLogout = {
                                role = ""
                                login = ""
                                userId = ""

                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}