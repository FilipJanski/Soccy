package com.example.soccy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rejestracja",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Login") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Hasło") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Powtórz hasło") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        login.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                            errorMessage = "Wszystkie pola są wymagane"
                            successMessage = null
                        }

                        password != confirmPassword -> {
                            errorMessage = "Hasła się nie zgadzają"
                            successMessage = null
                        }

                        else -> {
                            // 🔍 SPRAWDZENIE CZY LOGIN JUŻ ISTNIEJE
                            db.collection("users")
                                .whereEqualTo("login", login)
                                .get()
                                .addOnSuccessListener { result ->
                                    if (!result.isEmpty) {
                                        // ❌ login już istnieje
                                        errorMessage = "Użytkownik o takim loginie już istnieje"
                                        successMessage = null
                                    } else {
                                        // ✅ można dodać nowego użytkownika
                                        db.collection("users")
                                            .add(
                                                mapOf(
                                                    "login" to login,
                                                    "haslo" to password,
                                                    "rola" to "user"
                                                )
                                            )
                                            .addOnSuccessListener {
                                                successMessage = "Rejestracja zakończona sukcesem"
                                                errorMessage = null

                                                // wróć do logowania
                                                navController.popBackStack()
                                            }
                                            .addOnFailureListener {
                                                errorMessage = "Błąd podczas rejestracji"
                                                successMessage = null
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    errorMessage = "Błąd połączenia z bazą"
                                    successMessage = null
                                }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Zarejestruj się")
            }


            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = Color.Red)
            }

            successMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = Color.Green)
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { navController.popBackStack() }) {
                Text("Powrót do logowania")
            }
        }
    }
}
