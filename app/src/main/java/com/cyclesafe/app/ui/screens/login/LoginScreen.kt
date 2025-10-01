package com.cyclesafe.app.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyclesafe.app.R
import com.cyclesafe.app.ui.navigation.Screen
import com.cyclesafe.app.ui.theme.CycleSafeYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, onLoginSuccess: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.mipmap.bicycle_icon),
                contentDescription = "App Logo",
                colorFilter = ColorFilter.tint(CycleSafeYellow),
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to CycleSafe",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.loginUser() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CycleSafeYellow),
                enabled = loginState !is LoginState.Loading
            ) {
                if (loginState is LoginState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Text("Login", color = MaterialTheme.colorScheme.onSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { navController.navigate(Screen.Registration.route) }) {
                Text("Don't have an account? Register", color = CycleSafeYellow)
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = loginState) {
                is LoginState.Success -> {
                    LaunchedEffect(Unit) {
                        onLoginSuccess()
                    }
                }
                is LoginState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }
        }
    }
}