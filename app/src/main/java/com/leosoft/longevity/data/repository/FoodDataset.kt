package com.leosoft.longevity.data.repository

import com.leosoft.longevity.data.local.entity.FoodEntity
import java.util.Locale

internal data class FoodSeed(
    val key: String,
    val names: Map<String, String>,
    val kcalPer100g: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val ironMg: Float = 0f,
    val magnesiumMg: Float = 0f,
    val potassiumMg: Float = 0f,
    val vitaminDUi: Float = 0f,
    val omega3Mg: Float = 0f
) {
    fun localizedName(language: String): String = names[language] ?: names["en"] ?: key

    fun toFoodEntity(language: String): FoodEntity = FoodEntity(
        name = localizedName(language),
        kcalPer100g = kcalPer100g,
        protein = protein,
        carbs = carbs,
        fat = fat,
        fiber = fiber,
        ironMg = ironMg,
        magnesiumMg = magnesiumMg,
        potassiumMg = potassiumMg,
        vitaminDUi = vitaminDUi,
        omega3Mg = omega3Mg
    )
}

internal object FoodDataset {
    val defaults: List<FoodSeed> = listOf(
        FoodSeed(key = "egg", names = mapOf("tr" to "Yumurta", "en" to "Egg", "it" to "Uovo", "fr" to "Œuf", "de" to "Ei", "es" to "Huevo"), kcalPer100g = 155, protein = 13f, carbs = 1.1f, fat = 11f, fiber = 0f, vitaminDUi = 82f),
        FoodSeed(key = "salmon", names = mapOf("tr" to "Somon", "en" to "Salmon", "it" to "Salmone", "fr" to "Saumon", "de" to "Lachs", "es" to "Salmón"), kcalPer100g = 208, protein = 20f, carbs = 0f, fat = 13f, fiber = 0f, omega3Mg = 2000f, vitaminDUi = 526f),
        FoodSeed(key = "chicken_breast", names = mapOf("tr" to "Tavuk Göğüs", "en" to "Chicken Breast", "it" to "Petto di pollo", "fr" to "Blanc de poulet", "de" to "Hähnchenbrust", "es" to "Pechuga de pollo"), kcalPer100g = 165, protein = 31f, carbs = 0f, fat = 3.6f, fiber = 0f, potassiumMg = 256f),
        FoodSeed(key = "turkey_breast", names = mapOf("tr" to "Hindi Göğüs", "en" to "Turkey Breast", "it" to "Petto di tacchino", "fr" to "Blanc de dinde", "de" to "Putenbrust", "es" to "Pechuga de pavo"), kcalPer100g = 135, protein = 29f, carbs = 0f, fat = 1.5f, fiber = 0f, potassiumMg = 239f),
        FoodSeed(key = "tuna", names = mapOf("tr" to "Ton Balığı", "en" to "Tuna", "it" to "Tonno", "fr" to "Thon", "de" to "Thunfisch", "es" to "Atún"), kcalPer100g = 132, protein = 29f, carbs = 0f, fat = 1f, fiber = 0f, omega3Mg = 300f, vitaminDUi = 154f),
        FoodSeed(key = "yogurt", names = mapOf("tr" to "Yoğurt", "en" to "Yogurt", "it" to "Yogurt", "fr" to "Yaourt", "de" to "Joghurt", "es" to "Yogur"), kcalPer100g = 61, protein = 3.5f, carbs = 4.7f, fat = 3.3f, fiber = 0f, potassiumMg = 141f),
        FoodSeed(key = "oats", names = mapOf("tr" to "Yulaf", "en" to "Oats", "it" to "Avena", "fr" to "Avoine", "de" to "Haferflocken", "es" to "Avena"), kcalPer100g = 389, protein = 16.9f, carbs = 66.3f, fat = 6.9f, fiber = 10.6f, magnesiumMg = 177f, ironMg = 4.7f, potassiumMg = 429f),
        FoodSeed(key = "quinoa", names = mapOf("tr" to "Kinoa", "en" to "Quinoa", "it" to "Quinoa", "fr" to "Quinoa", "de" to "Quinoa", "es" to "Quinoa"), kcalPer100g = 368, protein = 14.1f, carbs = 64.2f, fat = 6.1f, fiber = 7f, magnesiumMg = 197f, ironMg = 4.6f, potassiumMg = 563f),
        FoodSeed(key = "brown_rice", names = mapOf("tr" to "Esmer Pirinç", "en" to "Brown Rice", "it" to "Riso integrale", "fr" to "Riz complet", "de" to "Vollkornreis", "es" to "Arroz integral"), kcalPer100g = 370, protein = 7.9f, carbs = 77.2f, fat = 2.9f, fiber = 3.5f, magnesiumMg = 143f, potassiumMg = 223f),
        FoodSeed(key = "sweet_potato", names = mapOf("tr" to "Tatlı Patates", "en" to "Sweet Potato", "it" to "Patata dolce", "fr" to "Patate douce", "de" to "Süßkartoffel", "es" to "Batata"), kcalPer100g = 86, protein = 1.6f, carbs = 20.1f, fat = 0.1f, fiber = 3f, potassiumMg = 337f),
        FoodSeed(key = "avocado", names = mapOf("tr" to "Avokado", "en" to "Avocado", "it" to "Avocado", "fr" to "Avocat", "de" to "Avocado", "es" to "Aguacate"), kcalPer100g = 160, protein = 2f, carbs = 8.5f, fat = 14.7f, fiber = 6.7f, potassiumMg = 485f, magnesiumMg = 29f),
        FoodSeed(key = "almond", names = mapOf("tr" to "Badem", "en" to "Almond", "it" to "Mandorla", "fr" to "Amande", "de" to "Mandel", "es" to "Almendra"), kcalPer100g = 579, protein = 21.2f, carbs = 21.6f, fat = 49.9f, fiber = 12.5f, magnesiumMg = 270f, ironMg = 3.7f, potassiumMg = 733f),
        FoodSeed(key = "spinach", names = mapOf("tr" to "Ispanak", "en" to "Spinach", "it" to "Spinaci", "fr" to "Épinards", "de" to "Spinat", "es" to "Espinaca"), kcalPer100g = 23, protein = 2.9f, carbs = 3.6f, fat = 0.4f, fiber = 2.2f, ironMg = 2.7f, magnesiumMg = 79f, potassiumMg = 558f),
        FoodSeed(key = "broccoli", names = mapOf("tr" to "Brokoli", "en" to "Broccoli", "it" to "Broccoli", "fr" to "Brocoli", "de" to "Brokkoli", "es" to "Brócoli"), kcalPer100g = 34, protein = 2.8f, carbs = 6.6f, fat = 0.4f, fiber = 2.6f, ironMg = 0.7f, potassiumMg = 316f),
        FoodSeed(key = "lentil", names = mapOf("tr" to "Mercimek", "en" to "Lentil", "it" to "Lenticchie", "fr" to "Lentilles", "de" to "Linsen", "es" to "Lenteja"), kcalPer100g = 353, protein = 25.8f, carbs = 60.1f, fat = 1.1f, fiber = 10.7f, ironMg = 6.5f, magnesiumMg = 122f, potassiumMg = 955f),
        FoodSeed(key = "chickpea", names = mapOf("tr" to "Nohut", "en" to "Chickpea", "it" to "Ceci", "fr" to "Pois chiches", "de" to "Kichererbse", "es" to "Garbanzos"), kcalPer100g = 364, protein = 19.3f, carbs = 60.7f, fat = 6f, fiber = 17.4f, ironMg = 6.2f, magnesiumMg = 79f, potassiumMg = 875f),
        FoodSeed(key = "olive", names = mapOf("tr" to "Zeytin", "en" to "Olive", "it" to "Oliva", "fr" to "Olive", "de" to "Olive", "es" to "Aceituna"), kcalPer100g = 115, protein = 0.8f, carbs = 6.3f, fat = 10.7f, fiber = 3.2f, ironMg = 3.3f, potassiumMg = 42f)
    )

    fun currentLanguage(): String = Locale.getDefault().language.lowercase(Locale.ROOT)
}
