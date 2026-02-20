package com.leosoft.longevity.data.local.entity

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

enum class WorkoutType { ELLIPTICAL, PILATES, WALKING, RUNNING, STRENGTH, YOGA, OTHER }

enum class RecordSource { LOCAL, HEALTH_CONNECT }

enum class SyncState { NONE, PENDING_UPLOAD, SYNCED, CONFLICT, FAILED }

enum class ConflictResolution { LAST_WRITE_WINS, LOCAL_PRIORITY, HEALTH_CONNECT_PRIORITY }
