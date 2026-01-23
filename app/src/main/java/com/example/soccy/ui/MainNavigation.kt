package com.example.soccy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.firebase.firestore.FirebaseFirestore

// 🔽 Ekrany nawigacji
sealed class Screen(val title: String, val icon: ImageVector) {
    data object Home : Screen("HomePage", Icons.Default.Home)
    data object Profile : Screen("Profile", Icons.Default.Person)

    data object Players : Screen("Zawodnicy", Icons.Default.Groups)

}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController) }
            composable("players") {
                PlayersScreen(navController)
            }
            composable("profile") { PlaceholderScreen("Profile") }
            composable("player/{playerId}") { backStackEntry ->
                val playerId = backStackEntry.arguments?.getString("playerId")
                playerId?.let {
                    PlayerProfileScreen(it)
                }
            }
            composable("addPlayer") { AddPlayerScreen() }
        }
    }
}

// 🔽 Pasek dolnej nawigacji
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val items = listOf(
            Screen.Home to "home",
            Screen.Players to "players",
            Screen.Profile to "profile"
        )

        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        items.forEach { (screen, route) ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo("home") {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = true
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

// 🔽 Ekran główny z przyciskiem testowym
@Composable
fun HomeScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val matches = remember { mutableStateListOf<String>() }

    LaunchedEffect(true) {
        db.collection("matches").get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val team1 = doc.getString("team1") ?: ""
                    val team2 = doc.getString("team2") ?: ""
                    val score = doc.getString("Score") ?: ""
                    matches.add("$team1 $score $team2")
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { navController.navigate("addPlayer") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Dodaj zawodnika")
        }


        LazyColumn {
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
}

// 🔽 Placeholdery dla innych zakładek
@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name screen", color = MaterialTheme.colorScheme.onBackground)
    }
}


