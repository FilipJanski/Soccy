package com.example.soccy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(
    userId: String,
    onLogout: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    var showChangeLogin by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                login = doc.getString("login") ?: ""
                password = doc.getString("haslo") ?: ""
                loading = false
            }
            .addOnFailureListener {
                message = "Nie udało się pobrać danych profilu"
                loading = false
            }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Profil",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            if (loading) {
                CircularProgressIndicator()
                return@Column
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Login: $login", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Hasło: $password", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showChangeLogin = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Zmień login") }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = { showChangePassword = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Zmień hasło") }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Wyloguj")
            }

            message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Color.Red)
            }
        }
    }

    if (showChangeLogin) {
        ChangeLoginDialog(
            currentLogin = login,
            onDismiss = { showChangeLogin = false },
            onConfirm = { newLogin ->
                message = null
                val trimmed = newLogin.trim()

                if (trimmed.isEmpty()) {
                    message = "Login nie może być pusty"
                    return@ChangeLoginDialog
                }

                db.collection("users")
                    .whereEqualTo("login", trimmed)
                    .get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            message = "Taki login już istnieje"
                        } else {
                            db.collection("users").document(userId)
                                .update("login", trimmed)
                                .addOnSuccessListener {
                                    login = trimmed
                                    message = "Login zmieniony"
                                    showChangeLogin = false
                                }
                                .addOnFailureListener {
                                    message = "Nie udało się zmienić loginu"
                                }
                        }
                    }
                    .addOnFailureListener {
                        message = "Błąd podczas sprawdzania loginu"
                    }
            }
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onConfirm = { newPass ->
                message = null
                val trimmed = newPass.trim()

                if (trimmed.length < 3) {
                    message = "Hasło jest za krótkie"
                    return@ChangePasswordDialog
                }

                db.collection("users").document(userId)
                    .update("haslo", trimmed)
                    .addOnSuccessListener {
                        password = trimmed
                        message = "Hasło zmienione"
                        showChangePassword = false
                    }
                    .addOnFailureListener {
                        message = "Nie udało się zmienić hasła"
                    }
            }
        )
    }
}

@Composable
private fun ChangeLoginDialog(
    currentLogin: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newLogin by remember { mutableStateOf(currentLogin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zmień login") },
        text = {
            OutlinedTextField(
                value = newLogin,
                onValueChange = { newLogin = it },
                singleLine = true,
                label = { Text("Nowy login") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(newLogin) }) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zmień hasło") },
        text = {
            OutlinedTextField(
                value = newPass,
                onValueChange = { newPass = it },
                singleLine = true,
                label = { Text("Nowe hasło") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(newPass) }) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}