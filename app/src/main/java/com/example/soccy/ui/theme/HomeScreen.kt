import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen() {
    val db = FirebaseFirestore.getInstance()
    val matches = remember { mutableStateListOf<String>() }

    // Pobieranie danych z Firestore
    LaunchedEffect(Unit) {
        db.collection("matches").get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val team1 = doc.getString("team1") ?: ""
                    val team2 = doc.getString("team2") ?: ""
                    val score = doc.getString("Score") ?: ""
                    matches.add("$team1 $score $team2")
                }
            }
            .addOnFailureListener {
                matches.add("Błąd podczas pobierania danych")
            }
    }

    // Wyświetlanie danych w liście
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        items(matches) { match ->
            Text(
                text = match,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }

}
