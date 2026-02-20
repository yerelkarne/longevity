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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
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
    private var activityRecognitionGranted: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    private lateinit var vm: MainViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val activityPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        activityRecognitionGranted = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm = ViewModelProvider(this)[MainViewModel::class.java]
        setContent {
            LongevityTheme {
                MainScaffold(vm, activityRecognitionGranted)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestNotificationPermissionIfNeeded()
        requestActivityRecognitionPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        vm.startPedometerForegroundOnly(forceFallback = !activityRecognitionGranted)
    }

    override fun onPause() {
        vm.stopPedometerForegroundOnly()
        super.onPause()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestActivityRecognitionPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            activityRecognitionGranted = true
            return
        }
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        activityRecognitionGranted = granted
        if (!granted) activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }
}

@Composable
private fun MainScaffold(vm: MainViewModel, activityRecognitionGranted: Boolean) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val openQuickAdd = remember { mutableStateOf(false) }
    var backgroundTrackingEnabled by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { openQuickAdd.value = true }) {
                Icon(Icons.Rounded.AddCircle, contentDescription = stringResource(R.string.add_record_fab_cd))
            }
        },
        bottomBar = {
            NavigationBar {
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
            composable("aktivite") {
                AktiviteModule(
                    viewModel = vm,
                    activityRecognitionGranted = activityRecognitionGranted,
                    backgroundTrackingEnabled = backgroundTrackingEnabled,
                    onBackgroundTrackingToggle = {
                        backgroundTrackingEnabled = it
                        vm.setBackgroundTrackingEnabled(it)
                    }
                )
            }
            composable("yasam") { YasamModule() }
            composable("analiz") { AnalizModule() }
        }

        if (openQuickAdd.value) {
            QuickAddDialog(viewModel = vm, onDismiss = { openQuickAdd.value = false })
        }
    }
}
