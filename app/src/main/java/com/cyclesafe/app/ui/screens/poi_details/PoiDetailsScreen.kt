package com.cyclesafe.app.ui.screens.poi_details

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cyclesafe.app.ui.components.RatingBar
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailsScreen(poiId: String, navController: NavController, viewModel: PoiDetailsViewModel = viewModel()) {
    val poiDetailsState by viewModel.poiDetailsState.collectAsState()
    val userRating by viewModel.userRating.collectAsState()
    val userComment by viewModel.userComment.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getPoiDetails(poiId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("POI Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                when (val state = poiDetailsState) {
                    is PoiDetailsState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is PoiDetailsState.Success -> {
                        val poi = state.poi
                        Column {
                            Text(text = poi.name, style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            poi.imageUrl?.let {
                                Image(
                                    painter = rememberAsyncImagePainter(it),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(200.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(text = poi.description)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Type: ${poi.type}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RatingBar(rating = poi.averageRating, onRatingChanged = {})
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "(${poi.ratingCount} ratings)")
                            }
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
                                Marker(state = MarkerState(position = LatLng(poi.latitude, poi.longitude)))
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Comments", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    is PoiDetailsState.Error -> {
                        Text(text = state.message)
                    }
                }
            }

            if (poiDetailsState is PoiDetailsState.Success) {
                val successState = poiDetailsState as PoiDetailsState.Success
                items(successState.comments) { comment ->
                    Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = comment.userName, style = MaterialTheme.typography.bodyLarge)
                            Text(text = comment.comment)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your Rating & Comment", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    RatingBar(rating = userRating, onRatingChanged = { viewModel.onUserRatingChange(it) })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userComment,
                        onValueChange = { viewModel.onUserCommentChange(it) },
                        label = { Text("Add a comment") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        viewModel.addRatingAndComment(poiId)
                    }) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}