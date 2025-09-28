package com.cyclesafe.app.ui.screens.poi_details

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.cyclesafe.app.ui.components.RatingBar
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun PoiDetailsScreen(poiId: String, viewModel: PoiDetailsViewModel = viewModel()) {
    val poiDetailsState by viewModel.poiDetailsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getPoiDetails(poiId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            when (val state = poiDetailsState) {
                is PoiDetailsState.Loading -> {
                    CircularProgressIndicator()
                }
                is PoiDetailsState.Success -> {
                    val poi = state.poi
                    Text(text = poi.name, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    poi.imageUrl?.let {
                        Image(
                            painter = rememberAsyncImagePainter(it),
                            contentDescription = null,
                            modifier = Modifier.size(256.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(text = poi.description)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Type: ${poi.type}")
                    Spacer(modifier = Modifier.height(8.dp))
                    RatingBar(rating = poi.averageRating, onRatingChanged = {})
                    Text(text = "(${poi.ratingCount} ratings)")
                    Spacer(modifier = Modifier.height(16.dp))

                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(poi.latitude, poi.longitude), 15f)
                    }

                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        cameraPositionState = cameraPositionState
                    ) {
                        val markerState = remember(poi.latitude, poi.longitude) {
                            MarkerState(position = LatLng(poi.latitude, poi.longitude))
                        }
                        Marker(state = markerState)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Comments", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is PoiDetailsState.Error -> {
                    Text(text = state.message)
                }
            }
        }

        if (poiDetailsState is PoiDetailsState.Success) {
            val successState = poiDetailsState as PoiDetailsState.Success
            items(successState.comments) { comment ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = comment.userName, style = MaterialTheme.typography.bodyLarge)
                    Text(text = comment.comment)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                var userRating by remember { mutableStateOf(successState.userRating) }
                var userComment by remember { mutableStateOf("") }
                RatingBar(rating = userRating, onRatingChanged = { userRating = it })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userComment,
                    onValueChange = { userComment = it },
                    label = { Text("Add a comment") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    viewModel.addRating(poiId, userRating)
                    if (userComment.isNotBlank()) {
                        viewModel.addComment(poiId, userComment)
                    }
                }) {
                    Text("Submit")
                }
            }
        }
    }
}