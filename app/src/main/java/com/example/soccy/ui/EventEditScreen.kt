package com.example.soccy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EventEditScreen(
    navController: NavHostController,
    eventId: String
) {
    val db = FirebaseFirestore.getInstance()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // wczytaj dane wydarzenia
    LaunchedEffect(eventId) {
        db.collection("events").document(eventId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    title = doc.getString("title") ?: ""
                    content = doc.getString("content") ?: ""
                } else {
                    error = "Nie znaleziono wydarzenia."
                }
                loading = false
            }
            .addOnFailureListener { e ->
                error = "Błąd pobierania: ${e.message}"
                loading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Edytuj wydarzenie", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            loading -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Powrót")
                }
            }
            else -> {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Treść") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        error = null
                        if (title.isBlank() || content.isBlank()) {
                            error = "Uzupełnij tytuł i treść."
                            return@Button
                        }

                        saving = true

                        db.collection("events").document(eventId)
                            .update(
                                mapOf(
                                    "title" to title.trim(),
                                    "content" to content.trim()
                                )
                            )
                            .addOnSuccessListener {
                                saving = false
                                // wróć do szczegółów (czyli poprzedniego ekranu)
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                saving = false
                                error = "Błąd zapisu: ${e.message}"
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving
                ) {
                    Text(if (saving) "Zapisywanie..." else "Zapisz zmiany")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Anuluj")
                }

                error?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
