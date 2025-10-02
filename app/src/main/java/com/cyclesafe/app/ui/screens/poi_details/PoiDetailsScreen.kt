package com.cyclesafe.app.ui.screens.poi_details

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cyclesafe.app.ui.components.RatingBar
import com.cyclesafe.app.ui.theme.CycleSafeYellow
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailsScreen(
    poiId: String,
    navController: NavController,
    viewModel: PoiDetailsViewModel = viewModel(factory = PoiDetailsViewModelFactory(LocalContext.current.applicationContext as Application, poiId))
) {
    val poiDetailsState by viewModel.poiDetailsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Point of Interest") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = poiDetailsState) {
            is PoiDetailsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PoiDetailsState.Success -> {
                PoiDetailsSuccessContent(state, viewModel, paddingValues)
            }
            is PoiDetailsState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(text = state.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PoiDetailsSuccessContent(
    state: PoiDetailsState.Success,
    viewModel: PoiDetailsViewModel,
    paddingValues: PaddingValues
) {
    val userRating by viewModel.userRating.collectAsState()
    val userComment by viewModel.userComment.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PoiDetailsCard(state)
        }

        item {
            UserInputCard(userRating, userComment, viewModel)
        }

        if (state.comments.isNotEmpty()) {
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Comments", style = MaterialTheme.typography.headlineSmall)
            }

            items(state.comments) { comment ->
                CommentCard(comment)
            }
        }
    }
}

@Composable
private fun PoiDetailsCard(state: PoiDetailsState.Success) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            if (state.poi.imageUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(state.poi.imageUrl),
                    contentDescription = "POI Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = state.poi.name, style = MaterialTheme.typography.headlineMedium)
                Text(text = "Added by ${state.authorName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text(text = state.poi.description, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Type: ${state.poi.type}", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingBar(rating = state.poi.averageRating, onRatingChanged = {})
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "(${state.poi.ratingCount} ratings)", style = MaterialTheme.typography.bodyMedium)
                }
            }

            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(state.poi.latitude, state.poi.longitude), 15f)
            }
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                cameraPositionState = cameraPositionState
            ) {
                Marker(state = rememberUpdatedMarkerState(position = LatLng(state.poi.latitude, state.poi.longitude)))
            }
        }
    }
}

@Composable
private fun UserInputCard(userRating: Float, userComment: String, viewModel: PoiDetailsViewModel) {
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Your Rating & Comment", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            RatingBar(rating = userRating, onRatingChanged = { viewModel.onUserRatingChange(it) })
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = userComment,
                onValueChange = { viewModel.onUserCommentChange(it) },
                label = { Text("Add a comment") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.addRatingAndComment() },
                modifier = Modifier.align(Alignment.End),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = CycleSafeYellow, contentColor = MaterialTheme.colorScheme.onSecondary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
private fun CommentCard(comment: com.cyclesafe.app.data.model.Comment) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = comment.userName, style = MaterialTheme.typography.titleMedium)
            Text(text = comment.comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}