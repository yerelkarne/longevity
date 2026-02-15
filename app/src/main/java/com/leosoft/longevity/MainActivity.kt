package com.leosoft.longevity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.leosoft.longevity.ui.main.MainViewModel
import com.leosoft.longevity.ui.navigation.bottomDestinations
import com.leosoft.longevity.ui.screens.tabs.AktiviteModule
import com.leosoft.longevity.ui.screens.tabs.AnalizModule
import com.leosoft.longevity.ui.screens.tabs.BeslenmeModule
import com.leosoft.longevity.ui.screens.tabs.GunumModule
import com.leosoft.longevity.ui.screens.tabs.YasamModule
import com.leosoft.longevity.ui.theme.LongevityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LongevityTheme {
                val vm: MainViewModel = viewModel()
                MainScaffold(vm)
            }
        }
    }
}

@Composable
private fun MainScaffold(vm: MainViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navController.navigate(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = destination.title) },
                        label = { Text(destination.title) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = bottomDestinations.first().route, modifier = Modifier.padding(padding)) {
            composable("gunum") { GunumModule(vm) }
            composable("beslenme") { BeslenmeModule(vm) }
            composable("aktivite") { AktiviteModule() }
            composable("yasam") { YasamModule() }
            composable("analiz") { AnalizModule() }
        }
    }
}
