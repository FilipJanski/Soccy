package com.example.soccy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EventDetailsScreen(
    navController: NavHostController,
    eventId: String,
    role: String
) {
    val db = FirebaseFirestore.getInstance()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

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
        Text("Szczegóły wydarzenia", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            loading -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Text(content, style = MaterialTheme.typography.bodyLarge)

                Spacer(modifier = Modifier.height(24.dp))


                if (role == "admin") {
                    Button(
                        onClick = { navController.navigate("eventEdit/$eventId") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edytuj")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            db.collection("events").document(eventId).delete()
                                .addOnSuccessListener { navController.popBackStack() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Usuń")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Powrót")
                }
            }
        }
    }
}
