package com.example.threedscanner.ui

import android.Manifest
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.threedscanner.sensor.PreviewRenderer
import com.example.threedscanner.sensor.ScanRenderer
import com.example.threedscanner.sensor.ScanStatus
import com.example.threedscanner.sensor.ScanViewModel
import com.example.threedscanner.sensor.SimpleScanEngine
import com.google.ar.core.Session
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Activity
import android.content.ContextWrapper

@Composable
fun ScanScreen(
    scanTag: String? = null,
    scanViewModel: ScanViewModel = viewModel()
) {
    val uiState by scanViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Engine & Renderers
    val scanEngine = remember { SimpleScanEngine(context) }
    val cameraPoseState = remember { com.example.threedscanner.sensor.CameraPoseState() }
    val scanRenderer = remember { ScanRenderer(context, cameraPoseState) }
    val previewRenderer = remember { PreviewRenderer(scanEngine, cameraPoseState) }
    
    // Connect Engine to VM
    LaunchedEffect(scanEngine) {
        scanEngine.onKeyframeAdded = { count ->
            scanViewModel.updateKeyframeCount(count)
        }
        scanEngine.onDistanceChange = { dist ->
            scanViewModel.updateScanDistance(dist)
        }
        scanEngine.onProcessingProgress = { p ->
            scanViewModel.processingProgress(p)
        }
        scanEngine.onScanResultReady = { count ->
             scanViewModel.finishProcessing(count)
        }
    }
    
    // Session State
    var session by remember { mutableStateOf<Session?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        var s: Session? = null
        try {
            // Check permission again just in case (though MainActivity caches it)
            // In a real app we'd verify ContextCompat.checkSelfPermission...
            
            s = Session(context)
            // Configure
            scanEngine.onResume(s)
            
            // Link to renderer
            s.resume()
            session = s
            
            // Link to renderer AFTER resume to ensure session is ready
            // and multiple resumes don't confuse state if side-effects exist
            scanRenderer.session = s
            scanRenderer.scanEngine = scanEngine
            
        } catch (e: Exception) {
            Log.e("ScanScreen", "Session creation failed", e)
            errorMsg = "AR Error: ${e.message}"
        }

        onDispose {
            try {
                // Unlink renderer first
                scanRenderer.session = null
                
                s?.pause()
                s?.close()
            } catch (e: Exception) {
                Log.e("ScanScreen", "Session close failed", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (errorMsg != null) {
            Text(
                text = errorMsg!!,
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Main AR View
            AndroidView(
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        preserveEGLContextOnPause = true
                        setEGLContextClientVersion(2)
                        setRenderer(scanRenderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // PIP Preview (Top End)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(150.dp, 200.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        setZOrderMediaOverlay(true) // On top of the other gl surface
                        setEGLContextClientVersion(2)
                        setRenderer(previewRenderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Text(
                "Result View",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(4.dp)
            )
        }



        // Top Controls (Distance + AF)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                 // AF Toggle
                 Button(
                     onClick = { scanViewModel.toggleAutofocus() },
                     modifier = Modifier.height(36.dp),
                     contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                 ) {
                     Text(if (uiState.isAutofocusEnabled) "AF: ON" else "AF: OFF")
                 }
                 
                 Spacer(modifier = Modifier.width(16.dp))
                 
                 // Distance
                 Text(
                     text = "%.0f cm".format(uiState.scanDistance * 100),
                     style = MaterialTheme.typography.displaySmall, 
                     color = Color.White
                 )
            }
        }
        
        // Sync AF State
        LaunchedEffect(uiState.isAutofocusEnabled) {
             session?.let { s -> 
                 scanEngine.setAutofocus(s, uiState.isAutofocusEnabled) 
             }
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Processing Bar
            if (uiState.status == ScanStatus.PROCESSING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(uiState.progress)
                            .height(8.dp)
                            .background(Color.Green)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Status Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                 val infoText = when(uiState.status) {
                     ScanStatus.SCANNING -> "Recording... Keyframes: ${uiState.keyframeCount}"
                     ScanStatus.PROCESSING -> "Merging Photos... ${(uiState.progress * 100).toInt()}%"
                     ScanStatus.VIEWING_RESULT -> "Complete! Points: ${uiState.pointCount}"
                     else -> "Ready to Record"
                 }
                 Text(
                     text = infoText,
                     style = MaterialTheme.typography.bodyLarge,
                     color = Color.White
                 )
            }

            // Controls
            Row(modifier = Modifier.fillMaxWidth()) {
                when (uiState.status) {
                    ScanStatus.IDLE -> {
                        Column(modifier = Modifier.fillMaxWidth()) {

                            Button(
                                onClick = {
                                    scanEngine.startScanning()
                                    scanViewModel.startScan()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Start Recording")
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {

                                    scanEngine.baseReset()
                                    scanViewModel.reset()


                                    context.findActivity()?.finish()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                    ScanStatus.SCANNING -> {
                        Button(
                            onClick = { 
                                scanEngine.stopAndProcess()
                                scanViewModel.stopScan() 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stop & Merge")
                        }
                    }
                    ScanStatus.PROCESSING -> {
                         Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text("Processing...")
                        }
                    }
                    ScanStatus.VIEWING_RESULT -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { 
                                    scanEngine.baseReset()
                                    scanViewModel.reset() 
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("New Scan")
                            }
                            Spacer(modifier = Modifier.padding(8.dp))
                            Button(
                                onClick = { 
                                    // Export OBJ
                                    // Unified Export
                                    val tag = (scanTag?.takeIf { it.isNotBlank() } ?: "Scan_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()))

                                    saveToAppScans(context, "${tag}.obj") {
                                        scanEngine.exportObj(it)
                                    }
                                    saveToAppScans(context, "${tag}.ply") {
                                        scanEngine.exportPly(it)
                                    }

                                    Toast.makeText(context, "Saved OBJ & PLY to app scans folder", Toast.LENGTH_LONG).show()


                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save (Mesh+Cloud)")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun saveToDownloads(context: Context, fileName: String, mimeType: String, writeBlock: (java.io.OutputStream) -> Unit) {
    val resolver = context.contentResolver
    val cv = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
    }
    
    try {
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { writeBlock(it) }
        }
    } catch (e: Exception) {
        Log.e("ScanScreen", "Export failed for $fileName", e)
    }
}

private fun saveToAppScans(context: Context, fileName: String, writeBlock: (java.io.OutputStream) -> Unit) {
    try {
        val dir = File(context.getExternalFilesDir(null), "scans")
        if (!dir.exists()) dir.mkdirs()

        val outFile = File(dir, fileName)
        outFile.outputStream().use { writeBlock(it) }

        Log.d("ScanScreen", "Saved to: ${outFile.absolutePath}")
    } catch (e: Exception) {
        Log.e("ScanScreen", "Export failed for $fileName", e)
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
