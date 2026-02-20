package com.example.soccy.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EditPlayerScreen(
    navController: NavHostController,
    playerId: String
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    //  stany pól
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var club by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var dominantFoot by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var jerseyNumber by remember { mutableStateOf("") }
    var goals by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf("") }
    var yellowCards by remember { mutableStateOf("") }
    var redCards by remember { mutableStateOf("") }
    var assists by remember { mutableStateOf("") }

    //  wczytanie danych
    LaunchedEffect(playerId) {
        db.collection("players").document(playerId).get()
            .addOnSuccessListener { doc ->
                firstName = doc.getString("firstName") ?: ""
                lastName = doc.getString("lastName") ?: ""
                club = doc.getString("club") ?: ""
                position = doc.getString("position") ?: ""
                height = doc.getLong("height")?.toString() ?: ""
                birthDate = doc.getString("birthDate") ?: ""
                dominantFoot = doc.getString("dominantFoot") ?: ""
                country = doc.getString("country") ?: ""
                jerseyNumber = doc.getLong("jerseyNumber")?.toString() ?: ""
                goals = doc.getLong("goals")?.toString() ?: ""
                matches = doc.getLong("matches")?.toString() ?: ""
                yellowCards = doc.getLong("yellowCards")?.toString() ?: ""
                redCards = doc.getLong("redCards")?.toString() ?: ""
                assists = doc.getLong("assists")?.toString() ?: ""
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        item {
            Text("Edytuj zawodnika", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
        }

        item { Input("Imię", firstName) { firstName = it } }
        item { Input("Nazwisko", lastName) { lastName = it } }
        item { Input("Klub", club) { club = it } }
        item { Input("Pozycja", position) { position = it } }
        item { Input("Wzrost (cm)", height) { height = it } }
        item { Input("Data urodzenia", birthDate) { birthDate = it } }
        item { Input("Dominująca noga", dominantFoot) { dominantFoot = it } }
        item { Input("Kraj", country) { country = it } }
        item { Input("Numer koszulki", jerseyNumber) { jerseyNumber = it } }
        item { Input("Gole", goals) { goals = it } }
        item { Input("Mecze", matches) { matches = it } }
        item { Input("Żółte kartki", yellowCards) { yellowCards = it } }
        item { Input("Czerwone kartki", redCards) { redCards = it } }
        item { Input("Asysty", assists) { assists = it } }

        item {
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val update = mapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "club" to club,
                        "position" to position,
                        "height" to height.toIntOrNull(),
                        "birthDate" to birthDate,
                        "dominantFoot" to dominantFoot,
                        "country" to country,
                        "jerseyNumber" to jerseyNumber.toIntOrNull(),
                        "goals" to goals.toIntOrNull(),
                        "matches" to matches.toIntOrNull(),
                        "yellowCards" to yellowCards.toIntOrNull(),
                        "redCards" to redCards.toIntOrNull(),
                        "assists" to assists.toIntOrNull()
                    )

                    db.collection("players")
                        .document(playerId)
                        .update(update)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zapisz zmiany")
            }
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Powrót")
            }
        }
    }
}
