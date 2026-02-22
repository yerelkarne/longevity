package com.leosoft.longevity.ui.life.pulse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.leosoft.longevity.R
import com.leosoft.longevity.camera.PulseCameraManager
import com.leosoft.longevity.model.PulseStatus
import com.leosoft.longevity.signal.PulseSignalProcessor
import com.leosoft.longevity.ui.main.MainViewModel

@Composable
fun PulseMeasureTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val manager = remember { PulseCameraManager() }
    val processor = remember { PulseSignalProcessor() }

    val liveSignal = remember { mutableStateListOf<Double>() }
    val measureSignal = remember { mutableStateListOf<Double>() }

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    var measuring by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    var quality by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf(context.getString(R.string.life_pulse_camera_initializing)) }
    var elapsed by remember { mutableStateOf(0) }
    var torch by remember { mutableStateOf(true) }
    var hasTorch by remember { mutableStateOf(true) }
    var resultText by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        denied = !it
    }

    DisposableEffect(hasPermission, owner) {
        if (hasPermission) {
            cameraReady = false
            manager.bindForPulse(
                context = context,
                lifecycleOwner = owner,
                onFrame = { luma, _ ->
                    liveSignal.add(luma)
                    if (liveSignal.size > 300) liveSignal.removeAt(0)
                    if (measuring) {
                        measureSignal.add(luma)
                        if (measureSignal.size > 1050) measureSignal.removeAt(0)
                    }
                },
                onTorchAvailability = {
                    hasTorch = it
                    cameraReady = true
                    statusText = if (it) context.getString(R.string.life_pulse_ready) else context.getString(R.string.life_pulse_torch_not_supported)
                }
            )
        }
        onDispose { manager.unbind(context) }
    }

    LaunchedEffect(torch, hasPermission, cameraReady) {
        if (hasPermission && cameraReady && !manager.setTorch(torch)) {
            statusText = context.getString(R.string.life_pulse_torch_not_supported)
        }
    }

    LaunchedEffect(measuring) {
        if (!measuring) return@LaunchedEffect
        if (!cameraReady) {
            statusText = context.getString(R.string.life_pulse_camera_initializing)
            measuring = false
            return@LaunchedEffect
        }

        torch = true
        elapsed = 0
        quality = 0
        resultText = ""
        measureSignal.clear()
        statusText = context.getString(R.string.life_pulse_measuring_now)

        while (measuring && elapsed <= 35) {
            kotlinx.coroutines.delay(1000)
            elapsed += 1
            if (elapsed >= 25 && measureSignal.size > 120) {
                val fs = measureSignal.size / elapsed.toDouble()
                val result = processor.process(measureSignal.toList(), fs)
                quality = result.quality
                statusText = when (result.status) {
                    PulseStatus.SUCCESS -> context.getString(R.string.life_pulse_status_finger_detected)
                    PulseStatus.NO_FINGER -> context.getString(R.string.life_pulse_status_no_finger)
                    PulseStatus.HOLD_STILL -> context.getString(R.string.life_pulse_status_hold_still)
                    PulseStatus.SATURATED -> context.getString(R.string.life_pulse_status_pressure)
                    PulseStatus.LOW_FPS -> context.getString(R.string.life_pulse_status_low_fps)
                    PulseStatus.UNSTABLE -> context.getString(R.string.life_pulse_status_unstable)
                    PulseStatus.LOW_QUALITY -> context.getString(R.string.life_pulse_quality_low)
                    PulseStatus.INSUFFICIENT_DATA -> context.getString(R.string.life_pulse_status_longer)
                }
                if (elapsed >= 30 && result.bpm > 0) {
                    resultText = context.getString(R.string.life_pulse_bpm, result.bpm)
                    viewModel.addPulseMeasurement(result.bpm, result.quality, result.qualityLabel, elapsed)
                    measuring = false
                }
            }
        }
        if (elapsed >= 35 && resultText.isEmpty()) {
            resultText = context.getString(R.string.life_pulse_result_inconclusive)
            measuring = false
        }
    }

    if (!hasPermission) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.life_pulse_permission_rationale))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text(stringResource(R.string.life_pulse_permission_button)) }
                if (denied) {
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }) { Text(stringResource(R.string.life_pulse_open_settings)) }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (measuring) stringResource(R.string.life_pulse_measuring_now) else stringResource(R.string.life_pulse_ready), fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.life_pulse_timer, elapsed / 60, elapsed % 60), fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            PulseWaveform(signal = liveSignal, modifier = Modifier.fillMaxSize().padding(8.dp))
        }

        LinearProgressIndicator(progress = { (elapsed / 35f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        Text(statusText)

        if (resultText.isNotEmpty()) {
            Text(stringResource(R.string.life_pulse_result_title), fontWeight = FontWeight.Bold)
            Text(resultText, fontWeight = FontWeight.Bold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { measuring = !measuring }) {
                Text(stringResource(if (measuring) R.string.life_pulse_stop else R.string.life_pulse_start))
            }
            Switch(checked = torch, onCheckedChange = { torch = it })
            if (!hasTorch) Text(stringResource(R.string.life_pulse_torch_not_supported))
        }
    }
}

@Composable
private fun PulseWaveform(signal: List<Double>, modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (signal.size < 2) return@Canvas
            val points = signal.takeLast(240)
            val min = points.minOrNull() ?: 0.0
            val max = points.maxOrNull() ?: 1.0
            val range = (max - min).coerceAtLeast(1e-6)
            val path = Path()
            points.forEachIndexed { index, value ->
                val x = size.width * index / (points.size - 1).toFloat()
                val y = size.height - (((value - min) / range).toFloat() * size.height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawLine(Color(0x332196F3), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1f)
            drawPath(path = path, color = Color(0xFFE91E63))
        }
    }
}
