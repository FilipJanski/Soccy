package com.example.soccy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.clickable


// 🔽 Ekrany nawigacji (dolny pasek)
sealed class Screen(val title: String, val icon: ImageVector, val route: String) {
    data object Home : Screen("Home", Icons.Default.Home, "home")
    data object Players : Screen("Zawodnicy", Icons.Default.Groups, "players")
    data object Profile : Screen("Profil", Icons.Default.Person, "profile")
}

@Composable
fun MainNavigation(role: String, login: String) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController, role) }
            composable(Screen.Players.route) { PlayersScreen(navController, role) }
            composable(Screen.Profile.route) { PlaceholderScreen("Profil: $login") }

            composable("player/{playerId}") { backStackEntry ->
                val playerId = backStackEntry.arguments?.getString("playerId")
                if (playerId != null) PlayerProfileScreen(playerId, role)
            }

            composable("addPlayer") {
                AddPlayerScreen(navController)
            }

            // ✅ ekran dodawania wydarzenia (na razie placeholder)
            composable("addEvent") { AddEventScreen(navController) }
            composable("event/{eventId}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")
                if (eventId != null) {
                    EventDetailsScreen(
                        navController = navController,
                        eventId = eventId,
                        role = role
                    )
                }
            }
            composable("eventEdit/{eventId}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")
                if (eventId != null) {
                    EventEditScreen(
                        navController = navController,
                        eventId = eventId
                    )
                }
            }


        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Home, Screen.Players, Screen.Profile)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // ✅ jak klikniesz daną zakładkę, zawsze wracamy do jej "root"
                        popUpTo(screen.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

// ✅ HomeScreen jest TYLKO ekranem Home — nie ma tu NavHost ani Scaffold
@Composable
fun HomeScreen(navController: NavHostController, role: String) {
    val db = FirebaseFirestore.getInstance()
    val events = remember { mutableStateListOf<Triple<String, String, String>>() } // (id, title, content)


    LaunchedEffect(true) {
        // EVENTS (najnowsze pierwsze)
        db.collection("events")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                events.clear()
                for (doc in result) {
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val content = doc.getString("content") ?: ""
                    events.add(Triple(id, title, content))
                }
            }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ✅ Przycisk tylko dla admina
        if (role == "admin") {
            Button(
                onClick = { navController.navigate("addEvent") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Dodaj wydarzenie")
            }
        }

        Text(
            text = "Aktualności",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (events.isEmpty()) {
            Text(
                text = "Brak wydarzeń.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            events.forEach { (id, title, content) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { navController.navigate("event/$id") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3
                        )
                    }
                }
            }
        }

    }
}

// Placeholder
@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name, color = MaterialTheme.colorScheme.onBackground)
    }
}
