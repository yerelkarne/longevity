package com.leosoft.longevity.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.leosoft.longevity.R

data class BottomDestination(val route: String, @StringRes val titleRes: Int, val icon: ImageVector)

val bottomDestinations = listOf(
    BottomDestination("gunum", R.string.nav_today, Icons.Rounded.Today),
    BottomDestination("beslenme", R.string.nav_nutrition, Icons.Rounded.LocalDining),
    BottomDestination("aktivite", R.string.nav_activity, Icons.Rounded.DirectionsWalk),
    BottomDestination("yasam", R.string.nav_life, Icons.Rounded.Favorite),
    BottomDestination("analiz", R.string.nav_analysis, Icons.Rounded.Analytics)
)
