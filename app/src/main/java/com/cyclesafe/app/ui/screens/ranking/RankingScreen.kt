package com.cyclesafe.app.ui.screens.ranking

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyclesafe.app.R
import com.cyclesafe.app.ui.navigation.Screen
import com.cyclesafe.app.ui.theme.CycleSafeYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(navController: NavController, viewModel: RankingViewModel = viewModel()) {
    val rankingState by viewModel.rankingState.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking") },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.mipmap.bicycle_icon),
                        contentDescription = "App Logo",
                        colorFilter = ColorFilter.tint(CycleSafeYellow),
                        modifier = Modifier.size(40.dp).padding(start = 8.dp)
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                            NavigationBarItem(
                                selected = false,
                                onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) } },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Map") },
                                label = { Text("Map") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = { navController.navigate(Screen.PoiList.route) { popUpTo(Screen.Home.route) } },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "POIs") },
                                label = { Text("POIs") }
                            )
                            NavigationBarItem(
                                selected = true,
                                onClick = { /* Already on Ranking */ },
                                icon = { Icon(Icons.Default.Star, contentDescription = "Ranking") },
                                label = { Text("Ranking") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Home.route) } },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") }
                            )            }
        }
    ) { paddingValues ->
        when (val state = rankingState) {
            is RankingUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is RankingUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(text = "Top Users", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    itemsIndexed(state.users) { index, user ->
                        val isCurrentUser = user.uid == currentUserId
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentUser) CycleSafeYellow else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}. ${user.firstName} ${user.lastName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${user.points} points",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            is RankingUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message)
                }
            }
        }
    }
}