package com.example.soccy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun PlayersScreen(navController: NavController, role: String) {
    val db = FirebaseFirestore.getInstance()
    var allPlayers by remember { mutableStateOf(listOf<Player>()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(true) {
        db.collection("players")
            .orderBy("lastName")
            .get()
            .addOnSuccessListener { result ->
                val players = result.map { doc ->
                    Player(
                        id = doc.id,
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        club = doc.getString("club") ?: ""
                    )
                }
                allPlayers = players
            }
    }


    val filteredPlayers = if (searchQuery.isBlank()) {
        allPlayers
    } else {
        allPlayers.filter { player ->
            val query = searchQuery.lowercase()
            player.firstName.lowercase().contains(query) ||
                    player.lastName.lowercase().contains(query) ||
                    player.club.lowercase().contains(query)
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        if (role == "admin") {
            Button(
                onClick = { navController.navigate("addPlayer") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Dodaj zawodnika")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Szukaj zawodnika") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        LazyColumn {
            items(filteredPlayers) { player ->
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
