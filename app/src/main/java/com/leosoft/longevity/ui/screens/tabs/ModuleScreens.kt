package com.leosoft.longevity.ui.screens.tabs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.leosoft.longevity.R
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealType
import com.leosoft.longevity.data.local.entity.WorkoutType
import com.leosoft.longevity.domain.usecase.CalculateMacroTotalsUseCase
import com.leosoft.longevity.reminders.ReminderAlarmScheduler
import java.time.format.DateTimeFormatter
import com.leosoft.longevity.ui.components.MiniProgressCard
import com.leosoft.longevity.ui.components.ScoreBar
import com.leosoft.longevity.ui.main.GoalPlanItem
import com.leosoft.longevity.ui.main.MainViewModel
import kotlinx.coroutines.launch

private enum class QuickAddType { FOOD, WATER, SUPPLEMENT, SLEEP, ACTIVITY }

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
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { content(it) }
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

@Composable fun AktiviteModule() = ModuleTabLayout(listOf("Adım", "Egzersiz Ekle", "Geçmiş", "Hedefler")) { PlaceholderTab("Aktivite") }
@Composable fun YasamModule() = ModuleTabLayout(listOf("Uyku", "Rutinler", "Alışkanlıklar", "Hatırlatmalar")) { PlaceholderTab("Yaşam") }
@Composable fun AnalizModule() = ModuleTabLayout(listOf("Skor", "BioAge", "Rapor", "Trendler")) { PlaceholderTab("Analiz") }

@Composable
fun GunumOzetScreen(viewModel: MainViewModel) {
    val data by viewModel.dashboard.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniProgressCard(stringResource(R.string.card_steps), "${data?.steps ?: 0}", ((data?.steps ?: 0) / 10000f), Modifier.weight(1f))
                MiniProgressCard(stringResource(R.string.card_water), "${data?.waterMl ?: 0} ml", ((data?.waterMl ?: 0) / 2000f), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniProgressCard(stringResource(R.string.card_macro), "P ${data?.macroTotals?.protein?.toInt() ?: 0}g", ((data?.macroTotals?.protein ?: 0f) / 120f), Modifier.weight(1f))
                MiniProgressCard(stringResource(R.string.card_sleep), "${data?.sleepMinutes ?: 0} dk", ((data?.sleepMinutes ?: 0) / 480f), Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.today_longevity_score), style = MaterialTheme.typography.titleMedium)
                    Text("${data?.score?.totalScore ?: 0f}/100", style = MaterialTheme.typography.headlineMedium)
                    ScoreBar(stringResource(R.string.score_nutrition), data?.score?.nutritionScore ?: 0f)
                    ScoreBar(stringResource(R.string.score_water), data?.score?.hydrationScore ?: 0f)
                    ScoreBar(stringResource(R.string.score_activity), data?.score?.activityScore ?: 0f)
                    ScoreBar(stringResource(R.string.score_sleep), data?.score?.sleepScore ?: 0f)
                }
            }
        }
    }
}

@Composable
private fun GunumBenScreen(viewModel: MainViewModel, onGoalsCreated: () -> Unit) {
    val profile by viewModel.profilePreferences.collectAsState()
    var ageText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    val genderKeys = listOf("male", "female", "unspecified")
    var genderIndex by remember { mutableStateOf(2) }
    var initializedFromProfile by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(profile, initializedFromProfile) {
        if (!initializedFromProfile) {
            ageText = profile.age.takeIf { it > 0 }?.toString().orEmpty()
            heightText = profile.heightCm.takeIf { it > 0 }?.toString().orEmpty()
            weightText = profile.weightKg.takeIf { it > 0f }?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }.orEmpty()
            genderIndex = genderKeys.indexOf(profile.gender).takeIf { it >= 0 } ?: 2
            initializedFromProfile = true
        }
    }

    val age = ageText.toIntOrNull()
    val height = heightText.toIntOrNull()
    val weight = weightText.toFloatOrNull()
    val canCalculate = age != null && height != null && weight != null && age > 0 && height > 0 && weight > 0
    val selectedGender = genderKeys[genderIndex]
    val targets = if (canCalculate) viewModel.buildPersonalizedTargets(age!!, height!!, weight!!, selectedGender) else null

    LaunchedEffect(age, height, weight, selectedGender, initializedFromProfile) {
        if (!initializedFromProfile) return@LaunchedEffect
        if (age != null && height != null && weight != null && age > 0 && height > 0 && weight > 0f) {
            viewModel.saveProfile(age, height, weight, selectedGender)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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

        targets?.let { t ->
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.me_targets_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.me_target_activity, t.steps))
                        Text(stringResource(R.string.me_target_sleep, formatSleepDurationLabel(t.sleepMinutes)))
                        Text(stringResource(R.string.me_target_water, t.waterMl))
                        Text(stringResource(R.string.me_target_macros, t.proteinGrams.toInt(), t.carbsGrams.toInt(), t.fatGrams.toInt(), t.fiberGrams.toInt()))
                        Text(stringResource(R.string.me_target_micros, t.ironMg, t.magnesiumMg, t.potassiumMg, t.vitaminDIu, t.omega3Mg))
                    }
                }
            }
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canCalculate,
                onClick = {
                    if (canCalculate) {
                        viewModel.createPersonalizedGoals(age!!, height!!, weight!!, selectedGender) { success ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.goals_created_message), Toast.LENGTH_SHORT).show()
                                onGoalsCreated()
                            } else {
                                Toast.makeText(context, context.getString(R.string.goals_create_error_message), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.me_create_goals))
            }
        }
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { if (goal.target == 0) 0f else (progress.current / goal.target.toFloat()).coerceIn(0f, 1f) },
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
                        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(stringResource(R.string.bed_time)) })
                        OutlinedTextField(value = secondaryText, onValueChange = { secondaryText = it }, label = { Text(stringResource(R.string.wake_time)) })
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
                    QuickAddType.SLEEP -> viewModel.addSleepLog(amountText, secondaryText)
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
    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val mealsSorted = remember(meals) { meals.sortedByDescending { it.time } }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }

    var mealToEdit by remember { mutableStateOf<com.leosoft.longevity.data.local.entity.MealEntryEntity?>(null) }
    var mealToDelete by remember { mutableStateOf<com.leosoft.longevity.data.local.entity.MealEntryEntity?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
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
    val meals by viewModel.mealEntries.collectAsState()
    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val totals = remember(meals, foods) { CalculateMacroTotalsUseCase().invoke(meals, foods.associateBy { it.id }) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.tab_macros), style = MaterialTheme.typography.titleMedium)
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
    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val total = meals.fold(NutrientTotals()) { acc, meal ->
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.tab_micros), style = MaterialTheme.typography.titleMedium)
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
    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
    val supplements by viewModel.supplements.collectAsState()
    val supplementsById = remember(supplements) { supplements.associateBy { it.id } }
    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
fun PlaceholderTab(text: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAF9)).padding(20.dp), verticalArrangement = Arrangement.Center) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Text(text = text, modifier = Modifier.padding(16.dp))
        }
    }
}
