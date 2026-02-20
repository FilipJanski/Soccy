package com.example.soccy.ui.viewer

import android.opengl.GLSurfaceView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PointCloudViewer(
    modifier: Modifier = Modifier,
    filePath: String,
) {
    val renderer = remember { PointCloudRenderer() }

    // Załaduj punkty gdy zmieni się ścieżka
    LaunchedEffect(filePath) {
        val pts = PlyObjParser.loadPoints(filePath)
        renderer.setPoints(pts)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }
    )
}