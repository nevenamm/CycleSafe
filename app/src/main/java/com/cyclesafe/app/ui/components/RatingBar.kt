package com.cyclesafe.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun RatingBar(
    modifier: Modifier = Modifier,
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    isIndicator: Boolean = false
) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            val imageVector: ImageVector
            val modifierWithClick: Modifier

            if (isIndicator) {
                imageVector = when {
                    rating >= i -> Icons.Filled.Star
                    rating >= i - 0.5 -> Icons.AutoMirrored.Filled.StarHalf
                    else -> Icons.Outlined.StarBorder
                }
                modifierWithClick = Modifier
            } else {
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder
                modifierWithClick = Modifier.clickable { onRatingChanged(i.toFloat()) }
            }

            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifierWithClick
            )
        }
    }
}