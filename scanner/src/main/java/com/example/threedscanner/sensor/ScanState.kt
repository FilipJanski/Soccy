package com.example.threedscanner.sensor

enum class ScanStatus {
    IDLE,
    SCANNING,
    PROCESSING,
    VIEWING_RESULT
}

data class ScanUiState(
    val status: ScanStatus = ScanStatus.IDLE,
    val pointCount: Int = 0,
    val message: String? = null,
    val boundingBoxVisible: Boolean = true,
    val scanDistance: Float = 0f,
    val keyframeCount: Int = 0,
    val progress: Float = 0f,
    val isAutofocusEnabled: Boolean = true
)
