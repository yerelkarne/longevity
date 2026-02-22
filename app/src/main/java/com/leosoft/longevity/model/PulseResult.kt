package com.leosoft.longevity.model

data class PulseResult(
    val bpm: Int,
    val quality: Int,
    val qualityLabel: String,
    val status: PulseStatus,
    val confidenceRange: IntRange,
    val debug: String = ""
)

enum class PulseStatus {
    SUCCESS,
    LOW_QUALITY,
    INSUFFICIENT_DATA,
    UNSTABLE,
    NO_FINGER,
    HOLD_STILL,
    SATURATED,
    LOW_FPS
}
