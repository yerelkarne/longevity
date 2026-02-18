package com.leosoft.longevity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
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
import com.leosoft.longevity.ui.screens.tabs.QuickAddDialog
import com.leosoft.longevity.ui.screens.tabs.YasamModule
import com.leosoft.longevity.ui.theme.LongevityTheme

class MainActivity : ComponentActivity() {
    private var showNotificationPermissionWarning by mutableStateOf(false)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        showNotificationPermissionWarning = !granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LongevityTheme {
                val vm: MainViewModel = viewModel()
                MainScaffold(
                    vm = vm,
                    showNotificationPermissionWarning = showNotificationPermissionWarning,
                    onDismissNotificationWarning = { showNotificationPermissionWarning = false }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showNotificationPermissionWarning = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    vm: MainViewModel,
    showNotificationPermissionWarning: Boolean,
    onDismissNotificationWarning: () -> Unit
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val openQuickAdd = remember { mutableStateOf(false) }

    val topBarColor = Color(0xFFEDE7F6)
    val bottomBarColor = MaterialTheme.colorScheme.surface

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = topBarColor),
                title = {
                    Text(
                        text = "Longevity Coach",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Cursive
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { openQuickAdd.value = true }) {
                Icon(Icons.Rounded.AddCircle, contentDescription = stringResource(R.string.add_record_fab_cd))
            }
        },
        bottomBar = {
            NavigationBar(containerColor = bottomBarColor) {
                bottomDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navController.navigate(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = stringResource(destination.titleRes)) },
                        label = { Text(stringResource(destination.titleRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = bottomDestinations.first().route, modifier = Modifier.padding(padding)) {
            composable("gunum") { GunumModule(vm) }
            composable("beslenme") { BeslenmeModule(vm) }
            composable("aktivite") { AktiviteModule() }
            composable("yasam") { YasamModule(vm) }
            composable("analiz") { AnalizModule() }
        }

        if (openQuickAdd.value) {
            QuickAddDialog(viewModel = vm, onDismiss = { openQuickAdd.value = false })
        }
        if (showNotificationPermissionWarning) {
            AlertDialog(
                onDismissRequest = onDismissNotificationWarning,
                title = { Text(stringResource(R.string.notification_permission_required_title)) },
                text = { Text(stringResource(R.string.notification_permission_required_message)) },
                confirmButton = {
                    TextButton(onClick = onDismissNotificationWarning) {
                        Text(stringResource(R.string.understood))
                    }
                }
            )
        }
    }
}
