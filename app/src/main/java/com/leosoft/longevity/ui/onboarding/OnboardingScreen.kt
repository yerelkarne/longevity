package com.leosoft.longevity.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leosoft.longevity.ui.main.OnboardingForm

@Composable
fun OnboardingScreen(onComplete: (OnboardingForm) -> Unit) {
    var form by remember { mutableStateOf(OnboardingForm()) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Uzun Ömür Koçu Kurulumu")
        NumberField("Protein hedefi", form.proteinTarget.toString()) { form = form.copy(proteinTarget = it.toFloatOrNull() ?: form.proteinTarget) }
        NumberField("Karb hedefi", form.carbsTarget.toString()) { form = form.copy(carbsTarget = it.toFloatOrNull() ?: form.carbsTarget) }
        NumberField("Yağ hedefi", form.fatTarget.toString()) { form = form.copy(fatTarget = it.toFloatOrNull() ?: form.fatTarget) }
        NumberField("Fiber hedefi", form.fiberTarget.toString()) { form = form.copy(fiberTarget = it.toFloatOrNull() ?: form.fiberTarget) }
        NumberField("Su hedefi (ml)", form.waterTarget.toString()) { form = form.copy(waterTarget = it.toIntOrNull() ?: form.waterTarget) }
        NumberField("Adım hedefi (5000/10000/15000)", form.stepsTarget.toString()) { form = form.copy(stepsTarget = it.toIntOrNull() ?: form.stepsTarget) }
        NumberField("Uyku hedefi (dakika)", form.sleepTarget.toString()) { form = form.copy(sleepTarget = it.toIntOrNull() ?: form.sleepTarget) }
        NumberField("Takviye hedefi (günlük)", form.supplementsTarget.toString()) { form = form.copy(supplementsTarget = it.toIntOrNull() ?: form.supplementsTarget) }
        OutlinedTextField(value = form.bedTime, onValueChange = { form = form.copy(bedTime = it) }, label = { Text("Yatış saati") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = form.wakeTime, onValueChange = { form = form.copy(wakeTime = it) }, label = { Text("Uyanış saati") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onComplete(form) }, modifier = Modifier.fillMaxWidth()) { Text("Başla") }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}
