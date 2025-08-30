package com.example.soccy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

data class Player(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val club: String = ""
)

@Composable
fun SearchScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<Player>() }
    val db = FirebaseFirestore.getInstance()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Wpisz imię, nazwisko lub klub") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (query.isNotBlank()) {
                    db.collection("players")
                        .get()
                        .addOnSuccessListener { result ->
                            results.clear()
                            for (doc in result) {
                                val firstName = doc.getString("firstName") ?: ""
                                val lastName = doc.getString("lastName") ?: ""
                                val club = doc.getString("club") ?: ""
                                if (firstName.contains(query, ignoreCase = true) ||
                                    lastName.contains(query, ignoreCase = true) ||
                                    club.contains(query, ignoreCase = true)) {
                                    results.add(Player(doc.id, firstName, lastName, club))
                                }
                            }
                        }
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Szukaj")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(results) { player ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            navController.navigate("player/${player.id}")
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${player.firstName} ${player.lastName}")
                        Text("Klub: ${player.club}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
