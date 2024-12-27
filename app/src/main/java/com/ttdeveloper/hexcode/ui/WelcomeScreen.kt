package com.ttdeveloper.hexcode.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(0) }
    val features = listOf(
        Feature(
            icon = Icons.Default.ColorLens,
            title = "Color Picker",
            description = "Tap anywhere on an image or camera preview to instantly reveal its exact color code"
        ),
        Feature(
            icon = Icons.Default.Camera,
            title = "Real-time Camera",
            description = "Use your camera to capture colors from the real world in real-time"
        ),
        Feature(
            icon = Icons.Default.PhotoLibrary,
            title = "Gallery Access",
            description = "Import images from your gallery to extract colors from existing photos"
        ),
        Feature(
            icon = Icons.Default.ContentCopy,
            title = "Quick Copy",
            description = "Copy color codes with a single tap for easy use in your projects"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to\nHexcode Revealer",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
        )

        Text(
            text = "Your pocket color picker for design inspiration",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            features.forEachIndexed { index, feature ->
                FeatureItem(
                    feature = feature,
                    isVisible = currentPage >= index
                )
                if (index < features.lastIndex) {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        Button(
            onClick = {
                if (currentPage < features.size - 1) {
                    currentPage++
                } else {
                    onGetStarted()
                }
            },
            modifier = Modifier
                .padding(vertical = 32.dp)
                .fillMaxWidth()
        ) {
            Text(
                if (currentPage < features.size - 1) "Next" else "Get Started",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (currentPage > 0) {
            TextButton(
                onClick = { currentPage = features.size - 1 }
            ) {
                Text("Skip")
            }
        }
    }
}

@Composable
private fun FeatureItem(
    feature: Feature,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                )
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private data class Feature(
    val icon: ImageVector,
    val title: String,
    val description: String
)
