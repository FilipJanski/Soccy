package com.example.soccy.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.soccy.model.Player
import com.google.firebase.firestore.FirebaseFirestore
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.core.net.toUri
import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.Dp
import java.io.File
import androidx.compose.ui.unit.DpOffset




@Composable
fun PlayerProfileScreen(playerId: String?, role: String, navController: NavHostController)
 {
    var player by remember { mutableStateOf<Player?>(null) }
    val db = FirebaseFirestore.getInstance()
    val currentPlayerId by rememberUpdatedState(playerId)

     DisposableEffect(currentPlayerId) {
         val id = currentPlayerId ?: return@DisposableEffect onDispose {}

         val listener = db.collection("players")
             .document(id)
             .addSnapshotListener { document, _ ->
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
                         assists = document.getLong("assists")?.toInt() ?: 0,
                         photoUri = document.getString("photoUri")
                     )
                 }
             }

         onDispose { listener.remove() }
     }



     player?.let {
        PlayerProfileContent(it, role, navController)
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun PlayerProfileContent(player: Player, role: String, navController: NavHostController)
{
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val localPath = copyImageToAppStorage(
                context = context,
                sourceUri = uri,
                playerId = player.id
            )

            if (localPath != null) {
                db.collection("players")
                    .document(player.id)
                    .update("photoUri", localPath)
            }
        }
    }
     var selectedTab by remember { mutableStateOf("info") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        var menuExpanded by remember { mutableStateOf(false) }

        if (role == "admin") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(end = 10.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant

                )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                offset = DpOffset(x = (205).dp, y = (140).dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Edytuj zawodnika") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("editPlayer/${player.id}")
                    }
                )

                DropdownMenuItem(
                    text = { Text("Zmień zdjęcie") },
                    onClick = {
                        menuExpanded = false
                        imagePicker.launch("image/*")
                    }
                )

                DropdownMenuItem(
                    text = { Text("Usuń zdjęcie") },
                    onClick = {
                        menuExpanded = false
                        player.photoUri.let {
                            val file = File(it?.toUri()?.path ?: "")
                            if (file.exists()) file.delete()
                        }

                        db.collection("players")
                            .document(player.id)
                            .update("photoUri", null)
                    }
                )

                HorizontalDivider()

                DropdownMenuItem(
                    text = { Text("Usuń zawodnika", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuExpanded = false
                        FirebaseFirestore.getInstance()
                            .collection("players")
                            .document(player.id)
                            .delete()
                            .addOnSuccessListener {
                                // 🔥 WRACAMY DO LISTY ZAWODNIKÓW
                                navController.popBackStack(
                                    route = "players",
                                    inclusive = false
                                )
                            }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!player.photoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = player.photoUri.toUri(),
                    contentDescription = "Zdjęcie zawodnika",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                PlayerAvatar(player = player)
            }


            Spacer(modifier = Modifier.width(16.dp))

            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(player.firstName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(player.lastName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }



        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val tabItems = listOf(
                "Informacje ogólne" to "info",
                "Statystyki" to "stats",
                "Model 3D" to "model"
            )

            tabItems.forEach { (label, key) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (selectedTab == key) MaterialTheme.colorScheme.primary else Color.LightGray)
                        .clickable { selectedTab = key },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selectedTab == key) Color.White else Color.Black,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            "info" -> {
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
            }
            "stats" -> {
                Column {
                    Text("Gole: ${player.goals}")
                    Text("Rozegrane mecze: ${player.matches}")
                    Text("Asysty: ${player.assists}")
                    Text("Żółte kartki: ${player.yellowCards}")
                    Text("Czerwone kartki: ${player.redCards}")
                }
            }

            "model" -> {
                val engine = rememberEngine()
                val modelLoader = rememberModelLoader(engine)
                val context = LocalContext.current

                val modelFileName = "player_model_${player.id}.glb"
                val fileExists = remember {
                    try {
                        context.assets.open(modelFileName).close()
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                Column {
                    if (fileExists) {
                        val node = ModelNode(modelLoader.createModelInstance(modelFileName)).apply {
                            position = Position(0f, -15f, 0f)
                        }

                        Scene(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp),
                            engine = engine,
                            view = rememberView(engine),
                            renderer = rememberRenderer(engine),
                            scene = rememberScene(engine),
                            cameraNode = rememberCameraNode(engine) {
                                position = Position(z = 25.0f)
                            },
                            modelLoader = modelLoader,
                            childNodes = listOf(node),
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        if (role == "admin") {
                            Button(
                                onClick = { /* TODO: Akcja dla nowego skanu */ },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Nowy skan 3D")
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Brak modelu 3D")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        if (role == "admin") {
                            Button(
                                onClick = { /* TODO: Akcja dla pierwszego skanu */ },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Zrób skan 3D")
                            }
                        }
                    }
                }
            }






        }
    }
}

@Composable
fun PlayerAvatar(
    player: Player,
    size: Dp = 80.dp
) {
    if (!player.photoUri.isNullOrEmpty()) {
        AsyncImage(
            model = player.photoUri.toUri(),
            contentDescription = "Zdjęcie zawodnika",
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getInitials(player.firstName, player.lastName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


fun copyImageToAppStorage(
    context: Context,
    sourceUri: Uri,
    playerId: String
): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: return null

        val file = File(context.filesDir, "player_$playerId.jpg")

        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        file.toURI().toString() // 👉 ZWRACAMY file://...
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getInitials(firstName: String, lastName: String): String {
    val first = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: ""
    val last = lastName.firstOrNull()?.uppercaseChar()?.toString() ?: ""
    return (first + last).ifEmpty { "?" }
}

