package com.leosoft.longevity.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import com.leosoft.longevity.data.local.entity.WorkoutType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class HealthConnectAdapter(private val context: Context) {
    private val client: HealthConnectClient? by lazy {
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null
    }

    fun isAvailable(): Boolean = sdkStatuses().any { it == HealthConnectClient.SDK_AVAILABLE }

    fun isInstallable(): Boolean = !isAvailable() && sdkStatuses().any { it == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED }

    fun permissionsContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun grantedPermissions(): Set<String> = client?.permissionController?.getGrantedPermissions().orEmpty()

    val allPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    suspend fun insertSteps(start: Instant, end: Instant, count: Long): String? {
        val record = StepsRecord(
            startTime = start,
            startZoneOffset = zoneOffsetAt(start),
            endTime = end,
            endZoneOffset = zoneOffsetAt(end),
            count = count
        )
        return client?.insertRecords(listOf(record))?.recordIdsList?.firstOrNull()
    }

    suspend fun insertHydration(time: Instant, amountMl: Double): String? {
        val record = HydrationRecord(
            startTime = time,
            startZoneOffset = zoneOffsetAt(time),
            endTime = time,
            endZoneOffset = zoneOffsetAt(time),
            volume = Volume.milliliters(amountMl)
        )
        return client?.insertRecords(listOf(record))?.recordIdsList?.firstOrNull()
    }

    suspend fun insertSleep(start: Instant, end: Instant): String? {
        val record = SleepSessionRecord(
            startTime = start,
            startZoneOffset = zoneOffsetAt(start),
            endTime = end,
            endZoneOffset = zoneOffsetAt(end)
        )
        return client?.insertRecords(listOf(record))?.recordIdsList?.firstOrNull()
    }

    suspend fun insertExercise(start: Instant, end: Instant, type: WorkoutType, notes: String): String? {
        val record = ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = zoneOffsetAt(start),
            endTime = end,
            endZoneOffset = zoneOffsetAt(end),
            exerciseType = workoutToHealthType(type),
            notes = notes
        )
        return client?.insertRecords(listOf(record))?.recordIdsList?.firstOrNull()
    }

    suspend fun insertNutrition(time: Instant, protein: Double, carbs: Double, fat: Double, calories: Double): String? {
        val record = NutritionRecord(
            startTime = time,
            startZoneOffset = zoneOffsetAt(time),
            endTime = time,
            endZoneOffset = zoneOffsetAt(time),
            protein = Mass.grams(protein),
            totalCarbohydrate = Mass.grams(carbs),
            totalFat = Mass.grams(fat),
            energy = Energy.calories(calories)
        )
        return client?.insertRecords(listOf(record))?.recordIdsList?.firstOrNull()
    }

    suspend fun readSteps(start: Instant, end: Instant): List<StepsRecord> =
        client?.readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end)))?.records.orEmpty()

    suspend fun readHydration(start: Instant, end: Instant): List<HydrationRecord> =
        client?.readRecords(ReadRecordsRequest(HydrationRecord::class, TimeRangeFilter.between(start, end)))?.records.orEmpty()

    suspend fun readSleep(start: Instant, end: Instant): List<SleepSessionRecord> =
        client?.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(start, end)))?.records.orEmpty()

    suspend fun readExercises(start: Instant, end: Instant): List<ExerciseSessionRecord> =
        client?.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, end)))?.records.orEmpty()

    suspend fun readNutrition(start: Instant, end: Instant): List<NutritionRecord> =
        client?.readRecords(ReadRecordsRequest(NutritionRecord::class, TimeRangeFilter.between(start, end)))?.records.orEmpty()

    fun dayStart(date: LocalDate): Instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
    fun dayEnd(date: LocalDate): Instant = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
    fun instantToLocalDateTime(value: Instant): LocalDateTime = LocalDateTime.ofInstant(value, ZoneId.systemDefault())

    private fun zoneOffsetAt(instant: Instant): ZoneOffset =
        ZoneId.systemDefault().rules.getOffset(instant)

    private fun workoutToHealthType(type: WorkoutType): Int = when (type) {
        WorkoutType.WALKING -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
        WorkoutType.RUNNING -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        WorkoutType.ELLIPTICAL -> ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
        WorkoutType.PILATES -> ExerciseSessionRecord.EXERCISE_TYPE_PILATES
        WorkoutType.STRENGTH -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
        WorkoutType.YOGA -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
        WorkoutType.OTHER -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
    }

    companion object {
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
        private const val PLATFORM_PROVIDER_PACKAGE = "com.android.healthconnect"
    }

    private fun sdkStatuses(): List<Int> {
        return listOf(PROVIDER_PACKAGE, PLATFORM_PROVIDER_PACKAGE)
            .map { pkg -> HealthConnectClient.getSdkStatus(context, pkg) }
            .distinct()
    }
}
