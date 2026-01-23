package com.example.soccy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterScreen(navController: NavController) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Rejestracja", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Login") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (login.isNotBlank() && password.isNotBlank()) {
                    val newUser = hashMapOf(
                        "login" to login,
                        "haslo" to password,
                        "rola" to "user"
                    )
                    db.collection("users")
                        .add(newUser)
                        .addOnSuccessListener {
                            message = "Zarejestrowano! Wróć do logowania"
                            navController.popBackStack()  // wraca do LoginScreen
                        }
                        .addOnFailureListener {
                            message = "Błąd podczas rejestracji"
                        }
                } else {
                    message = "Wypełnij wszystkie pola"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zarejestruj się")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(message, color = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            navController.popBackStack()
        }) {
            Text("Powrót")
        }
    }
}
