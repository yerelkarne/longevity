package com.leosoft.longevity.data.repository

import com.leosoft.longevity.data.local.entity.FoodEntity

internal object FoodDataset {
    val defaults: List<FoodEntity> = listOf(
        FoodEntity(name = "Yumurta", kcalPer100g = 155, protein = 13f, carbs = 1.1f, fat = 11f, fiber = 0f, vitaminDUi = 82f),
        FoodEntity(name = "Somon", kcalPer100g = 208, protein = 20f, carbs = 0f, fat = 13f, fiber = 0f, omega3Mg = 2000f, vitaminDUi = 526f),
        FoodEntity(name = "Tavuk Göğüs", kcalPer100g = 165, protein = 31f, carbs = 0f, fat = 3.6f, fiber = 0f, potassiumMg = 256f),
        FoodEntity(name = "Hindi Göğüs", kcalPer100g = 135, protein = 29f, carbs = 0f, fat = 1.5f, fiber = 0f, potassiumMg = 239f),
        FoodEntity(name = "Ton Balığı", kcalPer100g = 132, protein = 29f, carbs = 0f, fat = 1f, fiber = 0f, omega3Mg = 300f, vitaminDUi = 154f),
        FoodEntity(name = "Yoğurt", kcalPer100g = 61, protein = 3.5f, carbs = 4.7f, fat = 3.3f, fiber = 0f, potassiumMg = 141f),
        FoodEntity(name = "Yulaf", kcalPer100g = 389, protein = 16.9f, carbs = 66.3f, fat = 6.9f, fiber = 10.6f, magnesiumMg = 177f, ironMg = 4.7f, potassiumMg = 429f),
        FoodEntity(name = "Kinoa", kcalPer100g = 368, protein = 14.1f, carbs = 64.2f, fat = 6.1f, fiber = 7f, magnesiumMg = 197f, ironMg = 4.6f, potassiumMg = 563f),
        FoodEntity(name = "Esmer Pirinç", kcalPer100g = 370, protein = 7.9f, carbs = 77.2f, fat = 2.9f, fiber = 3.5f, magnesiumMg = 143f, potassiumMg = 223f),
        FoodEntity(name = "Tatlı Patates", kcalPer100g = 86, protein = 1.6f, carbs = 20.1f, fat = 0.1f, fiber = 3f, potassiumMg = 337f),
        FoodEntity(name = "Avokado", kcalPer100g = 160, protein = 2f, carbs = 8.5f, fat = 14.7f, fiber = 6.7f, potassiumMg = 485f, magnesiumMg = 29f),
        FoodEntity(name = "Badem", kcalPer100g = 579, protein = 21.2f, carbs = 21.6f, fat = 49.9f, fiber = 12.5f, magnesiumMg = 270f, ironMg = 3.7f, potassiumMg = 733f),
        FoodEntity(name = "Ispanak", kcalPer100g = 23, protein = 2.9f, carbs = 3.6f, fat = 0.4f, fiber = 2.2f, ironMg = 2.7f, magnesiumMg = 79f, potassiumMg = 558f),
        FoodEntity(name = "Brokoli", kcalPer100g = 34, protein = 2.8f, carbs = 6.6f, fat = 0.4f, fiber = 2.6f, ironMg = 0.7f, potassiumMg = 316f),
        FoodEntity(name = "Mercimek", kcalPer100g = 353, protein = 25.8f, carbs = 60.1f, fat = 1.1f, fiber = 10.7f, ironMg = 6.5f, magnesiumMg = 122f, potassiumMg = 955f),
        FoodEntity(name = "Nohut", kcalPer100g = 364, protein = 19.3f, carbs = 60.7f, fat = 6f, fiber = 17.4f, ironMg = 6.2f, magnesiumMg = 79f, potassiumMg = 875f),
        FoodEntity(name = "Zeytin", kcalPer100g = 115, protein = 0.8f, carbs = 6.3f, fat = 10.7f, fiber = 3.2f, ironMg = 3.3f, potassiumMg = 42f)
    )
}
