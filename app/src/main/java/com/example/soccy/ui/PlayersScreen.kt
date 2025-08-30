package com.example.soccy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun PlayersScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val players = remember { mutableStateListOf<Player>() }

    LaunchedEffect(true) {
        db.collection("players")
            .orderBy("lastName") // ← sortowanie alfabetycznie
            .get()
            .addOnSuccessListener { result ->
                players.clear()
                for (doc in result) {
                    val id = doc.id
                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val club = doc.getString("club") ?: ""
                    players.add(Player(id, firstName, lastName, club))
                }
            }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text(
            text = "Lista zawodników",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            items(players) { player ->
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
