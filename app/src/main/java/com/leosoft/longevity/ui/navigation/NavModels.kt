package com.leosoft.longevity.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.Today
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomDestination(val route: String, val title: String, val icon: ImageVector)

val bottomDestinations = listOf(
    BottomDestination("gunum", "Günüm", Icons.Rounded.Today),
    BottomDestination("beslenme", "Beslenme", Icons.Rounded.LocalDining),
    BottomDestination("aktivite", "Aktivite", Icons.Rounded.DirectionsWalk),
    BottomDestination("yasam", "Yaşam", Icons.Rounded.Favorite),
    BottomDestination("analiz", "Analiz", Icons.Rounded.Analytics)
)
