package com.example.soccy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.soccy.model.Player


// 🔹 Ekran profilu gracza
@Composable
fun PlayerProfileScreen(playerId: String?) {
    var player by remember { mutableStateOf<Player?>(null) }
    val db = FirebaseFirestore.getInstance()

    // Dodane — zapewnia aktualny playerId w LaunchedEffect
    val currentPlayerId by rememberUpdatedState(playerId)

    LaunchedEffect(currentPlayerId) {
        currentPlayerId?.let { id ->
            db.collection("players").document(id).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        player = Player(
                            id = document.id,
                            firstName = document.getString("firstName") ?: "",
                            lastName = document.getString("lastName") ?: "",
                            birthDate = document.getString("birthDate") ?: "",
                            height = document.getLong("height")?.toInt() ?: 0,
                            jerseyNumber = document.getLong("jerseyNumber")?.toInt() ?: 0,
                            dominantFoot = document.getString("dominantFoot") ?: "",
                            country = document.getString("country") ?: "",
                            club = document.getString("club") ?: "",
                            position = document.getString("position") ?: "",
                            goals = document.getLong("goals")?.toInt() ?: 0,
                            matches = document.getLong("matches")?.toInt() ?: 0,
                            yellowCards = document.getLong("yellowCards")?.toInt() ?: 0,
                            redCards = document.getLong("redCards")?.toInt() ?: 0,
                            assists = document.getLong("assists")?.toInt() ?: 0
                        )
                    }
                }
        }
    }

    player?.let {
        PlayerProfileContent(it)
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// 🔹 UI profilu gracza
@Composable
fun PlayerProfileContent(player: Player) {
    var selectedTab by remember { mutableStateOf("info") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Górny blok: zdjęcie + dane
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Gray, shape = CircleShape)
            ) {
                // Tu możesz dodać zdjęcie w przyszłości
            }

            Spacer(modifier = Modifier.width(16.dp))

            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = player.firstName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = player.lastName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Przełączniki: Informacje / Statystyki
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val buttonModifier = Modifier
                .weight(1f)
                .fillMaxHeight()

            Button(
                onClick = { selectedTab = "info" },
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "info") MaterialTheme.colorScheme.primary else Color.LightGray,
                    contentColor = if (selectedTab == "info") Color.White else Color.Black
                ),
                shape = RoundedCornerShape(0.dp)

            ) {
                Text("Informacje ogólne")
            }

            Button(
                onClick = { selectedTab = "stats" },
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "stats") MaterialTheme.colorScheme.primary else Color.LightGray,
                    contentColor = if (selectedTab == "stats") Color.White else Color.Black
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("Statystyki")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Zawartość zakładek
        if (selectedTab == "info") {
            Column {
                Text("Imię: ${player.firstName}")
                Text("Nazwisko: ${player.lastName}")
                Text("Wzrost: ${player.height} cm")
                Text("Data urodzenia: ${player.birthDate}")
                Text("Numer koszulki: ${player.jerseyNumber}")
                Text("Dominująca noga: ${player.dominantFoot}")
                Text("Kraj: ${player.country}")
                Text("Klub: ${player.club}")
                Text("Pozycja: ${player.position}")
            }
        } else {
            Column {
                Text("Gole: ${player.goals}")
                Text("Rozegrane mecze: ${player.matches}")
                Text("Asysty: ${player.assists}")
                Text("Żółte kartki: ${player.yellowCards}")
                Text("Czerwone kartki: ${player.redCards}")
            }
        }
    }
}
