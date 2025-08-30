package com.example.soccy.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.imePadding
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddPlayerScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
    ) {
        item {
            Text("Dodaj zawodnika", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item { Input("Imię", firstName) { firstName = it } }
        item { Input("Nazwisko", lastName) { lastName = it } }
        item { Input("Klub", club) { club = it } }
        item { Input("Pozycja", position) { position = it } }
        item { Input("Wzrost (cm)", height) { height = it } }
        item { Input("Data urodzenia (rrrr-mm-dd)", birthDate) { birthDate = it } }
        item { Input("Dominująca noga", dominantFoot) { dominantFoot = it } }
        item { Input("Kraj", country) { country = it } }
        item { Input("Numer na koszulce", jerseyNumber) { jerseyNumber = it } }
        item { Input("Gole", goals) { goals = it } }
        item { Input("Mecze", matches) { matches = it } }
        item { Input("Żółte kartki", yellowCards) { yellowCards = it } }
        item { Input("Czerwone kartki", redCards) { redCards = it } }
        item { Input("Asysty", assists) { assists = it } }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val player = hashMapOf(
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

                    if (firstName.isNotBlank() && lastName.isNotBlank()) {
                        db.collection("players")
                            .add(player)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Zawodnik dodany", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
                                e.printStackTrace()
                            }
                    } else {
                        Toast.makeText(context, "Uzupełnij imię i nazwisko", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dodaj zawodnika")
            }
        }
    }
}

@Composable
fun Input(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true
    )
}
