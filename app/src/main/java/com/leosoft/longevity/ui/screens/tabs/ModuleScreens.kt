package com.leosoft.longevity.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leosoft.longevity.R
import com.leosoft.longevity.data.local.entity.FoodEntity
import com.leosoft.longevity.data.local.entity.MealType
import com.leosoft.longevity.data.local.entity.WorkoutType
import com.leosoft.longevity.domain.usecase.CalculateMacroTotalsUseCase
import com.leosoft.longevity.ui.components.MiniProgressCard
import com.leosoft.longevity.ui.components.ScoreBar
import com.leosoft.longevity.ui.main.MainViewModel
import kotlinx.coroutines.launch

private enum class QuickAddType { FOOD, WATER, SUPPLEMENT, SLEEP, ACTIVITY, TASK, REMINDER, GOAL }

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
    ModuleTabLayout(tabs) { page ->
        when (page) {
            0 -> BeslenmeKayitScreen(viewModel)
            1 -> BeslenmeMakrolarScreen(viewModel)
            2 -> BeslenmeMikrolarScreen(viewModel)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
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
                        ExposedDropdownSimple(
                            label = stringResource(R.string.activity_type),
                            options = WorkoutType.entries.map { stringResource(workoutTypeLabel(it)) },
                            selected = WorkoutType.entries.indexOf(selectedWorkoutType),
                            onSelect = { idx -> selectedWorkoutType = WorkoutType.entries[idx] }
                        )
                        OutlinedTextField(value = customActivityName, onValueChange = { customActivityName = it }, label = { Text(stringResource(R.string.custom_activity_name_optional)) })
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
                        val goalOptions = listOf(
                            "water" to stringResource(R.string.goal_type_water),
                            "steps" to stringResource(R.string.goal_type_steps),
                            "protein" to stringResource(R.string.goal_type_protein),
                            "sleep" to stringResource(R.string.goal_type_sleep),
                            "supplements" to stringResource(R.string.goal_type_supplements)
                        )
                        ExposedDropdownSimple(
                            label = stringResource(R.string.goal_type),
                            options = goalOptions.map { it.second },
                            selected = goalOptions.indexOfFirst { it.first == selectedGoalType }.coerceAtLeast(0),
                            onSelect = { idx -> selectedGoalType = goalOptions[idx].first }
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
                    null -> Unit
                    QuickAddType.FOOD -> viewModel.addMealWithOptionalCustomFood(selectedFoodId, customFoodName, amountText.toIntOrNull() ?: 0, MealType.SNACK)
                    QuickAddType.WATER -> viewModel.addWater(amountText.toIntOrNull() ?: 0)
                    QuickAddType.SUPPLEMENT -> viewModel.addSupplementLog(selectedSupplementId)
                    QuickAddType.SLEEP -> viewModel.addSleepLog(amountText, secondaryText)
                    QuickAddType.ACTIVITY -> {
                        val resolvedType = if (customActivityName.isNotBlank()) WorkoutType.OTHER else selectedWorkoutType
                        val mergedNotes = listOf(customActivityName.takeIf { it.isNotBlank() }, notesText.takeIf { it.isNotBlank() }).joinToString(" | ")
                        viewModel.addWorkout(resolvedType, amountText.toIntOrNull() ?: 0, secondaryText.toIntOrNull() ?: 1, mergedNotes)
                    }
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
    val foods by viewModel.foods.collectAsState()
    val foodsById = remember(foods) { foods.associateBy { it.id } }
    val meals by viewModel.mealEntries.collectAsState()
    var mealToEdit by remember { mutableStateOf<com.leosoft.longevity.data.local.entity.MealEntryEntity?>(null) }
    var mealToDelete by remember { mutableStateOf<com.leosoft.longevity.data.local.entity.MealEntryEntity?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.daily_total))
                    Text(stringResource(R.string.total_grams, meals.sumOf { it.grams }))
                }
            }
        }
        items(meals, key = { it.id }) { entry ->
            val food = foodsById[entry.foodId]
            val n = food?.let { nutrientByGrams(it, entry.grams) }
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().clickable { mealToEdit = entry }
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${food?.name ?: stringResource(R.string.unknown_food)} • ${entry.grams} g", style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.time_label, entry.time.toLocalTime().toString()), style = MaterialTheme.typography.bodySmall)
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
                    viewModel.deleteMealEntry(entry.id)
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
    val totals = remember(meals, foods) { CalculateMacroTotalsUseCase().invoke(meals, foods.associateBy { it.id }) }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.tab_macros), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.macros_line, totals.protein, totals.carbs, totals.fat, totals.fiber))
        }
    }
}

@Composable
fun BeslenmeMikrolarScreen(viewModel: MainViewModel) {
    val foods by viewModel.foods.collectAsState()
    val foodsById = remember(foods) { foods.associateBy { it.id } }
    val meals by viewModel.mealEntries.collectAsState()
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

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.tab_micros), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.micros_line, total.iron, total.magnesium, total.potassium, total.vitaminD, total.omega3))
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
