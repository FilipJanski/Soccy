package com.example.threedscanner.sensor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScanViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun startScan() {
        _uiState.update { it.copy(status = ScanStatus.SCANNING, message = "Scanning started...") }
    }

    fun stopScan() {
        _uiState.update { it.copy(status = ScanStatus.PROCESSING, message = "Processing data...") }
        // processing is driven by callbacks now
    }

    fun processingProgress(p: Float) {
        _uiState.update { it.copy(status = ScanStatus.PROCESSING, progress = p, message = "Merging data: ${(p * 100).toInt()}%") }
    }
    
    fun finishProcessing(finalCount: Int) {
        _uiState.update { it.copy(status = ScanStatus.VIEWING_RESULT, message = "Scan complete! $finalCount points.", pointCount = finalCount, progress = 1.0f) }
    }

    fun reset() {
        _uiState.update { ScanUiState(status = ScanStatus.IDLE) }
    }

    fun updatePointCount(count: Int) {
        _uiState.update { it.copy(pointCount = count) }
    }

    fun updateScanDistance(dist: Float) {
        _uiState.update { it.copy(scanDistance = dist) }
    }
    
    fun updateKeyframeCount(count: Int) {
        _uiState.update { it.copy(keyframeCount = count) }
    }

    fun toggleAutofocus() {
        _uiState.update { it.copy(isAutofocusEnabled = !it.isAutofocusEnabled) }
    }
}
