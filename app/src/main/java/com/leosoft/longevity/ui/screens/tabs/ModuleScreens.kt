package com.leosoft.longevity.ui.screens.tabs

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.leosoft.longevity.data.local.entity.MealType
import com.leosoft.longevity.ui.components.MiniProgressCard
import com.leosoft.longevity.ui.components.ScoreBar
import com.leosoft.longevity.ui.main.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ModuleTabLayout(tabs: List<String>, content: @Composable (Int) -> Unit) {
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == pagerState.currentPage,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(tab) }
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            content(page)
        }
    }
}

@Composable
fun GunumModule(viewModel: MainViewModel) {
    val tabs = listOf("Özet", "Görevler", "Hatırlatmalar", "Skor")
    ModuleTabLayout(tabs) { page ->
        when (page) {
            0 -> GunumOzetScreen(viewModel)
            else -> PlaceholderTab("$page. sekme yakında")
        }
    }
}

@Composable
fun BeslenmeModule(viewModel: MainViewModel) {
    val tabs = listOf("Kayıt", "Makrolar", "Mikrolar", "Su", "Takviyeler")
    ModuleTabLayout(tabs) { page ->
        when (page) {
            0 -> BeslenmeKayitScreen(viewModel)
            else -> PlaceholderTab("$page. sekme hazır şablon")
        }
    }
}

@Composable fun AktiviteModule() = ModuleTabLayout(listOf("Adım", "Egzersiz Ekle", "Geçmiş", "Hedefler")) { PlaceholderTab("Aktivite") }
@Composable fun YasamModule() = ModuleTabLayout(listOf("Uyku", "Rutinler", "Alışkanlıklar", "Hatırlatmalar")) { PlaceholderTab("Yaşam") }
@Composable fun AnalizModule() = ModuleTabLayout(listOf("Skor", "BioAge", "Rapor", "Trendler")) { PlaceholderTab("Analiz") }

@Composable
fun GunumOzetScreen(viewModel: MainViewModel) {
    val data = viewModel.dashboard.value
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniProgressCard("Adım", "${data?.steps ?: 0}", ((data?.steps ?: 0) / 10000f), Modifier.weight(1f))
                MiniProgressCard("Su", "${data?.waterMl ?: 0} ml", ((data?.waterMl ?: 0) / 2000f), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniProgressCard("Makro", "P ${data?.macroTotals?.protein?.toInt() ?: 0}g", ((data?.macroTotals?.protein ?: 0f) / 120f), Modifier.weight(1f))
                MiniProgressCard("Uyku", "${data?.sleepMinutes ?: 0} dk", ((data?.sleepMinutes ?: 0) / 480f), Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bugünün Longevity Skoru", style = MaterialTheme.typography.titleMedium)
                    Text("${data?.score?.totalScore ?: 0f}/100", style = MaterialTheme.typography.headlineMedium)
                    ScoreBar("Beslenme", data?.score?.nutritionScore ?: 0f)
                    ScoreBar("Su", data?.score?.hydrationScore ?: 0f)
                    ScoreBar("Aktivite", data?.score?.activityScore ?: 0f)
                    ScoreBar("Uyku", data?.score?.sleepScore ?: 0f)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Bugün kalan görevler")
                    data?.pendingTasks?.forEach { Text("• $it") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeslenmeKayitScreen(viewModel: MainViewModel) {
    val foods = viewModel.foods.value
    val meals = viewModel.mealEntries.value
    var openDialog by remember { mutableStateOf(false) }
    var selectedFoodId by remember { mutableLongStateOf(foods.firstOrNull()?.id ?: 0L) }
    var grams by remember { mutableIntStateOf(100) }
    var mealType by remember { mutableStateOf(MealType.BREAKFAST) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Gün toplamı")
                    val total = meals.sumOf { it.grams }
                    Text("Toplam gram: $total")
                    Button(onClick = { openDialog = true }, modifier = Modifier.padding(top = 8.dp)) { Text("+ Gıda ekle") }
                }
            }
        }
        items(meals) { entry ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp)) {
                    Text("${entry.mealType} - ${entry.grams} g")
                    Text("Saat: ${entry.time.toLocalTime()}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { openDialog = false },
            title = { Text("Gıda ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    foods.forEach { food ->
                        FilterChip(
                            selected = selectedFoodId == food.id,
                            onClick = { selectedFoodId = food.id },
                            label = { Text(food.name) }
                        )
                    }
                    OutlinedTextField(value = grams.toString(), onValueChange = { grams = it.toIntOrNull() ?: grams }, label = { Text("Gram") })
                    OutlinedTextField(value = mealType.name, onValueChange = {}, readOnly = true, label = { Text("Öğün") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addMeal(selectedFoodId, grams, mealType)
                    openDialog = false
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { openDialog = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
fun PlaceholderTab(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAF9)).padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Text(text = text, modifier = Modifier.padding(16.dp))
        }
    }
}
