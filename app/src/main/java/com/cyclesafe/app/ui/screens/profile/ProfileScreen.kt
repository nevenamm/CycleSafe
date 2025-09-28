package com.cyclesafe.app.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cyclesafe.app.ui.navigation.Screen

@Composable
fun ProfileScreen(navController: NavController, viewModel: ProfileViewModel = viewModel()) {
    val profileState by viewModel.profileState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (profileState) {
            is ProfileState.Loading -> {
                CircularProgressIndicator()
            }
            is ProfileState.Success -> {
                val userProfile = (profileState as ProfileState.Success).user
                if (userProfile.imageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(userProfile.imageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                    )
                } else {
                    // Placeholder image
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "${userProfile.firstName} ${userProfile.lastName}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = userProfile.email)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = userProfile.phone)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }) {
                    Text("Log out")
                }
            }
            is ProfileState.Error -> {
                Text(text = (profileState as ProfileState.Error).message)
            }
            else -> {}
        }
    }
}