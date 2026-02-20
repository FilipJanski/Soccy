package com.example.threedscanner.sensor

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Shared state for the camera pose to synchronize the Main AR View
 * with the Picture-in-Picture Preview.
 */
class CameraPoseState {
    private val lock = ReentrantLock()
    
    // 4x4 Matrices
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    
    // Flag to indicate if we have valid data
    var hasData = false
        private set

    fun update(view: FloatArray, proj: FloatArray) {
        lock.withLock {
            System.arraycopy(view, 0, viewMatrix, 0, 16)
            System.arraycopy(proj, 0, projectionMatrix, 0, 16)
            hasData = true
        }
    }

    fun getView(dest: FloatArray) {
        lock.withLock {
             System.arraycopy(viewMatrix, 0, dest, 0, 16)
        }
    }
    
    fun getProjection(dest: FloatArray) {
        lock.withLock {
            System.arraycopy(projectionMatrix, 0, dest, 0, 16)
        }
    }
}
