package com.example.soccy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.*
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

                // Stan zalogowanego użytkownika
                var role by remember { mutableStateOf("") }
                var login by remember { mutableStateOf("") }

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {

                    // 🔐 LOGOWANIE
                    composable("login") {
                        LoginScreen(
                            navController = navController,
                            onLoginSuccess = { loggedRole, loggedLogin ->
                                role = loggedRole
                                login = loggedLogin

                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 📝 REJESTRACJA
                    composable("register") {
                        RegisterScreen(navController)
                    }

                    // 🏠 GŁÓWNA APLIKACJA
                    composable("main") {
                        MainNavigation(
                            role = role,
                            login = login
                        )
                    }
                }
            }
        }
    }
}
