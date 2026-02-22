package com.leosoft.longevity.ui.screens.tabs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leosoft.longevity.R
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealType
import com.leosoft.longevity.data.local.entity.SleepLogEntity
import com.leosoft.longevity.data.local.entity.WorkoutType
import com.leosoft.longevity.domain.usecase.CalculateMacroTotalsUseCase
import com.leosoft.longevity.reminders.ReminderAlarmScheduler
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.util.Locale
import com.leosoft.longevity.ui.components.MiniProgressCard
import com.leosoft.longevity.ui.components.ScoreBar
import com.leosoft.longevity.ui.main.GoalPlanItem
import com.leosoft.longevity.ui.main.MainViewModel
import com.leosoft.longevity.ui.main.WeightGoalMode
import kotlinx.coroutines.launch

private enum class QuickAddType { FOOD, WATER, SUPPLEMENT, SLEEP, ACTIVITY }

private val TrendBarColor = Color(0xFF9575CD)
private val TrendChipBackgroundColor = Color(0xFFF3E5F5)
private val TrendChipSelectedTextColor = Color(0xFF4A148C)
private val TrendChipDefaultTextColor = Color(0xFF6A1B9A)
private val TrendValueTextColor = Color(0xFF5E35B1)

@Composable
fun ModuleTabLayout(
    tabs: List<String>,
    initialPage: Int = 0,
    requestedPage: Int? = null,
    onRequestConsumed: () -> Unit = {},
    content: @Composable (Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))) { tabs.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(requestedPage) {
        val page = requestedPage ?: return@LaunchedEffect
        pagerState.animateScrollToPage(page.coerceIn(0, tabs.lastIndex))
        onRequestConsumed()
    }
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, tab ->
                Tab(selected = index == pagerState.currentPage, onClick = { scope.launch { pagerState.animateScrollToPage(index) } }, text = { Text(tab) })
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AnimatedContent(
                targetState = page,
                transitionSpec = { (fadeIn(animationSpec = tween(350)) + slideInVertically(animationSpec = tween(350)) { it / 10 }) togetherWith (fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { -it / 10 }) },
                label = "module-page-transition"
            ) { currentPage ->
                content(currentPage)
            }
        }
    }
}

@Composable
fun GunumModule(viewModel: MainViewModel) {
    val tabs = listOf(stringResource(R.string.tab_me), stringResource(R.string.tab_summary), stringResource(R.string.tab_tasks), stringResource(R.string.tab_reminders))
    var requestedPage by remember { mutableStateOf<Int?>(null) }
    ModuleTabLayout(
        tabs = tabs,
        initialPage = 1,
        requestedPage = requestedPage,
        onRequestConsumed = { requestedPage = null }
    ) { page ->
        when (page) {
            0 -> GunumBenScreen(viewModel, onGoalsCreated = { requestedPage = 2 })
            1 -> GunumOzetScreen(viewModel)
            2 -> GunumHedeflerScreen(viewModel)
            3 -> GunumHatirlatmalarScreen(viewModel)
            else -> GunumOzetScreen(viewModel)
        }
    }
}

@Composable
fun BeslenmeModule(viewModel: MainViewModel) {
    val tabs = listOf(stringResource(R.string.tab_log), stringResource(R.string.tab_macros), stringResource(R.string.tab_micros), stringResource(R.string.tab_water), stringResource(R.string.tab_supplements))
    ModuleTabLayout(tabs) { page ->
        when (page) {
            0 -> BeslenmeKayitScreen(viewModel)
            1 -> BeslenmeMakrolarScreen(viewModel)
            2 -> BeslenmeMikrolarScreen(viewModel)
            3 -> BeslenmeSuScreen(viewModel)
            4 -> BeslenmeTakviyelerScreen(viewModel)
            else -> PlaceholderTab(stringResource(R.string.placeholder_ready_template, page))
        }
    }
}

@Composable
fun AktiviteModule(viewModel: MainViewModel) {
    val tabs = listOf(
        stringResource(R.string.activity_tab_steps),
        stringResource(R.string.activity_tab_add_exercise)
    )
    ModuleTabLayout(tabs) { page ->
        when (page) {
            0 -> ActivityStepsScreen(viewModel)
            1 -> ActivityExerciseScreen(viewModel)
            else -> PlaceholderTab(stringResource(R.string.nav_activity))
        }
    }
}

@Composable
private fun ActivityStepsScreen(viewModel: MainViewModel) {
    val dashboard by viewModel.dashboard.collectAsState()
    val state by viewModel.stepTrackingState.collectAsState()
    val userGoals by viewModel.userGoals.collectAsState()
    val steps = dashboard?.steps ?: 0
    val goal = userGoals?.stepsTarget ?: 10000
    val progress = (steps / goal.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.today_steps_label), style = MaterialTheme.typography.titleMedium, color = TrendChipDefaultTextColor)
                    Text("$steps", style = MaterialTheme.typography.displaySmall, color = Color(0xFF2D2A32))
                    AnimatedProgressBar(
                        target = progress,
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = Color(0xFF7E57C2),
                        trackColor = Color(0xFFEDE7F6)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.steps_goal_progress, steps, goal), style = MaterialTheme.typography.bodyLarge)
                        Text("%${(progress * 100).toInt()}", style = MaterialTheme.typography.bodyLarge, color = TrendChipDefaultTextColor)
                    }
                    Text(stringResource(R.string.activity_goal_sync_info, goal), style = MaterialTheme.typography.bodySmall, color = TrendValueTextColor)
                }
            }
        }

        if (viewModel.usesEstimatedTracking) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Text(stringResource(R.string.estimated_tracking_info), modifier = Modifier.padding(12.dp))
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = TrendChipBackgroundColor)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (state.isForegroundTrackingEnabled) stringResource(R.string.step_tracking_on) else stringResource(R.string.step_tracking_off),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.setForegroundStepTracking(true) }) { Text(stringResource(R.string.enable_background)) }
                        Button(onClick = { viewModel.setForegroundStepTracking(false) }) { Text(stringResource(R.string.disable_background)) }
                    }
                }
            }
        }

        item {
            Text(stringResource(R.string.battery_optimization_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}


@Composable
private fun ActivityExerciseScreen(viewModel: MainViewModel) {
    val workoutLogs by viewModel.workoutLogs.collectAsState()
    var range by remember { mutableStateOf(ActivityChartRange.DAILY) }
    val chartData = remember(workoutLogs, range) { buildWorkoutChartData(workoutLogs, range) }
    val maxMinutes = (chartData.maxOfOrNull { it.minutes } ?: 1).coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.activity_exercise_trend_title), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TrendChipBackgroundColor, RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ActivityRangeChip(stringResource(R.string.life_sleep_range_daily), range == ActivityChartRange.DAILY) { range = ActivityChartRange.DAILY }
                        ActivityRangeChip(stringResource(R.string.life_sleep_range_weekly), range == ActivityChartRange.WEEKLY) { range = ActivityChartRange.WEEKLY }
                        ActivityRangeChip(stringResource(R.string.life_sleep_range_monthly), range == ActivityChartRange.MONTHLY) { range = ActivityChartRange.MONTHLY }
                    }

                    if (chartData.isEmpty()) {
                        Text(stringResource(R.string.activity_exercise_empty), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(190.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chartData.forEach { point ->
                                val ratio = point.minutes / maxMinutes.toFloat()
                                val barHeight = if (point.minutes <= 0) 0.dp else (12 + (108 * ratio)).dp
                                val animatedBarHeight by animateDpAsState(targetValue = barHeight, animationSpec = tween(650), label = "activity-bar")
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    Text(formatSleepHoursShort(point.minutes), style = MaterialTheme.typography.labelSmall, color = TrendValueTextColor)
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp),
                                        contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                               .height(animatedBarHeight)
                                                .background(TrendBarColor, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                        )
                                    }
                                    Text(point.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (workoutLogs.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.activity_exercise_empty), modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            items(workoutLogs, key = { it.id }) { workout ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(workout.date.toString(), style = MaterialTheme.typography.titleSmall)
                        Text(resolveWorkoutTypeLabel(workout.type))
                        Text(stringResource(R.string.activity_exercise_duration, formatSleepDurationLabel(workout.durationMinutes)))
                        Text(stringResource(R.string.activity_exercise_intensity, workout.intensity))
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ActivityRangeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = if (selected) TrendChipSelectedTextColor else TrendChipDefaultTextColor)
    }
}

private enum class ActivityChartRange { DAILY, WEEKLY, MONTHLY }
private data class ActivityChartPoint(val label: String, val minutes: Int)

private fun buildWorkoutChartData(
    workoutLogs: List<com.leosoft.longevity.data.local.entity.WorkoutLogEntity>,
    range: ActivityChartRange
): List<ActivityChartPoint> {
    return when (range) {
        ActivityChartRange.DAILY -> {
            val end = LocalDate.now()
            val start = end.minusDays(6)
            generateSequence(start) { d -> if (d < end) d.plusDays(1) else null }
                .take(7)
                .map { day ->
                    val total = workoutLogs.filter { it.date == day }.sumOf { it.durationMinutes }
                    ActivityChartPoint(day.dayOfMonth.toString(), total)
                }
                .toList()
        }
        ActivityChartRange.WEEKLY -> {
            val today = LocalDate.now()
            (5 downTo 0).map { weeksAgo ->
                val anchor = today.minusWeeks(weeksAgo.toLong())
                val start = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
                val end = start.plusDays(6)
                val total = workoutLogs.filter { it.date >= start && it.date <= end }.sumOf { it.durationMinutes }
                ActivityChartPoint("W${start.dayOfMonth}", total)
            }
        }
        ActivityChartRange.MONTHLY -> {
            val current = YearMonth.now()
            (5 downTo 0).map { mAgo ->
                val month = current.minusMonths(mAgo.toLong())
                val total = workoutLogs.filter { YearMonth.from(it.date) == month }.sumOf { it.durationMinutes }
                ActivityChartPoint(month.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())), total)
            }
        }
    }
}

@Composable
private fun resolveWorkoutTypeLabel(type: WorkoutType): String = stringResource(workoutTypeLabel(type))


@Composable
fun YasamModule(viewModel: MainViewModel) {
    val tabs = listOf(stringResource(R.string.life_tab_sleep), stringResource(R.string.life_tab_cycle))
    ModuleTabLayout(tabs) { page ->
        when (page) {
            0 -> YasamUykuScreen(viewModel)
            1 -> YasamReglScreen(viewModel)
            else -> PlaceholderTab(stringResource(R.string.nav_life))
        }
    }
}

@Composable
private fun YasamUykuScreen(viewModel: MainViewModel) {
    val sleepLogs by viewModel.sleepLogs.collectAsState()
    var range by remember { mutableStateOf(SleepChartRange.DAILY) }
    val chartData = remember(sleepLogs, range) { buildSleepChartData(sleepLogs, range) }
    val maxMinutes = (chartData.maxOfOrNull { it.minutes } ?: 1).coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.life_sleep_trend_title), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TrendChipBackgroundColor, RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SleepRangeChip(
                            text = stringResource(R.string.life_sleep_range_daily),
                            selected = range == SleepChartRange.DAILY,
                            onClick = { range = SleepChartRange.DAILY }
                        )
                        SleepRangeChip(
                            text = stringResource(R.string.life_sleep_range_weekly),
                            selected = range == SleepChartRange.WEEKLY,
                            onClick = { range = SleepChartRange.WEEKLY }
                        )
                        SleepRangeChip(
                            text = stringResource(R.string.life_sleep_range_monthly),
                            selected = range == SleepChartRange.MONTHLY,
                            onClick = { range = SleepChartRange.MONTHLY }
                        )
                    }
                    if (chartData.isEmpty()) {
                        Text(stringResource(R.string.life_sleep_empty), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chartData.forEach { point ->
                                val ratio = point.minutes / maxMinutes.toFloat()
                                val barHeight = if (point.minutes <= 0) 0.dp else (12 + (108 * ratio)).dp
                                val animatedBarHeight by animateDpAsState(targetValue = barHeight, animationSpec = tween(650), label = "sleep-bar")
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = formatSleepHoursShort(point.minutes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TrendChipDefaultTextColor
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(top = 6.dp),
                                        contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(animatedBarHeight)
                                                .background(TrendBarColor, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                        )
                                    }
                                    Text(point.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (sleepLogs.isNotEmpty()) {
            items(sleepLogs, key = { it.id }) { sleep ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(sleep.date.toString(), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.life_sleep_bedtime, sleep.bedtime.toLocalTime().toString()))
                        Text(stringResource(R.string.life_sleep_waketime, sleep.wakeTime.toLocalTime().toString()))
                        Text(stringResource(R.string.life_sleep_duration, formatSleepDurationLabel(sleep.durationMinutes)))
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SleepRangeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = if (selected) TrendChipSelectedTextColor else TrendChipDefaultTextColor)
    }
}

private enum class SleepChartRange { DAILY, WEEKLY, MONTHLY }

private data class SleepChartPoint(val label: String, val minutes: Int)


private fun formatSleepHoursShort(minutes: Int): String {
    if (minutes <= 0) return "0s"
    val hours = minutes / 60f
    return String.format(Locale.getDefault(), "%.1f s", hours)
}

private fun buildSleepChartData(
    sleepLogs: List<com.leosoft.longevity.data.local.entity.SleepLogEntity>,
    range: SleepChartRange
): List<SleepChartPoint> {
    val byDate = sleepLogs.associateBy { it.date }
    return when (range) {
        SleepChartRange.DAILY -> {
            val end = LocalDate.now()
            val start = end.minusDays(6)
            generateSequence(start) { d -> if (d < end) d.plusDays(1) else null }
                .take(7)
                .map { day -> SleepChartPoint(day.dayOfMonth.toString(), byDate[day]?.durationMinutes ?: 0) }
                .toList()
        }
        SleepChartRange.WEEKLY -> {
            val today = LocalDate.now()
            (5 downTo 0).map { weeksAgo ->
                val anchor = today.minusWeeks(weeksAgo.toLong())
                val start = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
                val end = start.plusDays(6)
                val avg = sleepLogs.filter { it.date >= start && it.date <= end }
                    .map { it.durationMinutes }
                    .let { if (it.isEmpty()) 0 else it.sum() / it.size }
                SleepChartPoint("W${start.dayOfMonth}", avg)
            }
        }
        SleepChartRange.MONTHLY -> {
            val current = YearMonth.now()
            (5 downTo 0).map { mAgo ->
                val month = current.minusMonths(mAgo.toLong())
                val avg = sleepLogs.filter { YearMonth.from(it.date) == month }
                    .map { it.durationMinutes }
                    .let { if (it.isEmpty()) 0 else it.sum() / it.size }
                SleepChartPoint(month.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())), avg)
            }
        }
    }
}
@Composable
private fun YasamRutinlerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.life_routines_intro))
                    TextButton(onClick = { showAddDialog = true }) { Text(stringResource(R.string.life_routine_add)) }
                }
            }
        }
        if (reminders.isEmpty()) {
            item {
                Text(stringResource(R.string.life_routines_empty))
            }
        } else {
            items(reminders, key = { it.id }) { reminder ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(reminder.reminderType, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (reminder.cadence == "hourly") stringResource(R.string.reminder_hourly_every, reminder.intervalHours ?: 1)
                            else stringResource(R.string.reminder_daily_at, reminder.reminderTime)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            title = stringResource(R.string.life_routine_create_title),
            onDismiss = { showAddDialog = false },
            onSave = { title, cadence, dailyTime, interval ->
                scope.launch {
                    val id = viewModel.addReminder(title, dailyTime, cadence, interval)
                    ReminderAlarmScheduler.schedule(context, id, title, cadence, dailyTime, interval ?: 1)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun YasamReglScreen(viewModel: MainViewModel) {
    val logs by viewModel.menstrualCycleLogs.collectAsState()
    val latest = logs.firstOrNull()
    val months = remember { (0..11).map { YearMonth.now().plusMonths(it.toLong()) } }
    val monthPager = androidx.compose.foundation.pager.rememberPagerState { months.size }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (latest == null) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.life_routines_empty), modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            val start = latest.periodStartDate
            val today = LocalDate.now()
            val cycleLength = latest.cycleLengthDays.coerceAtLeast(1)
            val periodLength = latest.periodLengthDays.coerceIn(1, cycleLength)
            val daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt()
            val cyclesSinceStart = if (daysFromStart <= 0) 0 else (daysFromStart / cycleLength) + 1
            val next = start.plusDays(cyclesSinceStart.toLong() * cycleLength.toLong())
            val ovulationOffset = (cycleLength - 14).coerceIn(0, cycleLength - 1)
            val fertileStartOffset = (ovulationOffset - 5).coerceAtLeast(0)
            val fertileEndOffset = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)
            val ovulation = next.plusDays(ovulationOffset.toLong())
            val fertileStart = next.plusDays(fertileStartOffset.toLong())
            val fertileEnd = next.plusDays(fertileEndOffset.toLong())

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.menstrual_cycle_title), style = MaterialTheme.typography.titleLarge, color = TrendChipDefaultTextColor)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(R.string.menstrual_phase_period), style = MaterialTheme.typography.labelSmall, color = Color(0xFFAD1457))
                                    Text(next.toString(), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(R.string.menstrual_phase_ovulation), style = MaterialTheme.typography.labelSmall, color = Color(0xFFF57F17))
                                    Text(ovulation.toString(), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.menstrual_phase_fertile), style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                                Text("${fertileStart} - ${fertileEnd}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val cycleProgress = ((daysFromStart % cycleLength).coerceAtLeast(0) / cycleLength.toFloat()).coerceIn(0f, 1f)
                            val animatedCycleProgress by animateFloatAsState(targetValue = cycleProgress, animationSpec = tween(750), label = "cycle-progress")
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Döngü ilerleme", style = MaterialTheme.typography.labelSmall, color = TrendChipDefaultTextColor)
                                AnimatedProgressBar(target = animatedCycleProgress, modifier = Modifier.fillMaxWidth().height(8.dp))
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalPager(state = monthPager, modifier = Modifier.fillMaxWidth()) { page ->
                            val month = months[page]
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    listOf("P", "S", "Ç", "P", "C", "C", "P").forEach { d ->
                                        Text(d, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                    }
                                }

                                val firstDayOffset = (month.atDay(1).dayOfWeek.value % 7)
                                val totalCells = ((firstDayOffset + month.lengthOfMonth() + 6) / 7) * 7
                                (0 until totalCells).chunked(7).forEach { week ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                        week.forEach { idx ->
                                            val dayNumber = idx - firstDayOffset + 1
                                            if (dayNumber !in 1..month.lengthOfMonth()) {
                                                Box(modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                                            } else {
                                                val day = month.atDay(dayNumber)
                                                val cycleDistance = java.time.temporal.ChronoUnit.DAYS.between(start, day).toInt()
                                                val cycleDay = if (cycleDistance >= 0) cycleDistance % cycleLength else -1
                                                val isPeriod = cycleDay in 0 until periodLength
                                                val isOvulation = cycleDay == ovulationOffset
                                                val isFertile = cycleDay in fertileStartOffset..fertileEndOffset
                                                val color = when {
                                                    isPeriod -> Color(0xFFF8BBD0)
                                                    isOvulation -> Color(0xFFFFF59D)
                                                    isFertile -> Color(0xFFC8E6C9)
                                                    else -> Color(0xFFF5F5F5)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(color, RoundedCornerShape(8.dp))
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                                ) {
                                                    Text(dayNumber.toString(), style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("● ${stringResource(R.string.menstrual_phase_period)}", color = Color(0xFFE91E63), style = MaterialTheme.typography.labelSmall)
                            Text("● ${stringResource(R.string.menstrual_phase_fertile)}", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
                            Text("● ${stringResource(R.string.menstrual_phase_ovulation)}", color = Color(0xFFFFC107), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsModule(viewModel: MainViewModel) {
    val prefs by viewModel.healthSyncPreferences.collectAsState()
    val scope = rememberCoroutineScope()
    var pendingSyncAfterPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(viewModel.permissionsContract()) {
        viewModel.refreshHealthPermissions()
        if (pendingSyncAfterPermission) {
            pendingSyncAfterPermission = false
            viewModel.syncNow()
        }
    }

    LaunchedEffect(Unit) { viewModel.refreshHealthPermissions() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.settings_health_connect))
                Switch(
                    checked = prefs.enabled,
                    onCheckedChange = { enabled ->
                        viewModel.setHealthSyncEnabled(enabled)
                        if (enabled && viewModel.healthConnectAvailable) {
                            scope.launch {
                                val missingPermissions = viewModel.missingHealthPermissions()
                                if (missingPermissions.isNotEmpty()) launcher.launch(missingPermissions)
                            }
                        }
                    },
                    enabled = viewModel.healthConnectAvailable
                )
            }
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        val missingPermissions = viewModel.missingHealthPermissions()
                        if (missingPermissions.isNotEmpty()) {
                            pendingSyncAfterPermission = true
                            launcher.launch(missingPermissions)
                        } else {
                            viewModel.syncNow()
                        }
                    }
                },
                enabled = prefs.enabled && viewModel.healthConnectAvailable
            ) {
                Text(stringResource(R.string.settings_sync_now))
            }
        }
    }
}

@Composable
fun GunumOzetScreen(viewModel: MainViewModel) {
    val selectedDate by viewModel.selectedGoalsDate.collectAsState()
    val foods by viewModel.foods.collectAsState()
    val allMeals by viewModel.allMealEntries.collectAsState()
    val allWater by viewModel.allWaterLogs.collectAsState()
    val allSteps by viewModel.allStepsLogs.collectAsState()
    val sleepLogs by viewModel.sleepLogs.collectAsState()
    val userGoals by viewModel.userGoals.collectAsState()
    var range by remember { mutableStateOf(GunumSummaryRange.DAILY) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    val summary = remember(selectedDate, range, allMeals, allWater, allSteps, sleepLogs, foods, userGoals) {
        buildGunumSummaryMetrics(
            selectedDate = selectedDate,
            range = range,
            meals = allMeals,
            foodsById = foods.associateBy { it.id },
            waterLogs = allWater,
            stepsLogs = allSteps,
            sleepLogs = sleepLogs,
            stepsTarget = userGoals?.stepsTarget ?: 10000,
            waterTarget = userGoals?.waterTargetMl ?: 2000,
            proteinTarget = userGoals?.proteinTarget?.toInt() ?: 120,
            sleepTarget = userGoals?.sleepTargetMinutes ?: 480
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            NutritionDatePickerCard(
                selectedDate = selectedDate,
                selectedDateText = selectedDate.format(dateFormatter),
                onDateSelected = { viewModel.setSelectedGoalsDate(it) }
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().background(TrendChipBackgroundColor, RoundedCornerShape(16.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GunumRangeChip(stringResource(R.string.life_sleep_range_daily), range == GunumSummaryRange.DAILY) { range = GunumSummaryRange.DAILY }
                GunumRangeChip(stringResource(R.string.life_sleep_range_weekly), range == GunumSummaryRange.WEEKLY) { range = GunumSummaryRange.WEEKLY }
                GunumRangeChip(stringResource(R.string.life_sleep_range_monthly), range == GunumSummaryRange.MONTHLY) { range = GunumSummaryRange.MONTHLY }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniProgressCard(stringResource(R.string.card_steps), "${summary.steps}", summary.stepsProgress, Modifier.weight(1f))
                MiniProgressCard(stringResource(R.string.card_water), "${summary.waterMl} ml", summary.waterProgress, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniProgressCard(stringResource(R.string.card_macro), "P ${summary.proteinGr}g", summary.proteinProgress, Modifier.weight(1f))
                MiniProgressCard(stringResource(R.string.card_sleep), "${summary.sleepMinutes} dk", summary.sleepProgress, Modifier.weight(1f))
            }
        }
        item {
            AnimatedSummaryBarsCard(summary)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.GunumRangeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = if (selected) TrendChipSelectedTextColor else TrendChipDefaultTextColor)
    }
}

@Composable
private fun AnimatedSummaryBarsCard(summary: GunumSummaryMetrics) {
    val stepsAnim by animateFloatAsState(targetValue = summary.stepsProgress, animationSpec = tween(700))
    val waterAnim by animateFloatAsState(targetValue = summary.waterProgress, animationSpec = tween(700))
    val proteinAnim by animateFloatAsState(targetValue = summary.proteinProgress, animationSpec = tween(700))
    val sleepAnim by animateFloatAsState(targetValue = summary.sleepProgress, animationSpec = tween(700))

    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.today_longevity_score), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    stringResource(R.string.card_steps) to stepsAnim,
                    stringResource(R.string.card_water) to waterAnim,
                    stringResource(R.string.card_macro) to proteinAnim,
                    stringResource(R.string.card_sleep) to sleepAnim
                ).forEach { (label, progress) ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("%${(progress * 100).toInt()}", style = MaterialTheme.typography.labelSmall, color = TrendValueTextColor)
                        Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
                            Box(modifier = Modifier.fillMaxWidth().height((10 + 110 * progress).dp).background(TrendBarColor, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)))
                        }
                        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private enum class GunumSummaryRange { DAILY, WEEKLY, MONTHLY }

private data class GunumSummaryMetrics(
    val steps: Int,
    val waterMl: Int,
    val proteinGr: Int,
    val sleepMinutes: Int,
    val stepsProgress: Float,
    val waterProgress: Float,
    val proteinProgress: Float,
    val sleepProgress: Float
)

private fun buildGunumSummaryMetrics(
    selectedDate: LocalDate,
    range: GunumSummaryRange,
    meals: List<com.leosoft.longevity.data.local.entity.MealEntryEntity>,
    foodsById: Map<Long, FoodEntity>,
    waterLogs: List<com.leosoft.longevity.data.local.entity.WaterLogEntity>,
    stepsLogs: List<com.leosoft.longevity.data.local.entity.StepsLogEntity>,
    sleepLogs: List<SleepLogEntity>,
    stepsTarget: Int,
    waterTarget: Int,
    proteinTarget: Int,
    sleepTarget: Int
): GunumSummaryMetrics {
    val (start, end) = when (range) {
        GunumSummaryRange.DAILY -> selectedDate to selectedDate
        GunumSummaryRange.WEEKLY -> {
            val s = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
            s to s.plusDays(6)
        }
        GunumSummaryRange.MONTHLY -> {
            val ym = YearMonth.from(selectedDate)
            ym.atDay(1) to ym.atEndOfMonth()
        }
    }
    val dayCount = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
    val steps = stepsLogs.filter { it.date in start..end }.sumOf { it.steps }
    val water = waterLogs.filter { it.date in start..end }.sumOf { it.amountMl }
    val protein = CalculateMacroTotalsUseCase().invoke(meals.filter { it.date in start..end }, foodsById).protein.toInt()
    val sleep = sleepLogs.filter { it.date in start..end }.sumOf { it.durationMinutes }
    val avgSteps = if (range == GunumSummaryRange.DAILY) steps else (steps / dayCount)
    val avgWater = if (range == GunumSummaryRange.DAILY) water else (water / dayCount)
    val avgProtein = if (range == GunumSummaryRange.DAILY) protein else (protein / dayCount)
    val avgSleep = if (range == GunumSummaryRange.DAILY) sleep else (sleep / dayCount)
    return GunumSummaryMetrics(
        steps = avgSteps,
        waterMl = avgWater,
        proteinGr = avgProtein,
        sleepMinutes = avgSleep,
        stepsProgress = (avgSteps / stepsTarget.toFloat()).coerceIn(0f, 1f),
        waterProgress = (avgWater / waterTarget.toFloat()).coerceIn(0f, 1f),
        proteinProgress = (avgProtein / proteinTarget.toFloat()).coerceIn(0f, 1f),
        sleepProgress = (avgSleep / sleepTarget.toFloat()).coerceIn(0f, 1f)
    )
}

@Composable
private fun GunumBenScreen(viewModel: MainViewModel, onGoalsCreated: () -> Unit) {
    val profile by viewModel.profilePreferences.collectAsState()
    val menstrualLogs by viewModel.menstrualCycleLogs.collectAsState()
    var ageText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    val genderKeys = listOf("male", "female", "unspecified")
    var genderIndex by remember { mutableStateOf(2) }
    val weightGoalModes = listOf(WeightGoalMode.REACH_IDEAL, WeightGoalMode.MAINTAIN)
    var selectedWeightGoalModeIndex by remember { mutableStateOf(0) }
    var showResetGoalsDialog by remember { mutableStateOf(false) }
    var isCreatingGoals by remember { mutableStateOf(false) }
    var selectedPeriodStartDate by remember { mutableStateOf(menstrualLogs.firstOrNull()?.periodStartDate) }
    val context = LocalContext.current

    LaunchedEffect(profile, menstrualLogs) {
        if (ageText.isBlank()) {
            ageText = profile.age.takeIf { it > 0 }?.toString().orEmpty()
        }
        if (heightText.isBlank()) {
            heightText = profile.heightCm.takeIf { it > 0 }?.toString().orEmpty()
        }
        if (weightText.isBlank()) {
            weightText = profile.weightKg.takeIf { it > 0f }?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }.orEmpty()
        }
        if (genderIndex == 2) {
            genderIndex = genderKeys.indexOf(profile.gender).takeIf { it >= 0 } ?: 2
        }
        if (selectedPeriodStartDate == null) {
            selectedPeriodStartDate = menstrualLogs.firstOrNull()?.periodStartDate
        }
    }

    val age = ageText.toIntOrNull()
    val height = heightText.toIntOrNull()
    val weight = weightText.toFloatOrNull()
    val canCalculate = age != null && height != null && weight != null && age > 0 && height > 0 && weight > 0
    val selectedGender = genderKeys[genderIndex]
    val selectedWeightGoalMode = weightGoalModes[selectedWeightGoalModeIndex]
    val targets = if (canCalculate) viewModel.buildPersonalizedTargets(age!!, height!!, weight!!, selectedGender, selectedWeightGoalMode) else null

    LaunchedEffect(age, height, weight, selectedGender) {
        if (age != null && height != null && weight != null && age > 0 && height > 0 && weight > 0f) {
            viewModel.saveProfile(age, height, weight, selectedGender)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.me_intro), style = MaterialTheme.typography.bodyMedium)
        }
        item {
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.me_age)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        item {
            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.me_height_cm)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        item {
            OutlinedTextField(
                value = weightText,
                onValueChange = { value -> weightText = value.filter { it.isDigit() || it == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.me_weight_kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        item {
            ExposedDropdownSimple(
                label = stringResource(R.string.me_gender),
                options = listOf(
                    stringResource(R.string.gender_male),
                    stringResource(R.string.gender_female),
                    stringResource(R.string.gender_unspecified)
                ),
                selected = genderIndex,
                onSelect = { genderIndex = it }
            )
        }
        if (selectedGender == "female") {
            item {
                val initial = selectedPeriodStartDate ?: java.time.LocalDate.now()
                OutlinedTextField(
                    value = selectedPeriodStartDate?.toString() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val picked = java.time.LocalDate.of(y, m + 1, d)
                                    selectedPeriodStartDate = picked
                                    viewModel.addMenstrualCycleLog(picked)
                                },
                                initial.year,
                                initial.monthValue - 1,
                                initial.dayOfMonth
                            ).show()
                        },
                    label = { Text(stringResource(R.string.me_menstrual_start_date)) },
                    trailingIcon = {
                        TextButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val picked = java.time.LocalDate.of(y, m + 1, d)
                                    selectedPeriodStartDate = picked
                                    viewModel.addMenstrualCycleLog(picked)
                                },
                                initial.year,
                                initial.monthValue - 1,
                                initial.dayOfMonth
                            ).show()
                        }) {
                            Text(stringResource(R.string.me_select_date))
                        }
                    }
                )
            }
        }
        item {
            ExposedDropdownSimple(
                label = stringResource(R.string.me_goal_mode_label),
                options = listOf(
                    stringResource(R.string.me_goal_mode_reach_ideal),
                    stringResource(R.string.me_goal_mode_maintain)
                ),
                selected = selectedWeightGoalModeIndex,
                onSelect = { selectedWeightGoalModeIndex = it }
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canCalculate && !isCreatingGoals,
                onClick = {
                    if (canCalculate) {
                        showResetGoalsDialog = true
                    }
                }
            ) {
                if (isCreatingGoals) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.me_create_goals))
                }
            }
        }

        targets?.let { t ->
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.me_targets_title), style = MaterialTheme.typography.titleMedium)

                        GoalTargetSection(title = stringResource(R.string.goal_section_basic)) {
                            GoalTargetRow(stringResource(R.string.card_steps), "${t.steps}")
                            GoalTargetRow(stringResource(R.string.card_sleep), formatSleepDurationLabel(t.sleepMinutes))
                            GoalTargetRow(stringResource(R.string.card_water), "${t.waterMl} ml")
                            GoalTargetRow(stringResource(R.string.me_target_ideal_weight), stringResource(R.string.me_target_ideal_weight_value, t.idealWeightKg))
                            GoalTargetRow(stringResource(R.string.me_target_plan_type), weightPlanSummaryLabel(t.weightPlanSummary))
                        }

                        GoalTargetSection(title = stringResource(R.string.tab_macros)) {
                            GoalTargetRow(stringResource(R.string.nutrient_protein), "${t.proteinGrams.toInt()} g")
                            GoalTargetRow(stringResource(R.string.nutrient_carbs), "${t.carbsGrams.toInt()} g")
                            GoalTargetRow(stringResource(R.string.nutrient_fat), "${t.fatGrams.toInt()} g")
                            GoalTargetRow(stringResource(R.string.nutrient_fiber), "${t.fiberGrams.toInt()} g")
                        }

                        GoalTargetSection(title = stringResource(R.string.tab_micros)) {
                            GoalTargetRow(stringResource(R.string.nutrient_iron), "${t.ironMg} mg")
                            GoalTargetRow(stringResource(R.string.nutrient_magnesium), "${t.magnesiumMg} mg")
                            GoalTargetRow(stringResource(R.string.nutrient_potassium), "${t.potassiumMg} mg")
                            GoalTargetRow(stringResource(R.string.nutrient_vitamin_d), "${t.vitaminDIu} IU")
                            GoalTargetRow(stringResource(R.string.nutrient_omega3), "${t.omega3Mg} mg")
                        }
                    }
                }
            }
        }
    }

    if (showResetGoalsDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreatingGoals) showResetGoalsDialog = false },
            title = { Text(stringResource(R.string.me_reset_goals_title)) },
            text = { Text(stringResource(R.string.me_reset_goals_message)) },
            confirmButton = {
                TextButton(onClick = {
                    if (!canCalculate || isCreatingGoals) return@TextButton
                    isCreatingGoals = true
                    viewModel.createPersonalizedGoals(age!!, height!!, weight!!, selectedGender, selectedWeightGoalMode) { success ->
                        isCreatingGoals = false
                        showResetGoalsDialog = false
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.goals_created_message), Toast.LENGTH_SHORT).show()
                            onGoalsCreated()
                        } else {
                            Toast.makeText(context, context.getString(R.string.goals_create_error_message), Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { if (!isCreatingGoals) showResetGoalsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}



@Composable
private fun weightPlanSummaryLabel(plan: String): String = when (plan) {
    "gain" -> stringResource(R.string.me_weight_plan_gain)
    "lose" -> stringResource(R.string.me_weight_plan_lose)
    else -> stringResource(R.string.me_weight_plan_maintain)
}

@Composable
private fun GoalTargetSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            HorizontalDivider(color = Color(0xFFE5E5EE))
            content()
        }
    }
}

@Composable
private fun GoalTargetRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private enum class GoalCadence { HOURLY, DAILY, WEEKLY }

@Composable
private fun GunumHedeflerScreen(viewModel: MainViewModel) {
    val selectedDate by viewModel.selectedGoalsDate.collectAsState()
    val dashboard by viewModel.goalsDashboard.collectAsState()
    val goals by viewModel.goalPlans.collectAsState()
    val foods by viewModel.foods.collectAsState()
    val goalsMeals by viewModel.goalsMealEntries.collectAsState()
    val foodsById = remember(foods) { foods.associateBy { it.id } }
    val consumedTotals = remember(goalsMeals, foodsById) {
        goalsMeals.fold(NutrientTotals()) { acc, meal ->
            val n = foodsById[meal.foodId]?.let { nutrientByGrams(it, meal.grams) } ?: NutrientTotals()
            acc.copy(
                protein = acc.protein + n.protein,
                carbs = acc.carbs + n.carbs,
                fat = acc.fat + n.fat,
                fiber = acc.fiber + n.fiber,
                iron = acc.iron + n.iron,
                magnesium = acc.magnesium + n.magnesium,
                potassium = acc.potassium + n.potassium,
                vitaminD = acc.vitaminD + n.vitaminD,
                omega3 = acc.omega3 + n.omega3
            )
        }
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalPlanItem?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalPlanItem?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NutritionDatePickerCard(
                selectedDate = selectedDate,
                selectedDateText = selectedDate.format(dateFormatter),
                onDateSelected = { viewModel.setSelectedGoalsDate(it) }
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.tab_tasks), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showAddDialog = true }) {
                    Text(stringResource(R.string.goal_add_link))
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.goals_empty_message))
                        TextButton(onClick = { showAddDialog = true }) {
                            Text(stringResource(R.string.goal_add_link))
                        }
                    }
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                val progress = goalProgress(goal, dashboard, consumedTotals)
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable { goalToEdit = goal }) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(goalTypeLabel(goal.goalType), style = MaterialTheme.typography.titleSmall)
                        if (isActivityGoalType(goal.goalType)) {
                            Text(goalActivityLabel(goal.goalType), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(stringResource(R.string.goal_frequency_label, cadenceLabel(goal.cadence)))
                        Text(goalProgressLabel(goal.goalType, progress.current, goal.target))
                        AnimatedProgressBar(
                            target = if (goal.target == 0) 0f else (progress.current / goal.target.toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            title = stringResource(R.string.goal_add_link),
            onDismiss = { showAddDialog = false },
            onSave = { typeKey, target, cadence ->
                viewModel.addGoalPlan(typeKey, target, cadence)
                val userGoalType = goalTypeToUserGoalKey(typeKey)
                if (userGoalType in listOf("water", "steps", "protein", "carbs", "fat", "fiber", "sleep")) {
                    viewModel.updateGoal(userGoalType, target)
                }
                showAddDialog = false
            }
        )
    }

    goalToEdit?.let { current ->
        AddGoalDialog(
            title = stringResource(R.string.goal_edit_title),
            initialGoalType = current.goalType,
            initialTarget = current.target,
            initialCadence = current.cadence,
            initialActivityType = current.goalType,
            onDismiss = { goalToEdit = null },
            onSave = { typeKey, target, cadence ->
                viewModel.updateGoalPlan(current.id, typeKey, target, cadence)
                val userGoalType = goalTypeToUserGoalKey(typeKey)
                if (userGoalType in listOf("water", "steps", "protein", "carbs", "fat", "fiber", "sleep")) {
                    viewModel.updateGoal(userGoalType, target)
                }
                goalToEdit = null
            },
            onDelete = {
                goalToEdit = null
                goalToDelete = current
            }
        )
    }

    goalToDelete?.let { current ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text(stringResource(R.string.goal_delete_title)) },
            text = { Text(stringResource(R.string.goal_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGoalPlan(current.id)
                    goalToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun GunumHatirlatmalarScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<com.leosoft.longevity.data.local.entity.ReminderLogEntity?>(null) }
    var reminderToDelete by remember { mutableStateOf<com.leosoft.longevity.data.local.entity.ReminderLogEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (reminders.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.reminders_empty_message))
                        TextButton(onClick = { showAddDialog = true }) { Text(stringResource(R.string.reminder_add_link)) }
                    }
                }
            }
        } else {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.tab_reminders), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAddDialog = true }) { Text(stringResource(R.string.reminder_add_link)) }
                }
            }
            items(reminders, key = { it.id }) { reminder ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().clickable { reminderToEdit = reminder }
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(reminder.reminderType, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (reminder.cadence == "hourly") stringResource(R.string.reminder_hourly_every, reminder.intervalHours ?: 1)
                            else stringResource(R.string.reminder_daily_at, reminder.reminderTime)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            title = stringResource(R.string.reminder_add_link),
            onDismiss = { showAddDialog = false },
            onSave = { title, cadence, dailyTime, interval ->
                scope.launch {
                    val id = viewModel.addReminder(title, dailyTime, cadence, interval)
                    ReminderAlarmScheduler.schedule(context, id, title, cadence, dailyTime, interval ?: 1)
                    showAddDialog = false
                }
            }
        )
    }

    reminderToEdit?.let { current ->
        AddReminderDialog(
            title = stringResource(R.string.reminder_edit_title),
            initialTitle = current.reminderType,
            initialCadence = current.cadence,
            initialDailyTime = current.reminderTime,
            initialIntervalHours = current.intervalHours ?: 1,
            onDismiss = { reminderToEdit = null },
            onSave = { title, cadence, dailyTime, interval ->
                viewModel.updateReminder(current.id, title, dailyTime, cadence, interval)
                ReminderAlarmScheduler.schedule(context, current.id, title, cadence, dailyTime, interval ?: 1)
                reminderToEdit = null
            },
            onDelete = {
                reminderToEdit = null
                reminderToDelete = current
            }
        )
    }

    reminderToDelete?.let { current ->
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = { Text(stringResource(R.string.reminder_delete_title)) },
            text = { Text(stringResource(R.string.reminder_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReminder(current.id)
                    ReminderAlarmScheduler.cancel(context, current.id)
                    reminderToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}


@Composable
private fun AddReminderDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int?) -> Unit,
    initialTitle: String = "",
    initialCadence: String = "daily",
    initialDailyTime: String = "09:00",
    initialIntervalHours: Int = 1,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var reminderTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
    var cadence by remember(initialCadence) { mutableStateOf(initialCadence) }
    var dailyTime by remember(initialDailyTime) { mutableStateOf(initialDailyTime) }
    var intervalText by remember(initialIntervalHours) { mutableStateOf(initialIntervalHours.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = reminderTitle,
                    onValueChange = { reminderTitle = it },
                    label = { Text(stringResource(R.string.reminder_type)) },
                    singleLine = true
                )
                ExposedDropdownSimple(
                    label = stringResource(R.string.reminder_cadence_label),
                    options = listOf(stringResource(R.string.reminder_daily), stringResource(R.string.reminder_hourly)),
                    selected = if (cadence == "daily") 0 else 1,
                    onSelect = { cadence = if (it == 0) "daily" else "hourly" }
                )
                if (cadence == "daily") {
                    val openTimePicker = {
                        val parts = dailyTime.split(":")
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(context, { _, h, m -> dailyTime = String.format("%02d:%02d", h, m) }, hour, minute, true).show()
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dailyTime,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.reminder_pick_time)) }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { openTimePicker() }
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { intervalText = it },
                        label = { Text(stringResource(R.string.reminder_interval_hours)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val interval = if (cadence == "hourly") (intervalText.toIntOrNull() ?: 1).coerceAtLeast(1) else null
                onSave(reminderTitle.ifBlank { context.getString(R.string.reminder_default_title) }, cadence, dailyTime, interval)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onDelete?.let { TextButton(onClick = it) { Text(stringResource(R.string.delete)) } }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

private data class GoalProgress(val current: Int)

@Composable
private fun AddGoalDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, Int, String) -> Unit,
    initialGoalType: String = "water",
    initialTarget: Int? = null,
    initialCadence: String = "daily",
    initialActivityType: String = WorkoutType.WALKING.name.lowercase(),
    onDelete: (() -> Unit)? = null
) {
    val goalTypeKeys = listOf("water", "activity", "protein", "carbs", "fat", "fiber", "sleep", "iron", "magnesium", "potassium", "vitamin_d", "omega3")
    val activityTypeKeys = WorkoutType.entries.filter { it != WorkoutType.OTHER }
    val initialActivityIndex = activityTypeKeys.indexOfFirst { it.name.lowercase() == initialActivityType }
    var selectedActivityIdx by remember(initialActivityType) { mutableStateOf(initialActivityIndex.takeIf { it >= 0 } ?: 0) }
    val cadenceKeys = listOf("hourly", "daily", "weekly")
    val initialGoalTypeKey = if (initialGoalType == "steps" || isActivityGoalType(initialGoalType)) "activity" else initialGoalType
    var selectedTypeIdx by remember(initialGoalTypeKey) { mutableStateOf(goalTypeKeys.indexOf(initialGoalTypeKey).takeIf { it >= 0 } ?: 0) }
    var selectedCadenceIdx by remember(initialCadence) { mutableStateOf(cadenceKeys.indexOf(initialCadence).takeIf { it >= 0 } ?: 1) }
    var targetText by remember(initialTarget) { mutableStateOf(initialTarget?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownSimple(
                    label = stringResource(R.string.goal_type),
                    options = goalTypeKeys.map { goalTypeLabel(it) },
                    selected = selectedTypeIdx,
                    onSelect = { idx -> selectedTypeIdx = idx }
                )
                Text(goalTargetHintLabel(goalTypeKeys[selectedTypeIdx]), style = MaterialTheme.typography.bodySmall)
                if (goalTypeKeys[selectedTypeIdx] == "activity") {
                    ExposedDropdownSimple(
                        label = stringResource(R.string.activity_type),
                        options = activityTypeKeys.map { stringResource(workoutTypeLabel(it)) },
                        selected = selectedActivityIdx,
                        onSelect = { idx -> selectedActivityIdx = idx }
                    )
                }
                ExposedDropdownSimple(
                    label = stringResource(R.string.goal_frequency),
                    options = cadenceKeys.map { cadenceLabel(it) },
                    selected = selectedCadenceIdx,
                    onSelect = { idx -> selectedCadenceIdx = idx }
                )
                OutlinedTextField(value = targetText, onValueChange = { targetText = it }, label = { Text(stringResource(R.string.target_value)) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = targetText.toIntOrNull() ?: return@TextButton
                val selectedGoalType = goalTypeKeys[selectedTypeIdx]
                val resolvedGoalType = if (selectedGoalType == "activity") activityTypeKeys[selectedActivityIdx].name.lowercase() else selectedGoalType
                onSave(resolvedGoalType, target, cadenceKeys[selectedCadenceIdx])
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onDelete?.let {
                    TextButton(onClick = it) { Text(stringResource(R.string.delete)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

@Composable
private fun goalTypeLabel(type: String): String = when {
    type == "water" -> stringResource(R.string.goal_type_water)
    type == "steps" || type == "activity" || isActivityGoalType(type) -> stringResource(R.string.goal_type_activity)
    type == "protein" -> stringResource(R.string.goal_type_protein)
    type == "carbs" -> stringResource(R.string.nutrient_carbs)
    type == "fat" -> stringResource(R.string.nutrient_fat)
    type == "fiber" -> stringResource(R.string.nutrient_fiber)
    type == "sleep" -> stringResource(R.string.goal_type_sleep)
    type == "iron" -> stringResource(R.string.nutrient_iron)
    type == "magnesium" -> stringResource(R.string.nutrient_magnesium)
    type == "potassium" -> stringResource(R.string.nutrient_potassium)
    type == "vitamin_d" -> stringResource(R.string.nutrient_vitamin_d)
    type == "omega3" -> stringResource(R.string.nutrient_omega3)
    else -> type
}

@Composable
private fun goalTargetHintLabel(type: String): String = when {
    type == "water" -> stringResource(R.string.goal_hint_water)
    type == "steps" || type == "activity" || isActivityGoalType(type) -> stringResource(R.string.goal_hint_activity)
    type == "protein" -> stringResource(R.string.goal_hint_protein)
    type in listOf("carbs", "fat", "fiber") -> stringResource(R.string.goal_hint_macros)
    type == "sleep" -> stringResource(R.string.goal_hint_sleep)
    type in listOf("iron", "magnesium", "potassium", "vitamin_d", "omega3") -> stringResource(R.string.goal_hint_micros)
    else -> ""
}


private fun isActivityGoalType(type: String): Boolean =
    type in WorkoutType.entries.filter { it != WorkoutType.OTHER }.map { it.name.lowercase() }

private fun goalTypeToUserGoalKey(type: String): String = if (type == "steps" || type == "activity" || isActivityGoalType(type)) "steps" else type

@Composable
private fun goalActivityLabel(type: String): String {
    val workoutType = WorkoutType.entries.firstOrNull { it.name.equals(type, ignoreCase = true) }
    return if (workoutType != null) {
        stringResource(workoutTypeLabel(workoutType))
    } else {
        ""
    }
}

@Composable
private fun cadenceLabel(cadence: String): String = when (cadence) {
    "hourly" -> stringResource(R.string.goal_frequency_hourly)
    "daily" -> stringResource(R.string.goal_frequency_daily)
    "weekly" -> stringResource(R.string.goal_frequency_weekly)
    else -> cadence
}

@Composable
private fun goalProgressLabel(goalType: String, current: Int, target: Int): String {
    val unit = goalUnit(goalType)
    val currentLabel = formatGoalValue(goalType, current)
    val targetLabel = formatGoalValue(goalType, target)
    return if (unit.isBlank()) {
        stringResource(R.string.goal_progress_text_plain, currentLabel, targetLabel)
    } else {
        stringResource(R.string.goal_progress_text_with_unit, currentLabel, targetLabel, unit)
    }
}

private fun goalUnit(goalType: String): String = when (goalType) {
    "water" -> "ml"
    "steps", "activity" -> "adım"
    "protein", "carbs", "fat", "fiber" -> "g"
    "sleep" -> ""
    "iron", "magnesium", "potassium", "omega3" -> "mg"
    "vitamin_d" -> "IU"
    else -> if (isActivityGoalType(goalType)) "dk" else ""
}

private fun formatGoalValue(goalType: String, value: Int): String = when (goalType) {
    "sleep" -> formatSleepDurationLabel(value)
    else -> value.toString()
}

private fun formatSleepDurationLabel(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val remainMinutes = safe % 60
    return when {
        hours == 0 -> "$remainMinutes dakika"
        remainMinutes == 0 -> "$hours saat"
        else -> "$hours saat $remainMinutes dakika"
    }
}

private fun goalProgress(goal: GoalPlanItem, dashboard: com.leosoft.longevity.domain.model.DashboardSummary?, consumed: NutrientTotals): GoalProgress {
    val current = when {
        goal.goalType == "water" -> dashboard?.waterMl ?: 0
        goal.goalType == "steps" || goal.goalType == "activity" || isActivityGoalType(goal.goalType) -> {
            if (goal.goalType == "steps" || goal.goalType == "activity") {
                dashboard?.steps ?: 0
            } else {
                dashboard?.workoutMinutesByType?.get(goal.goalType) ?: 0
            }
        }
        goal.goalType == "protein" -> consumed.protein.toInt()
        goal.goalType == "carbs" -> consumed.carbs.toInt()
        goal.goalType == "fat" -> consumed.fat.toInt()
        goal.goalType == "fiber" -> consumed.fiber.toInt()
        goal.goalType == "sleep" -> dashboard?.sleepMinutes ?: 0
        goal.goalType == "iron" -> consumed.iron.toInt()
        goal.goalType == "magnesium" -> consumed.magnesium.toInt()
        goal.goalType == "potassium" -> consumed.potassium.toInt()
        goal.goalType == "vitamin_d" -> consumed.vitaminD.toInt()
        goal.goalType == "omega3" -> consumed.omega3.toInt()
        else -> 0
    }
    return GoalProgress(current = current)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var foodsReady by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.ensureCoreFoods().join()
        foodsReady = true
    }
    val nutritiousFoods by viewModel.nutritiousFoods.collectAsState()
    val allFoods by viewModel.foods.collectAsState()
    val foods = if (nutritiousFoods.isNotEmpty()) nutritiousFoods else allFoods
    val supplements by viewModel.supplements.collectAsState()
    var type by remember { mutableStateOf<QuickAddType?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var selectedFoodId by remember { mutableStateOf<Long?>(null) }
    var customFoodName by remember { mutableStateOf("") }
    var selectedSupplementId by remember { mutableLongStateOf(supplements.firstOrNull()?.id ?: 0L) }
    var amountText by remember { mutableStateOf("") }
    var secondaryText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var selectedWorkoutType by remember { mutableStateOf(WorkoutType.WALKING) }
    var customActivityName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_record_cta)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = type?.let { stringResource(typeLabel(it)) } ?: stringResource(R.string.select_prompt),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.what_to_add)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        QuickAddType.entries.forEach {
                            DropdownMenuItem(text = { Text(stringResource(typeLabel(it))) }, onClick = { type = it; expanded = false })
                        }
                    }
                }

                when (type) {
                    null -> Text(stringResource(R.string.select_first_hint), style = MaterialTheme.typography.bodySmall)
                    QuickAddType.FOOD -> {
                        if (!foodsReady && foods.isEmpty()) {
                            Text(stringResource(R.string.foods_loading_hint), style = MaterialTheme.typography.bodySmall)
                        }
                        ExposedDropdownSimple(
                            label = stringResource(R.string.food_list_label),
                            options = listOf(stringResource(R.string.select_prompt)) + foods.map { it.name },
                            selected = foods.indexOfFirst { it.id == selectedFoodId }.takeIf { it >= 0 }?.plus(1) ?: 0,
                            onSelect = { idx -> selectedFoodId = if (idx == 0) null else foods[idx - 1].id }
                        )
                        OutlinedTextField(value = customFoodName, onValueChange = { customFoodName = it }, label = { Text(stringResource(R.string.food_name_custom_optional)) })
                        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(stringResource(R.string.grams)) })
                    }
                    QuickAddType.WATER -> OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(stringResource(R.string.water_ml_input)) })
                    QuickAddType.SUPPLEMENT -> ExposedDropdownSimple(
                        label = stringResource(R.string.supplement_name),
                        options = supplements.map { it.name },
                        selected = supplements.indexOfFirst { it.id == selectedSupplementId }.coerceAtLeast(0),
                        onSelect = { idx -> selectedSupplementId = supplements[idx].id }
                    )
                    QuickAddType.SLEEP -> {
                        val now = java.time.LocalTime.now()
                        val bed = amountText.takeIf { it.contains(":") } ?: String.format("%02d:%02d", now.hour, now.minute)
                        val wake = secondaryText.takeIf { it.contains(":") } ?: String.format("%02d:%02d", now.hour, now.minute)

                        fun openBedTimePicker() {
                            val parts = bed.split(":")
                            TimePickerDialog(
                                context,
                                { _, h, m -> amountText = String.format("%02d:%02d", h, m) },
                                parts[0].toInt(),
                                parts[1].toInt(),
                                true
                            ).show()
                        }

                        fun openWakeTimePicker() {
                            val parts = wake.split(":")
                            TimePickerDialog(
                                context,
                                { _, h, m -> secondaryText = String.format("%02d:%02d", h, m) },
                                parts[0].toInt(),
                                parts[1].toInt(),
                                true
                            ).show()
                        }

                        OutlinedTextField(
                            value = bed,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { openBedTimePicker() },
                            label = { Text(stringResource(R.string.bed_time)) },
                            trailingIcon = {
                                TextButton(onClick = { openBedTimePicker() }) {
                                    Text(stringResource(R.string.select_time))
                                }
                            }
                        )
                        OutlinedTextField(
                            value = wake,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { openWakeTimePicker() },
                            label = { Text(stringResource(R.string.wake_time)) },
                            trailingIcon = {
                                TextButton(onClick = { openWakeTimePicker() }) {
                                    Text(stringResource(R.string.select_time))
                                }
                            }
                        )
                    }
                    QuickAddType.ACTIVITY -> {
                        val isStepBased = selectedWorkoutType == WorkoutType.WALKING || selectedWorkoutType == WorkoutType.RUNNING
                        ExposedDropdownSimple(
                            label = stringResource(R.string.activity_type),
                            options = WorkoutType.entries.map { stringResource(workoutTypeLabel(it)) },
                            selected = WorkoutType.entries.indexOf(selectedWorkoutType),
                            onSelect = { idx -> selectedWorkoutType = WorkoutType.entries[idx] }
                        )
                        OutlinedTextField(value = customActivityName, onValueChange = { customActivityName = it }, label = { Text(stringResource(R.string.custom_activity_name_optional)) })
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text(if (isStepBased) stringResource(R.string.steps_input) else stringResource(R.string.duration_min)) }
                        )
                        OutlinedTextField(
                            value = secondaryText,
                            onValueChange = { secondaryText = it },
                            label = { Text(if (isStepBased) stringResource(R.string.distance_km_optional) else stringResource(R.string.intensity_1_3)) }
                        )
                    }
                }
                OutlinedTextField(value = notesText, onValueChange = { notesText = it }, label = { Text(stringResource(R.string.notes_optional)) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (type) {
                    null -> Unit
                    QuickAddType.FOOD -> viewModel.addMealWithOptionalCustomFood(selectedFoodId, customFoodName, amountText.toIntOrNull() ?: 0, MealType.SNACK)
                    QuickAddType.WATER -> viewModel.addWater(amountText.toIntOrNull() ?: 0)
                    QuickAddType.SUPPLEMENT -> viewModel.addSupplementLog(selectedSupplementId)
                    QuickAddType.SLEEP -> {
                        val now = java.time.LocalTime.now()
                        val bed = amountText.takeIf { it.contains(":") } ?: String.format("%02d:%02d", now.hour, now.minute)
                        val wake = secondaryText.takeIf { it.contains(":") } ?: String.format("%02d:%02d", now.hour, now.minute)
                        viewModel.addSleepLog(bed, wake)
                    }
                    QuickAddType.ACTIVITY -> {
                        val resolvedType = if (customActivityName.isNotBlank()) WorkoutType.OTHER else selectedWorkoutType
                        val isStepBased = resolvedType == WorkoutType.WALKING || resolvedType == WorkoutType.RUNNING
                        val mergedNotes = listOf(
                            customActivityName.takeIf { it.isNotBlank() },
                            secondaryText.takeIf { it.isNotBlank() && isStepBased }?.let { context.getString(R.string.distance_km_note, it) },
                            notesText.takeIf { it.isNotBlank() }
                        ).joinToString(" | ")
                        if (isStepBased) {
                            viewModel.addSteps(amountText.toIntOrNull() ?: 0)
                        } else {
                            viewModel.addWorkout(resolvedType, amountText.toIntOrNull() ?: 0, secondaryText.toIntOrNull() ?: 1, mergedNotes)
                        }
                    }
                }
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownSimple(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    if (options.isEmpty()) return
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            readOnly = true,
            value = options[selected.coerceIn(0, options.lastIndex)],
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelect(index); expanded = false })
            }
        }
    }
}

private fun typeLabel(type: QuickAddType): Int = when (type) {
    QuickAddType.FOOD -> R.string.add_type_food
    QuickAddType.WATER -> R.string.add_type_water
    QuickAddType.SUPPLEMENT -> R.string.add_type_supplement
    QuickAddType.SLEEP -> R.string.add_type_sleep
    QuickAddType.ACTIVITY -> R.string.add_type_activity
}

private fun workoutTypeLabel(type: WorkoutType): Int = when (type) {
    WorkoutType.ELLIPTICAL -> R.string.workout_elliptical
    WorkoutType.PILATES -> R.string.workout_pilates
    WorkoutType.WALKING -> R.string.workout_walking
    WorkoutType.RUNNING -> R.string.workout_running
    WorkoutType.STRENGTH -> R.string.workout_strength
    WorkoutType.YOGA -> R.string.workout_yoga
    WorkoutType.OTHER -> R.string.workout_other
}

private data class NutrientTotals(
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val iron: Float = 0f,
    val magnesium: Float = 0f,
    val potassium: Float = 0f,
    val vitaminD: Float = 0f,
    val omega3: Float = 0f
)

private fun nutrientByGrams(food: FoodEntity, grams: Int): NutrientTotals {
    val ratio = grams / 100f
    return NutrientTotals(
        protein = food.protein * ratio,
        carbs = food.carbs * ratio,
        fat = food.fat * ratio,
        fiber = food.fiber * ratio,
        iron = food.ironMg * ratio,
        magnesium = food.magnesiumMg * ratio,
        potassium = food.potassiumMg * ratio,
        vitaminD = food.vitaminDUi * ratio,
        omega3 = food.omega3Mg * ratio
    )
}

@Composable
fun BeslenmeKayitScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val foods by viewModel.foods.collectAsState()
    val foodsById = remember(foods) { foods.associateBy { it.id } }
    val meals by viewModel.mealEntries.collectAsState()
    val allMeals by viewModel.allMealEntries.collectAsState()
    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val mealsSorted = remember(meals) { meals.sortedByDescending { it.time } }
    var range by remember { mutableStateOf(NutritionChartRange.DAILY) }
    val caloriesChartData = remember(allMeals, foodsById, range) { buildCaloriesChartData(allMeals, foodsById, range) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }

    var mealToEdit by remember { mutableStateOf<com.leosoft.longevity.data.local.entity.MealEntryEntity?>(null) }
    var mealToDelete by remember { mutableStateOf<com.leosoft.longevity.data.local.entity.MealEntryEntity?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            viewModel.setSelectedNutritionDate(java.time.LocalDate.of(year, month + 1, dayOfMonth))
                        },
                        selectedDate.year,
                        selectedDate.monthValue - 1,
                        selectedDate.dayOfMonth
                    ).show()
                }) {
                    Text(stringResource(R.string.nutrition_selected_date, selectedDate.format(dateFormatter)))
                }
            }
        }
        item {
            NutritionTrendChartCard(
                title = stringResource(R.string.tab_log),
                range = range,
                onRangeChange = { range = it },
                unit = "kcal",
                data = caloriesChartData
            )
        }

        if (mealsSorted.isEmpty()) {
            item {
                EmptyDateRecordCard(stringResource(R.string.nutrition_no_records_for_date))
            }
        }

        items(mealsSorted, key = { it.id }) { entry ->
            val food = foodsById[entry.foodId]
            val n = food?.let { nutrientByGrams(it, entry.grams) }
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().clickable { mealToEdit = entry }
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${food?.name ?: stringResource(R.string.unknown_food)} • ${entry.grams} g", style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.record_date_time, entry.time.format(dateTimeFormatter)), style = MaterialTheme.typography.bodySmall)
                    if (n != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            NutrientChip(label = stringResource(R.string.nutrient_protein), value = stringResource(R.string.nutrient_grams_value, n.protein), modifier = Modifier.weight(1f))
                            NutrientChip(label = stringResource(R.string.nutrient_carbs), value = stringResource(R.string.nutrient_grams_value, n.carbs), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            NutrientChip(label = stringResource(R.string.nutrient_fat), value = stringResource(R.string.nutrient_grams_value, n.fat), modifier = Modifier.weight(1f))
                            NutrientChip(label = stringResource(R.string.nutrient_fiber), value = stringResource(R.string.nutrient_grams_value, n.fiber), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            NutrientChip(label = stringResource(R.string.nutrient_iron), value = stringResource(R.string.nutrient_mg_value, n.iron), modifier = Modifier.weight(1f))
                            NutrientChip(label = stringResource(R.string.nutrient_magnesium), value = stringResource(R.string.nutrient_mg_value, n.magnesium), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            NutrientChip(label = stringResource(R.string.nutrient_potassium), value = stringResource(R.string.nutrient_mg_value, n.potassium), modifier = Modifier.weight(1f))
                            NutrientChip(label = stringResource(R.string.nutrient_vitamin_d), value = stringResource(R.string.nutrient_iu_value, n.vitaminD), modifier = Modifier.weight(1f))
                        }
                        NutrientChip(label = stringResource(R.string.nutrient_omega3), value = stringResource(R.string.nutrient_mg_value, n.omega3), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    mealToEdit?.let { entry ->
        MealEntryActionsDialog(
            onDismiss = { mealToEdit = null },
            onDelete = { selected ->
                mealToEdit = null
                mealToDelete = selected
            },
            currentMeal = entry,
            foods = foods,
            onSaveEdit = { updated ->
                viewModel.updateMealEntry(updated)
                mealToEdit = null
            }
        )
    }

    mealToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { mealToDelete = null },
            title = { Text(stringResource(R.string.delete_meal_title)) },
            text = { Text(stringResource(R.string.delete_meal_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMealEntry(entry)
                    mealToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { mealToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun NutrientChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6F5)), modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealEntryActionsDialog(
    onDismiss: () -> Unit,
    onDelete: (com.leosoft.longevity.data.local.entity.MealEntryEntity) -> Unit,
    currentMeal: com.leosoft.longevity.data.local.entity.MealEntryEntity,
    foods: List<FoodEntity>,
    onSaveEdit: (com.leosoft.longevity.data.local.entity.MealEntryEntity) -> Unit
) {
    var isEditing by remember(currentMeal.id) { mutableStateOf(false) }
    var selectedFoodId by remember(currentMeal.id) { mutableLongStateOf(currentMeal.foodId) }
    var gramsText by remember(currentMeal.id) { mutableStateOf(currentMeal.grams.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isEditing) R.string.edit_meal else R.string.meal_actions_title)) },
        text = {
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownSimple(
                        label = stringResource(R.string.food_list_label),
                        options = foods.map { it.name },
                        selected = foods.indexOfFirst { it.id == selectedFoodId }.coerceAtLeast(0),
                        onSelect = { idx -> selectedFoodId = foods[idx].id }
                    )
                    OutlinedTextField(
                        value = gramsText,
                        onValueChange = { gramsText = it },
                        label = { Text(stringResource(R.string.grams)) }
                    )
                }
            } else {
                Text(stringResource(R.string.meal_actions_hint))
            }
        },
        confirmButton = {
            if (isEditing) {
                TextButton(onClick = {
                    val grams = gramsText.toIntOrNull() ?: return@TextButton
                    onSaveEdit(currentMeal.copy(foodId = selectedFoodId, grams = grams))
                }) { Text(stringResource(R.string.save)) }
            } else {
                TextButton(onClick = { isEditing = true }) { Text(stringResource(R.string.edit)) }
            }
        },
        dismissButton = {
            if (isEditing) {
                TextButton(onClick = { isEditing = false }) { Text(stringResource(R.string.back)) }
            } else {
                TextButton(onClick = { onDelete(currentMeal) }) { Text(stringResource(R.string.delete)) }
            }
        }
    )
}

@Composable
fun BeslenmeMakrolarScreen(viewModel: MainViewModel) {
    val foods by viewModel.foods.collectAsState()
    val foodsById = remember(foods) { foods.associateBy { it.id } }
    val meals by viewModel.mealEntries.collectAsState()
    val allMeals by viewModel.allMealEntries.collectAsState()
    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    var range by remember { mutableStateOf(NutritionChartRange.DAILY) }
    val scopedMeals = remember(allMeals, selectedDate, range) { filterMealsByRange(allMeals, selectedDate, range) }
    val totals = remember(scopedMeals, foodsById) { CalculateMacroTotalsUseCase().invoke(scopedMeals, foodsById) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            NutritionDatePickerCard(
                selectedDate = selectedDate,
                selectedDateText = selectedDate.format(dateFormatter),
                onDateSelected = { viewModel.setSelectedNutritionDate(it) }
            )
        }
        item {
            if (meals.isEmpty()) {
                EmptyDateRecordCard(stringResource(R.string.nutrition_no_records_for_date))
            } else {
                val metrics = listOf(
                    NutritionMetric(stringResource(R.string.nutrient_protein), totals.protein.toInt(), TrendBarColor),
                    NutritionMetric(stringResource(R.string.nutrient_carbs), totals.carbs.toInt(), TrendBarColor),
                    NutritionMetric(stringResource(R.string.nutrient_fat), totals.fat.toInt(), TrendBarColor),
                    NutritionMetric(stringResource(R.string.nutrient_fiber), totals.fiber.toInt(), TrendBarColor)
                )
                NutritionMetricChartCard(
                    title = stringResource(R.string.tab_macros),
                    metrics = metrics,
                    unit = "g",
                    range = range,
                    onRangeChange = { range = it }
                )
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            NutrientChip(label = stringResource(R.string.nutrient_protein), value = stringResource(R.string.nutrient_grams_value, totals.protein), modifier = Modifier.weight(1f))
                            NutrientChip(label = stringResource(R.string.nutrient_carbs), value = stringResource(R.string.nutrient_grams_value, totals.carbs), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            NutrientChip(label = stringResource(R.string.nutrient_fat), value = stringResource(R.string.nutrient_grams_value, totals.fat), modifier = Modifier.weight(1f))
                            NutrientChip(label = stringResource(R.string.nutrient_fiber), value = stringResource(R.string.nutrient_grams_value, totals.fiber), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BeslenmeMikrolarScreen(viewModel: MainViewModel) {
    val foods by viewModel.foods.collectAsState()
    val foodsById = remember(foods) { foods.associateBy { it.id } }
    val meals by viewModel.mealEntries.collectAsState()
    val allMeals by viewModel.allMealEntries.collectAsState()
    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    var range by remember { mutableStateOf(NutritionChartRange.DAILY) }
    val scopedMeals = remember(allMeals, selectedDate, range) { filterMealsByRange(allMeals, selectedDate, range) }
    val total = scopedMeals.fold(NutrientTotals()) { acc, meal ->
        val n = foodsById[meal.foodId]?.let { nutrientByGrams(it, meal.grams) } ?: NutrientTotals()
        acc.copy(
            iron = acc.iron + n.iron,
            magnesium = acc.magnesium + n.magnesium,
            potassium = acc.potassium + n.potassium,
            vitaminD = acc.vitaminD + n.vitaminD,
            omega3 = acc.omega3 + n.omega3
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            NutritionDatePickerCard(
                selectedDate = selectedDate,
                selectedDateText = selectedDate.format(dateFormatter),
                onDateSelected = { viewModel.setSelectedNutritionDate(it) }
            )
        }
        item {
            if (meals.isEmpty()) {
                EmptyDateRecordCard(stringResource(R.string.nutrition_no_records_for_date))
            } else {
                val metrics = listOf(
                    NutritionMetric(stringResource(R.string.nutrient_iron), total.iron.toInt(), TrendBarColor),
                    NutritionMetric(stringResource(R.string.nutrient_magnesium), total.magnesium.toInt(), TrendBarColor),
                    NutritionMetric(stringResource(R.string.nutrient_potassium), total.potassium.toInt(), TrendBarColor),
                    NutritionMetric(stringResource(R.string.nutrient_omega3), total.omega3.toInt(), TrendBarColor)
                )
                NutritionMetricChartCard(
                    title = stringResource(R.string.tab_micros),
                    metrics = metrics,
                    unit = "mg",
                    range = range,
                    onRangeChange = { range = it }
                )
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            NutrientChip(label = stringResource(R.string.nutrient_iron), value = stringResource(R.string.nutrient_mg_value, total.iron), modifier = Modifier.weight(1f))
                            NutrientChip(label = stringResource(R.string.nutrient_magnesium), value = stringResource(R.string.nutrient_mg_value, total.magnesium), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            NutrientChip(label = stringResource(R.string.nutrient_potassium), value = stringResource(R.string.nutrient_mg_value, total.potassium), modifier = Modifier.weight(1f))
                            NutrientChip(label = stringResource(R.string.nutrient_vitamin_d), value = stringResource(R.string.nutrient_iu_value, total.vitaminD), modifier = Modifier.weight(1f))
                        }
                        NutrientChip(label = stringResource(R.string.nutrient_omega3), value = stringResource(R.string.nutrient_mg_value, total.omega3), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}


@Composable
fun BeslenmeSuScreen(viewModel: MainViewModel) {
    val logs by viewModel.waterLogs.collectAsState()
    val allLogs by viewModel.allWaterLogs.collectAsState()
    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }
    var range by remember { mutableStateOf(NutritionChartRange.DAILY) }
    val waterChartData = remember(allLogs, range) { buildWaterChartData(allLogs, range) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            NutritionDatePickerCard(
                selectedDate = selectedDate,
                selectedDateText = selectedDate.format(dateFormatter),
                onDateSelected = { viewModel.setSelectedNutritionDate(it) }
            )
        }
        item {
            NutritionTrendChartCard(
                title = stringResource(R.string.tab_water),
                range = range,
                onRangeChange = { range = it },
                unit = "ml",
                data = waterChartData
            )
        }

        if (logs.isEmpty()) {
            item { EmptyDateRecordCard(stringResource(R.string.water_no_records_for_date)) }
        } else {
            items(logs, key = { it.id }) { log ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.water_ml_logged, log.amountMl), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.record_date_time, log.time.format(dateTimeFormatter)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun BeslenmeTakviyelerScreen(viewModel: MainViewModel) {
    val logs by viewModel.supplementLogs.collectAsState()
    val allLogs by viewModel.allSupplementLogs.collectAsState()
    val supplements by viewModel.supplements.collectAsState()
    val supplementsById = remember(supplements) { supplements.associateBy { it.id } }
    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }
    var range by remember { mutableStateOf(NutritionChartRange.DAILY) }
    val supplementChartData = remember(allLogs, range) { buildSupplementChartData(allLogs, range) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F5FF)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            NutritionDatePickerCard(
                selectedDate = selectedDate,
                selectedDateText = selectedDate.format(dateFormatter),
                onDateSelected = { viewModel.setSelectedNutritionDate(it) }
            )
        }
        item {
            NutritionTrendChartCard(
                title = stringResource(R.string.tab_supplements),
                range = range,
                onRangeChange = { range = it },
                unit = "adet",
                data = supplementChartData
            )
        }

        if (logs.isEmpty()) {
            item { EmptyDateRecordCard(stringResource(R.string.supplement_no_records_for_date)) }
        } else {
            items(logs, key = { it.id }) { log ->
                val name = supplementsById[log.supplementId]?.name ?: stringResource(R.string.supplement_unknown)
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.record_date_time, log.time.format(dateTimeFormatter)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private data class NutritionMetric(val label: String, val value: Int, val color: Color)

@Composable
private fun NutritionMetricChartCard(
    title: String,
    metrics: List<NutritionMetric>,
    unit: String,
    range: NutritionChartRange,
    onRangeChange: (NutritionChartRange) -> Unit
) {
    val max = (metrics.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
    var reveal by remember(range, metrics) { mutableStateOf(false) }
    LaunchedEffect(range, metrics) { reveal = true }
    val revealFactor by animateFloatAsState(targetValue = if (reveal) 1f else 0f, animationSpec = tween(850), label = "nutrition-metric-reveal")
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().background(TrendChipBackgroundColor, RoundedCornerShape(16.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NutritionRangeChip(stringResource(R.string.life_sleep_range_daily), range == NutritionChartRange.DAILY) { onRangeChange(NutritionChartRange.DAILY) }
                NutritionRangeChip(stringResource(R.string.life_sleep_range_weekly), range == NutritionChartRange.WEEKLY) { onRangeChange(NutritionChartRange.WEEKLY) }
                NutritionRangeChip(stringResource(R.string.life_sleep_range_monthly), range == NutritionChartRange.MONTHLY) { onRangeChange(NutritionChartRange.MONTHLY) }
            }
            Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.forEach { m ->
                    val hBase = if (m.value <= 0) 0f else (12f + (90f * (m.value / max.toFloat())))
                    val h = (hBase * revealFactor).dp
                    val animatedH by animateDpAsState(targetValue = h, animationSpec = tween(650), label = "nutrition-metric-bar")
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("${m.value} $unit", style = MaterialTheme.typography.labelSmall, color = TrendValueTextColor)
                        Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
                            Box(modifier = Modifier.fillMaxWidth().height(animatedH).background(m.color, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)))
                        }
                        Text(m.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private enum class NutritionChartRange { DAILY, WEEKLY, MONTHLY }
private data class NutritionTrendPoint(val label: String, val value: Int)

@Composable
private fun NutritionTrendChartCard(
    title: String,
    range: NutritionChartRange,
    onRangeChange: (NutritionChartRange) -> Unit,
    unit: String,
    data: List<NutritionTrendPoint>
) {
    val max = (data.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
    var reveal by remember(range, data) { mutableStateOf(false) }
    LaunchedEffect(range, data) { reveal = true }
    val revealFactor by animateFloatAsState(targetValue = if (reveal) 1f else 0f, animationSpec = tween(850), label = "nutrition-trend-reveal")
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().background(TrendChipBackgroundColor, RoundedCornerShape(16.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NutritionRangeChip(stringResource(R.string.life_sleep_range_daily), range == NutritionChartRange.DAILY) { onRangeChange(NutritionChartRange.DAILY) }
                NutritionRangeChip(stringResource(R.string.life_sleep_range_weekly), range == NutritionChartRange.WEEKLY) { onRangeChange(NutritionChartRange.WEEKLY) }
                NutritionRangeChip(stringResource(R.string.life_sleep_range_monthly), range == NutritionChartRange.MONTHLY) { onRangeChange(NutritionChartRange.MONTHLY) }
            }
            Row(modifier = Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                data.forEach { p ->
                    val hBase = if (p.value <= 0) 0f else (12f + (96f * (p.value / max.toFloat())))
                    val h = (hBase * revealFactor).dp
                    val animatedH by animateDpAsState(targetValue = h, animationSpec = tween(650), label = "nutrition-trend-bar")
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("${p.value} $unit", style = MaterialTheme.typography.labelSmall, color = TrendValueTextColor, maxLines = 1)
                        Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
                            Box(modifier = Modifier.fillMaxWidth().height(animatedH).background(TrendBarColor, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)))
                        }
                        Text(p.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NutritionRangeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = if (selected) TrendChipSelectedTextColor else TrendChipDefaultTextColor)
    }
}

private fun filterMealsByRange(
    allMeals: List<com.leosoft.longevity.data.local.entity.MealEntryEntity>,
    anchor: LocalDate,
    range: NutritionChartRange
): List<com.leosoft.longevity.data.local.entity.MealEntryEntity> {
    return when (range) {
        NutritionChartRange.DAILY -> allMeals.filter { it.date == anchor }
        NutritionChartRange.WEEKLY -> {
            val start = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
            val end = start.plusDays(6)
            allMeals.filter { it.date >= start && it.date <= end }
        }
        NutritionChartRange.MONTHLY -> {
            val ym = YearMonth.from(anchor)
            allMeals.filter { YearMonth.from(it.date) == ym }
        }
    }
}

private fun buildCaloriesChartData(
    allMeals: List<com.leosoft.longevity.data.local.entity.MealEntryEntity>,
    foodsById: Map<Long, FoodEntity>,
    range: NutritionChartRange
): List<NutritionTrendPoint> = buildNutritionPeriodData(range) { day ->
    allMeals.filter { it.date == day }.sumOf { meal ->
        val kcal = foodsById[meal.foodId]?.kcalPer100g ?: 0
        ((kcal * meal.grams) / 100f).toInt()
    }
}

private fun buildWaterChartData(
    allLogs: List<com.leosoft.longevity.data.local.entity.WaterLogEntity>,
    range: NutritionChartRange
): List<NutritionTrendPoint> = buildNutritionPeriodData(range) { day ->
    allLogs.filter { it.date == day }.sumOf { it.amountMl }
}

private fun buildSupplementChartData(
    allLogs: List<com.leosoft.longevity.data.local.entity.SupplementLogEntity>,
    range: NutritionChartRange
): List<NutritionTrendPoint> = buildNutritionPeriodData(range) { day ->
    allLogs.count { it.date == day && it.taken }
}

private fun buildNutritionPeriodData(
    range: NutritionChartRange,
    dayValue: (LocalDate) -> Int
): List<NutritionTrendPoint> {
    return when (range) {
        NutritionChartRange.DAILY -> {
            val end = LocalDate.now()
            val start = end.minusDays(6)
            generateSequence(start) { d -> if (d < end) d.plusDays(1) else null }
                .take(7)
                .map { NutritionTrendPoint(it.dayOfMonth.toString(), dayValue(it)) }
                .toList()
        }
        NutritionChartRange.WEEKLY -> {
            val today = LocalDate.now()
            (5 downTo 0).map { w ->
                val anchor = today.minusWeeks(w.toLong())
                val start = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
                val total = (0..6).sumOf { dayValue(start.plusDays(it.toLong())) }
                NutritionTrendPoint("W${start.dayOfMonth}", total)
            }
        }
        NutritionChartRange.MONTHLY -> {
            val cur = YearMonth.now()
            (5 downTo 0).map { m ->
                val ym = cur.minusMonths(m.toLong())
                val total = (1..ym.lengthOfMonth()).sumOf { dayValue(ym.atDay(it)) }
                NutritionTrendPoint(ym.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())), total)
            }
        }
    }
}

@Composable
private fun EmptyDateRecordCard(message: String) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Text(message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NutritionDatePickerCard(
    selectedDate: java.time.LocalDate,
    selectedDateText: String,
    onDateSelected: (java.time.LocalDate) -> Unit
) {
    val context = LocalContext.current
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    onDateSelected(java.time.LocalDate.of(year, month + 1, dayOfMonth))
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }) {
            Text(stringResource(R.string.nutrition_selected_date, selectedDateText))
        }
    }
}

@Composable
private fun AnimatedProgressBar(
    target: Float,
    modifier: Modifier = Modifier,
    color: Color = TrendBarColor,
    trackColor: Color = Color(0xFFEDE7F6)
) {
    val animated by animateFloatAsState(targetValue = target.coerceIn(0f, 1f), animationSpec = tween(700), label = "animated-progress")
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier,
        color = color,
        trackColor = trackColor
    )
}

@Composable
fun PlaceholderTab(text: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAF9)).padding(20.dp), verticalArrangement = Arrangement.Center) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Text(text = text, modifier = Modifier.padding(16.dp))
        }
    }
}
