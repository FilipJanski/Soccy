package com.example.threedscanner.sensor

import android.content.Context
import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.Session
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.ByteOrder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class SimpleScanEngine(private val context: Context) {

    // --- Data Structures ---
    data class Keyframe(
        val timestamp: Long,
        val pose: com.google.ar.core.Pose,
        val depthData: ShortArray,
        val width: Int,
        val height: Int,
        val intrinsics: com.google.ar.core.CameraIntrinsics
    )
    
    // Voxel accumulator for denoising
    data class VoxelData(
        var xSum: Float = 0f,
        var ySum: Float = 0f,
        var zSum: Float = 0f,
        var count: Int = 0
    )

    // Running state
    private val keyframes = ArrayList<Keyframe>()
    private val points = ArrayList<FloatArray>() // The FINAL filtered point cloud
    private var livePreviewPoints: FloatArray? = null
    private val livePreviewLock = Any()

    private val voxelGrid = java.util.HashMap<String, VoxelData>()
    
    // State machine
    var isScanning = false
        private set
    var isProcessing = false
        private set

    // Scan Origin (Anchor for the bounding box)
    private var startPose: com.google.ar.core.Pose? = null
        
    // --- Parameters ---
    private val LEAF_SIZE = 0.005f // 5mm
    
    // Expanded World Box - Relative to Start Pose
    // 1.2m Wide/Tall, 1.5m Deep
    private val boxMin = floatArrayOf(-0.6f, -1.0f, -1.5f)
    private val boxMax = floatArrayOf(0.6f, 1.0f, -0.2f) // Starts 20cm in front
    private val MAX_RANGE_MM = 1500 
    
    // Capture thresholds
    private val MIN_MOVE_DIST = 0.05f 
    private var lastKeyframePose: com.google.ar.core.Pose? = null

    // UI Callbacks
    var onKeyframeAdded: ((Int) -> Unit)? = null
    var onProcessingProgress: ((Float) -> Unit)? = null
    var onProcessingComplete: (() -> Unit)? = null
    var onDistanceChange: ((Float) -> Unit)? = null
    var onScanResultReady: ((Int) -> Unit)? = null

    // ---------------------------------------------------------

    fun onResume(session: Session) {
        val config = session.config
        if (session.isDepthModeSupported(com.google.ar.core.Config.DepthMode.AUTOMATIC)) {
            config.depthMode = com.google.ar.core.Config.DepthMode.AUTOMATIC
        }
        // Default to Auto Focus
        config.focusMode = com.google.ar.core.Config.FocusMode.AUTO
        session.configure(config)
    }

    fun setAutofocus(session: Session, enabled: Boolean) {
        val config = session.config
        config.focusMode = if (enabled) com.google.ar.core.Config.FocusMode.AUTO else com.google.ar.core.Config.FocusMode.FIXED
        session.configure(config)
    }
    
    fun startScanning() {
        keyframes.clear()
        points.clear()
        voxelGrid.clear()
        lastKeyframePose = null
        startPose = null // Will be set on first frame
        isScanning = true
        isProcessing = false
        notifyKeyframes()
    }

    fun stopAndProcess() {
        isScanning = false
        isProcessing = true
        
        // Launch processing
        GlobalScope.launch(Dispatchers.Default) {
            reconstruct()
        }
    }
    
    fun baseReset() {
        keyframes.clear()
        points.clear()
        voxelGrid.clear()
        lastKeyframePose = null
        startPose = null
        isScanning = false
        isProcessing = false
        notifyKeyframes()
    }

    fun processFrame(frame: Frame) {
        try {
             val dist = estimateDistance(frame)
             if (dist != null) onDistanceChange?.invoke(dist)
             
             if (isScanning) {
                 updateLivePreview(frame)
             }

        } catch (e: Exception) {}

        if (!isScanning) return

        try {
            val camera = frame.camera
            if (camera.trackingState != com.google.ar.core.TrackingState.TRACKING) return
            
            val currentPose = camera.pose
            
            if (startPose == null) {
                startPose = currentPose // Anchor box here
            }
            
            var shouldCapture = false
            if (lastKeyframePose == null) {
                shouldCapture = true
            } else {
                val last = lastKeyframePose!!
                val dx = currentPose.tx() - last.tx()
                val dy = currentPose.ty() - last.ty()
                val dz = currentPose.tz() - last.tz()
                val distSq = dx*dx + dy*dy + dz*dz
                
                if (distSq > MIN_MOVE_DIST * MIN_MOVE_DIST) {
                    shouldCapture = true
                }
            }
            
            if (shouldCapture) {
                captureKeyframe(frame, currentPose)
            }
            
        } catch (e: Exception) {
            Log.e("ScanEngine", "Frame error", e)
        }
    }
    
    private fun estimateDistance(frame: Frame): Float? {
        val depthImage = frame.acquireDepthImage16Bits()
        try {
            val width = depthImage.width
            val height = depthImage.height
            val buffer = depthImage.planes[0].buffer
            val cx = width/2
            val cy = height/2
            val index = (cy * width + cx) * 2
            val d1 = buffer.get(index).toInt() and 0xFF
            val d2 = buffer.get(index + 1).toInt() and 0xFF
            val depthMm = (d2 shl 8) or d1
            if (depthMm > 0) return depthMm / 1000.0f
        } catch (e: Exception) { return null }
        finally { depthImage.close() }
        return null
    }

    private fun captureKeyframe(frame: Frame, pose: com.google.ar.core.Pose) {
        val depthImage = frame.acquireDepthImage16Bits()
        try {
            val w = depthImage.width
            val h = depthImage.height
            val buffer = depthImage.planes[0].buffer
            
            val len = w * h
            val data = ShortArray(len)
            
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            val tempShortBuffer = buffer.asShortBuffer()
            tempShortBuffer.get(data)
            
            val kf = Keyframe(
                System.currentTimeMillis(),
                pose,
                data,
                w,
                h,
                frame.camera.imageIntrinsics
            )
            
            synchronized(keyframes) {
                keyframes.add(kf)
            }
            lastKeyframePose = pose
            notifyKeyframes()
            
        } catch (e: Exception) {
            Log.e("ScanEngine", "Capture failed", e)
        } finally {
            depthImage.close()
        }
    }
    
    private fun updateLivePreview(frame: Frame) {
        val depthImage = frame.acquireDepthImage16Bits()
        try {
             val w = depthImage.width
             val h = depthImage.height
             val buffer = depthImage.planes[0].buffer
             buffer.order(ByteOrder.LITTLE_ENDIAN)
             
             val step = 8 // Faster for preview
             val floats = ArrayList<Float>()
             val intrinsics = frame.camera.imageIntrinsics
             val dim = intrinsics.imageDimensions
             val fx = intrinsics.focalLength[0] * (w.toFloat() / dim[0].toFloat())
             val fy = intrinsics.focalLength[1] * (h.toFloat() / dim[1].toFloat())
             val cx = intrinsics.principalPoint[0] * (w.toFloat() / dim[0].toFloat())
             val cy = intrinsics.principalPoint[1] * (h.toFloat() / dim[1].toFloat())
             
             val modelMtx = FloatArray(16)
             frame.camera.pose.toMatrix(modelMtx, 0)
             
             for (y in 0 until h step step) {
                 for (x in 0 until w step step) {
                     val idx = (y * w + x) * 2
                     val d1 = buffer.get(idx).toInt() and 0xFF
                     val d2 = buffer.get(idx + 1).toInt() and 0xFF
                     val depthMm = (d2 shl 8) or d1
                     
                     if (depthMm > 200 && depthMm < MAX_RANGE_MM) {
                         val zM = depthMm / 1000.0f
                         val xLocal = (x - cx) * zM / fx
                         val yLocal = -(y - cy) * zM / fy
                         val zLocal = -zM
                         
                         val xw = modelMtx[0] * xLocal + modelMtx[4] * yLocal + modelMtx[8] * zLocal + modelMtx[12]
                         val yw = modelMtx[1] * xLocal + modelMtx[5] * yLocal + modelMtx[9] * zLocal + modelMtx[13]
                         val zw = modelMtx[2] * xLocal + modelMtx[6] * yLocal + modelMtx[10] * zLocal + modelMtx[14]
                         
                         floats.add(xw)
                         floats.add(yw)
                         floats.add(zw)
                     }
                 }
             }
             
             synchronized(livePreviewLock) {
                 livePreviewPoints = floats.toFloatArray()
             }
             
        } catch (e: Exception) {
        } finally {
            depthImage.close()
        }
    }

    private fun reconstruct() {
        Log.d("ScanEngine", "Starting reconstruction...")
        
        synchronized(points) { points.clear() }
        synchronized(voxelGrid) { voxelGrid.clear() }
        
        // LEAF_SIZE used here
        
        val total = keyframes.size
        if (total == 0) {
            isProcessing = false
            GlobalScope.launch(Dispatchers.Main) { onProcessingComplete?.invoke() }
            return
        }
        
        // Prepare Start Pose Inverse Matrix for ROI check
        val anchorMatrix = FloatArray(16)
        val anchorInverse = FloatArray(16)
        if (startPose != null) {
            startPose!!.toMatrix(anchorMatrix, 0)
            android.opengl.Matrix.invertM(anchorInverse, 0, anchorMatrix, 0)
        } else {
            android.opengl.Matrix.setIdentityM(anchorInverse, 0)
        }
        
        var processed = 0
        val modelMatrix = FloatArray(16)
        
        // Reuse temporary point array
        val pointWorld = FloatArray(4) // x,y,z,1
        val pointLocal = FloatArray(4) // Result
        
        for (kf in keyframes) {
            kf.pose.toMatrix(modelMatrix, 0)
            
            val fx = kf.intrinsics.focalLength[0]
            val fy = kf.intrinsics.focalLength[1]
            val cx = kf.intrinsics.principalPoint[0]
            val cy = kf.intrinsics.principalPoint[1]

            val dim = kf.intrinsics.imageDimensions
            val scaleX = kf.width.toFloat() / dim[0].toFloat()
            val scaleY = kf.height.toFloat() / dim[1].toFloat()
            
            val sFx = fx * scaleX
            val sFy = fy * scaleY
            val sCx = cx * scaleX
            val sCy = cy * scaleY
            
            val stride = 2 
            
            for (y in 0 until kf.height step stride) {
                for (x in 0 until kf.width step stride) {
                    val idx = y * kf.width + x
                    val depthMm = kf.depthData[idx].toInt() and 0xFFFF
                    
                    if (depthMm > 100 && depthMm < MAX_RANGE_MM) { // Min 10cm, Max Range
                         val zM = depthMm / 1000.0f
                         
                         val xCam = (x - sCx) * zM / sFx
                         val yCam = -(y - sCy) * zM / sFy 
                         val zCam = -zM
                         
                         // World Space
                         val xw = modelMatrix[0] * xCam + modelMatrix[4] * yCam + modelMatrix[8] * zCam + modelMatrix[12]
                         val yw = modelMatrix[1] * xCam + modelMatrix[5] * yCam + modelMatrix[9] * zCam + modelMatrix[13]
                         val zw = modelMatrix[2] * xCam + modelMatrix[6] * yCam + modelMatrix[10] * zCam + modelMatrix[14]
                         
                         // Relative Box Check: Transform World Point to Anchor Space
                         pointWorld[0] = xw
                         pointWorld[1] = yw
                         pointWorld[2] = zw
                         pointWorld[3] = 1.0f
                         
                         android.opengl.Matrix.multiplyMV(pointLocal, 0, anchorInverse, 0, pointWorld, 0)
                         
                         val xr = pointLocal[0]
                         val yr = pointLocal[1]
                         val zr = pointLocal[2]
                         
                         if (xr >= boxMin[0] && xr <= boxMax[0] &&
                             yr >= boxMin[1] && yr <= boxMax[1] &&
                             zr >= boxMin[2] && zr <= boxMax[2]) {
                             
                             // Voxel Grid (Density Filter)
                             val xi = kotlin.math.floor(xw / LEAF_SIZE).toInt()
                             val yi = kotlin.math.floor(yw / LEAF_SIZE).toInt()
                             val zi = kotlin.math.floor(zw / LEAF_SIZE).toInt()
                             val key = "$xi,$yi,$zi"
                             
                             val voxel = voxelGrid.getOrPut(key) { VoxelData() }
                             voxel.xSum += xw
                             voxel.ySum += yw
                             voxel.zSum += zw
                             voxel.count++
                        }
                    }
                }
            }
            
            processed++
            onProcessingProgress?.invoke(processed.toFloat() / total.toFloat())
        }
        
        // Filter and Flatten
        val minVoxelCount = 1 // Relaxed filter: Even single points are kept to ensure output
        
        synchronized(points) {
            for (v in voxelGrid.values) {
                if (v.count >= minVoxelCount) {
                    points.add(floatArrayOf(v.xSum / v.count, v.ySum / v.count, v.zSum / v.count))
                }
            }
        }
        
        Log.d("ScanEngine", "Reconstruction complete. Points: ${points.size}")
        isProcessing = false
        
        GlobalScope.launch(Dispatchers.Main) {
            onProcessingComplete?.invoke()
            onScanResultReady?.invoke(points.size)
        }
    }

    fun getDrawPoints(): FloatArray? {
        if (isScanning) {
            synchronized(livePreviewLock) {
                return livePreviewPoints 
            }
        } else {
             synchronized(points) {
                 if (points.isEmpty()) return null
                 val out = FloatArray(points.size * 3)
                 var i = 0
                 for (p in points) {
                     out[i++] = p[0]
                     out[i++] = p[1]
                     out[i++] = p[2]
                 }
                 return out
             }
        }
    }

    fun getPoints(): List<FloatArray> {
        return points 
    }
    
    private fun notifyKeyframes() {
        onKeyframeAdded?.invoke(keyframes.size)
    }

    fun exportObj(outputStream: java.io.OutputStream) {
        val keys = synchronized(voxelGrid) { voxelGrid.keys.toSet() } 
        // We use the keys (indices) to build a hull mesh
        
        Log.d("ScanEngine", "Exporting Mesh OBJ from ${keys.size} voxels")
        
        try {
            val writer = BufferedWriter(OutputStreamWriter(outputStream))
            
            writer.write("# 3D Scan Voxel Mesh\n")
            writer.write("o ScanMesh\n")
            
            // Helper to parse key
            fun parseKey(k: String): Triple<Int, Int, Int> {
                val parts = k.split(",")
                return Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            }
            
            // Collect all vertices first to index them? 
            // Naive approach: Write vertices/faces for each voxel face individually. 
            // It creates duplicate vertices but is O(N) and essentially streaming.
            // OBJ files allow this.
            
            var vOffset = 1
            val half = LEAF_SIZE / 2f
            
            // Neighbor offsets
            val neighbors = arrayOf(
                Triple(1, 0, 0), Triple(-1, 0, 0),
                Triple(0, 1, 0), Triple(0, -1, 0),
                Triple(0, 0, 1), Triple(0, 0, -1)
            )
            
            // Pre-hashed set for lookup
            val occupied = HashSet<String>(keys)
            
            for (k in keys) {
                val (x, y, z) = parseKey(k)
                
                // Center of this voxel in world space
                // Use (x+0.5)*size to center it or just x*size.
                // Since during reconstruct we did floor(xw/size), the voxel x spans [x*size, (x+1)*size].
                // Center is (x+0.5)*size.
                val cx = (x + 0.5f) * LEAF_SIZE
                val cy = (y + 0.5f) * LEAF_SIZE
                val cz = (z + 0.5f) * LEAF_SIZE
                
                for ((nx, ny, nz) in neighbors) {
                    val nk = "${x+nx},${y+ny},${z+nz}"
                    if (!occupied.contains(nk)) {
                        // Face is exposed! Draw it.
                        // Face normal is (nx, ny, nz)
                        
                        // Vertices for the face. 
                        // Start relative to center
                        // Right Face (+X): (0.5, -0.5, -0.5), (0.5, 0.5, -0.5), (0.5, 0.5, 0.5), (0.5, -0.5, 0.5)
                        // ...
                        
                        // Face is exposed! Draw it.
                        // Face normal is (nx, ny, nz)
                        
                        // Vertices for the face. 
                        // Start relative to center
                        // Right Face (+X): (0.5, -0.5, -0.5), (0.5, 0.5, -0.5), (0.5, 0.5, 0.5), (0.5, -0.5, 0.5)
                        
                        val corners = ArrayList<FloatArray>()
                        
                        // Vertices are relative to cx,cy,cz
                        if (nx == 1) { // Right Face
                             corners.add(floatArrayOf( half, -half,  half))
                             corners.add(floatArrayOf( half, -half, -half))
                             corners.add(floatArrayOf( half,  half, -half))
                             corners.add(floatArrayOf( half,  half,  half))
                        } else if (nx == -1) { // Left
                             corners.add(floatArrayOf(-half, -half, -half))
                             corners.add(floatArrayOf(-half, -half,  half))
                             corners.add(floatArrayOf(-half,  half,  half))
                             corners.add(floatArrayOf(-half,  half, -half))
                        } else if (ny == 1) { // Top
                             corners.add(floatArrayOf(-half,  half,  half))
                             corners.add(floatArrayOf(-half,  half, -half))
                             corners.add(floatArrayOf( half,  half, -half))
                             corners.add(floatArrayOf( half,  half,  half))
                        } else if (ny == -1) { // Bottom
                             corners.add(floatArrayOf(-half, -half, -half))
                             corners.add(floatArrayOf(-half, -half,  half))
                             corners.add(floatArrayOf( half, -half,  half))
                             corners.add(floatArrayOf( half, -half, -half))
                        } else if (nz == 1) { // Front
                             corners.add(floatArrayOf(-half, -half,  half))
                             corners.add(floatArrayOf( half, -half,  half))
                             corners.add(floatArrayOf( half,  half,  half))
                             corners.add(floatArrayOf(-half,  half,  half))
                        } else if (nz == -1) { // Back
                             corners.add(floatArrayOf( half, -half, -half))
                             corners.add(floatArrayOf(-half, -half, -half))
                             corners.add(floatArrayOf(-half,  half, -half))
                             corners.add(floatArrayOf( half,  half, -half))
                        }
                        
                        for (c in corners) {
                            writer.write(String.format(Locale.US, "v %.6f %.6f %.6f\n", cx+c[0], cy+c[1], cz+c[2]))
                        }
                        
                        // Write Quad Face
                        writer.write("f $vOffset ${vOffset+1} ${vOffset+2} ${vOffset+3}\n")
                        vOffset += 4
                    }
                }
            }
            
            writer.flush()
            writer.close() 
            
        } catch (e: Exception) {
            Log.e("ScanEngine", "Export error", e)
            try { outputStream.write("# Error: ${e.message}".toByteArray()) } catch (_: Exception) {}
        }
    }

    fun exportPly(outputStream: java.io.OutputStream) {
        val currentPoints = synchronized(points) { ArrayList(points) }
        Log.d("ScanEngine", "Exporting PLY: ${currentPoints.size} points")
        
        try {
            val writer = BufferedWriter(OutputStreamWriter(outputStream))
            
            writer.write("ply\n")
            writer.write("format ascii 1.0\n")
            writer.write("element vertex ${currentPoints.size}\n")
            writer.write("property float x\n")
            writer.write("property float y\n")
            writer.write("property float z\n")
            writer.write("end_header\n")
            
            for (p in currentPoints) {
                writer.write(String.format(Locale.US, "%.6f %.6f %.6f\n", p[0], p[1], p[2]))
            }
            
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            Log.e("ScanEngine", "Export PLY error", e)
        }
    }
}
