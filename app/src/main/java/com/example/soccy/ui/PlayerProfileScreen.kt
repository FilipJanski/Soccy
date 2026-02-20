package com.example.soccy.ui


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.soccy.model.Player
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File


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
    var scanRefreshKey by remember { mutableIntStateOf(0) }
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
                                // WRACAMY DO LISTY ZAWODNIKÓW
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
                val activity = LocalContext.current.findActivity()
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {

                    scanRefreshKey++
                }

                val context = LocalContext.current
                val scansDir = File(context.getExternalFilesDir(null), "scans")
                val plyFile = remember(player.id, scanRefreshKey) {
                    File(scansDir, "${player.id}.ply")
                }
                val hasPly = remember(plyFile.absolutePath, scanRefreshKey) { plyFile.exists() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (hasPly) {
                            PlyPointCloudViewer(
                                plyFile = plyFile,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Brak modelu 3D")
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (role == "admin") {
                        Button(
                            onClick = {
                                val act = activity ?: return@Button
                                val intent = android.content.Intent(act, com.example.threedscanner.MainActivity::class.java)
                                intent.putExtra("scan_tag", player.id)
                                launcher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (hasPly) "Nowy skan 3D" else "Zrób skan 3D")
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

        file.toURI().toString()
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



fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
