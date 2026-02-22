package com.leosoft.longevity.ui.life.pulse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leosoft.longevity.R

@Composable
fun PulseInfoTab() {
    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.life_pulse_info_how_it_works_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.life_pulse_info_how_it_works_body))
                Text(stringResource(R.string.life_pulse_info_best_results_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.life_pulse_info_step1))
                Text(stringResource(R.string.life_pulse_info_step2))
                Text(stringResource(R.string.life_pulse_info_step3))
                Text(stringResource(R.string.life_pulse_info_step4))
                Text(stringResource(R.string.life_pulse_info_step5))
                Text(stringResource(R.string.life_pulse_info_fail_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.life_pulse_info_fail_body))
                Text(stringResource(R.string.life_pulse_disclaimer), style = MaterialTheme.typography.bodyMedium)
            } }
        }
    }
}
