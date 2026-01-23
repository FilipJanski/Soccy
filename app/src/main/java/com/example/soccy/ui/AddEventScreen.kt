package com.example.soccy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddEventScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Dodaj wydarzenie", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

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
                .height(160.dp)
        )

        error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                error = null

                if (title.isBlank() || content.isBlank()) {
                    error = "Uzupełnij tytuł i treść."
                    return@Button
                }

                saving = true

                val event = hashMapOf(
                    "title" to title.trim(),
                    "content" to content.trim(),
                    "createdAt" to Timestamp.now()
                    // "createdBy" -> możesz dodać później (np. login admina)
                )

                db.collection("events")
                    .add(event)
                    .addOnSuccessListener {
                        saving = false
                        navController.popBackStack() // wróć na Home
                    }
                    .addOnFailureListener { e ->
                        saving = false
                        error = "Błąd zapisu: ${e.message}"
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving
        ) {
            Text(if (saving) "Zapisywanie..." else "Dodaj")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Powrót")
        }
    }
}
