package com.example.soccy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavController


@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: (String, String, String) -> Unit // role, login, userId
) {

    val db = FirebaseFirestore.getInstance()
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
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
                text = "Logowanie",
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
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    db.collection("users")
                        .whereEqualTo("login", login)
                        .whereEqualTo("haslo", password)
                        .get()
                        .addOnSuccessListener { result ->
                            val user = result.documents.firstOrNull()
                            if (user != null) {
                                val role = user.getString("rola") ?: "user"
                                val login = user.getString("login") ?: ""
                                val userId = user.id
                                onLoginSuccess(role, login, userId)
                            } else {
                                errorMessage = "Nieprawidłowy login lub hasło"
                            }
                        }
                        .addOnFailureListener {
                            errorMessage = "Błąd połączenia z bazą"
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Zaloguj się")
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = Color.Red)
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = {
                navController.navigate("register")
            }) {
                Text("Nie masz konta? Zarejestruj się")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "login: admin\nhasło: admin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )


        }
    }
}
