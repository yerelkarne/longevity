package com.leosoft.longevity.ui.life.pulse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.leosoft.longevity.R
import com.leosoft.longevity.ui.main.MainViewModel

@Composable
fun PulseCameraRoute(viewModel: MainViewModel) {
    var selected by remember { mutableStateOf(0) }
    val tabs = listOf(stringResource(R.string.life_pulse_tab_info), stringResource(R.string.life_pulse_tab_measure))
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selected) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = selected == idx, onClick = { selected = idx }, text = { Text(title) })
            }
        }
        if (selected == 0) PulseInfoTab() else PulseMeasureTab(viewModel)
    }
}
