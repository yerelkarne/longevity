package com.leosoft.longevity.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leosoft.longevity.R
import com.leosoft.longevity.data.local.entity.MealType
import com.leosoft.longevity.data.local.entity.WorkoutType
import com.leosoft.longevity.ui.components.MiniProgressCard
import com.leosoft.longevity.ui.components.ScoreBar
import com.leosoft.longevity.ui.main.MainViewModel
import kotlinx.coroutines.launch

private enum class QuickAddType(val key: String) {
    FOOD("food"), WATER("water"), SUPPLEMENT("supplement"), SLEEP("sleep"), ACTIVITY("activity"), TASK("task"), REMINDER("reminder"), GOAL("goal")
}

@Composable
fun ModuleTabLayout(tabs: List<String>, content: @Composable (Int) -> Unit) {
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
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
    val tabs = listOf(stringResource(R.string.tab_summary), stringResource(R.string.tab_tasks), stringResource(R.string.tab_reminders), stringResource(R.string.tab_score))
    ModuleTabLayout(tabs) { page ->
        when (page) {
            0 -> GunumOzetScreen(viewModel)
            else -> PlaceholderTab(stringResource(R.string.placeholder_coming_soon, page))
        }
    }
}

@Composable
fun BeslenmeModule(viewModel: MainViewModel) {
    val tabs = listOf(stringResource(R.string.tab_log), stringResource(R.string.tab_macros), stringResource(R.string.tab_micros), stringResource(R.string.tab_water), stringResource(R.string.tab_supplements))
    ModuleTabLayout(tabs) { page -> if (page == 0) BeslenmeKayitScreen(viewModel) else PlaceholderTab(stringResource(R.string.placeholder_ready_template, page)) }
}

@Composable fun AktiviteModule() = ModuleTabLayout(listOf("Adım", "Egzersiz Ekle", "Geçmiş", "Hedefler")) { PlaceholderTab("Aktivite") }
@Composable fun YasamModule() = ModuleTabLayout(listOf("Uyku", "Rutinler", "Alışkanlıklar", "Hatırlatmalar")) { PlaceholderTab("Yaşam") }
@Composable fun AnalizModule() = ModuleTabLayout(listOf("Skor", "BioAge", "Rapor", "Trendler")) { PlaceholderTab("Analiz") }

@Composable
fun GunumOzetScreen(viewModel: MainViewModel) {
    val data = viewModel.dashboard.value
    var openQuickAdd by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.remaining_tasks))
                        data?.pendingTasks?.forEach { Text("• $it") }
                    }
                }
            }
        }

        FloatingActionButton(onClick = { openQuickAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Text(stringResource(R.string.add_record_cta))
        }
    }

    if (openQuickAdd) {
        QuickAddDialog(viewModel = viewModel, onDismiss = { openQuickAdd = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val foods = viewModel.foods.value
    val supplements = viewModel.supplements.value
    var type by remember { mutableStateOf(QuickAddType.FOOD) }
    var expanded by remember { mutableStateOf(false) }
    var selectedFoodId by remember { mutableLongStateOf(foods.firstOrNull()?.id ?: 0L) }
    var selectedSupplementId by remember { mutableLongStateOf(supplements.firstOrNull()?.id ?: 0L) }
    var amountText by remember { mutableStateOf("") }
    var secondaryText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var selectedWorkoutType by remember { mutableStateOf(WorkoutType.WALKING) }
    var selectedGoalType by remember { mutableStateOf("water") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_record_cta)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = stringResource(typeLabel(type)),
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
                    QuickAddType.FOOD -> {
                        ExposedDropdownSimple(
                            label = stringResource(R.string.food_name),
                            options = foods.map { it.name },
                            selected = foods.indexOfFirst { it.id == selectedFoodId }.coerceAtLeast(0),
                            onSelect = { idx -> selectedFoodId = foods[idx].id }
                        )
                        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(stringResource(R.string.grams)) })
                    }
                    QuickAddType.WATER -> OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(stringResource(R.string.water_ml_input)) })
                    QuickAddType.SUPPLEMENT -> {
                        ExposedDropdownSimple(
                            label = stringResource(R.string.supplement_name),
                            options = supplements.map { it.name },
                            selected = supplements.indexOfFirst { it.id == selectedSupplementId }.coerceAtLeast(0),
                            onSelect = { idx -> selectedSupplementId = supplements[idx].id }
                        )
                    }
                    QuickAddType.SLEEP -> {
                        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(stringResource(R.string.bed_time)) })
                        OutlinedTextField(value = secondaryText, onValueChange = { secondaryText = it }, label = { Text(stringResource(R.string.wake_time)) })
                    }
                    QuickAddType.ACTIVITY -> {
                        ExposedDropdownSimple(
                            label = stringResource(R.string.activity_type),
                            options = WorkoutType.entries.map { it.name },
                            selected = WorkoutType.entries.indexOf(selectedWorkoutType),
                            onSelect = { idx -> selectedWorkoutType = WorkoutType.entries[idx] }
                        )
                        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(stringResource(R.string.duration_min)) })
                        OutlinedTextField(value = secondaryText, onValueChange = { secondaryText = it }, label = { Text(stringResource(R.string.intensity_1_3)) })
                    }
                    QuickAddType.TASK -> {
                        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(stringResource(R.string.task_title)) })
                        OutlinedTextField(value = secondaryText, onValueChange = { secondaryText = it }, label = { Text(stringResource(R.string.optional_target)) })
                    }
                    QuickAddType.REMINDER -> {
                        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(stringResource(R.string.reminder_type)) })
                        OutlinedTextField(value = secondaryText, onValueChange = { secondaryText = it }, label = { Text(stringResource(R.string.reminder_time)) })
                    }
                    QuickAddType.GOAL -> {
                        ExposedDropdownSimple(
                            label = stringResource(R.string.goal_type),
                            options = listOf("water", "steps", "protein", "sleep", "supplements"),
                            selected = listOf("water", "steps", "protein", "sleep", "supplements").indexOf(selectedGoalType),
                            onSelect = { idx -> selectedGoalType = listOf("water", "steps", "protein", "sleep", "supplements")[idx] }
                        )
                        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(stringResource(R.string.target_value)) })
                    }
                }

                OutlinedTextField(value = notesText, onValueChange = { notesText = it }, label = { Text(stringResource(R.string.notes_optional)) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (type) {
                    QuickAddType.FOOD -> viewModel.addMeal(selectedFoodId, amountText.toIntOrNull() ?: 0, MealType.SNACK)
                    QuickAddType.WATER -> viewModel.addWater(amountText.toIntOrNull() ?: 0)
                    QuickAddType.SUPPLEMENT -> viewModel.addSupplementLog(selectedSupplementId)
                    QuickAddType.SLEEP -> viewModel.addSleepLog(amountText, secondaryText)
                    QuickAddType.ACTIVITY -> viewModel.addWorkout(selectedWorkoutType, amountText.toIntOrNull() ?: 0, secondaryText.toIntOrNull() ?: 1, notesText)
                    QuickAddType.TASK -> viewModel.addTask(amountText, secondaryText.ifBlank { null })
                    QuickAddType.REMINDER -> viewModel.addReminder(amountText, secondaryText)
                    QuickAddType.GOAL -> viewModel.updateGoal(selectedGoalType, amountText.toIntOrNull() ?: 0)
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
    QuickAddType.TASK -> R.string.add_type_task
    QuickAddType.REMINDER -> R.string.add_type_reminder
    QuickAddType.GOAL -> R.string.add_type_goal
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeslenmeKayitScreen(viewModel: MainViewModel) {
    val foods = viewModel.foods.value
    val meals = viewModel.mealEntries.value
    var openDialog by remember { mutableStateOf(false) }
    var selectedFoodId by remember { mutableLongStateOf(foods.firstOrNull()?.id ?: 0L) }
    var grams by remember { mutableIntStateOf(100) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.daily_total))
                    Text(stringResource(R.string.total_grams, meals.sumOf { it.grams }))
                    Button(onClick = { openDialog = true }, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.add_food)) }
                }
            }
        }
        items(meals) { entry ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp)) {
                    Text("${entry.mealType} - ${entry.grams} g")
                    Text(stringResource(R.string.time_label, entry.time.toLocalTime().toString()), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { openDialog = false },
            title = { Text(stringResource(R.string.add_food_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownSimple(
                        label = stringResource(R.string.food_name),
                        options = foods.map { it.name },
                        selected = foods.indexOfFirst { it.id == selectedFoodId }.coerceAtLeast(0),
                        onSelect = { selectedFoodId = foods[it].id }
                    )
                    OutlinedTextField(value = grams.toString(), onValueChange = { grams = it.toIntOrNull() ?: grams }, label = { Text(stringResource(R.string.grams)) })
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.addMeal(selectedFoodId, grams, MealType.SNACK); openDialog = false }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { openDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
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
