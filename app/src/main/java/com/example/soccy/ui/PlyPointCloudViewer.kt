package com.example.soccy.ui

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color

@Composable
fun PlyPointCloudViewer(
    plyFile: File,
    modifier: Modifier = Modifier
) {

    val renderer = remember { PointCloudRenderer() }

    // Parsujemy punkty przy zmianie pliku
    var error by remember(plyFile.absolutePath) { mutableStateOf<String?>(null) }
    val points: FloatArray? by remember(plyFile.absolutePath) {
        mutableStateOf(
            runCatching {
                error = null
                readAsciiPlyXYZ(plyFile, maxPoints = 200_000)
            }.getOrElse {
                error = it.message ?: "Błąd PLY"
                null
            }
        )
    }

    error?.let { msg ->
        Text(text = msg, color = Color.Red)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val r = PointCloudRenderer()

            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                preserveEGLContextOnPause = true

                setRenderer(r)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                // zapamiętaj renderer
                tag = r

                // ustaw startowe punkty
                points?.let { r.setPointsNormalized(it) }
            }
        },
        update = { view ->
            val r = view.tag as? PointCloudRenderer ?: return@AndroidView
            points?.let { r.setPointsNormalized(it) }
        }
    )
}

private fun readAsciiPlyXYZ(file: File, maxPoints: Int = 200_000): FloatArray {
    file.bufferedReader().use { br ->
        var vertexCount = 0
        var formatAscii = false
        var headerEnded = false

        // --- header ---
        while (true) {
            val line = br.readLine() ?: break
            val t = line.trim()

            if (t.startsWith("format ")) {
                // obsługujemy tylko ASCII
                formatAscii = t.contains("ascii")
                if (!formatAscii) {
                    throw IllegalArgumentException("PLY nie jest ASCII (binary). Ten viewer obsługuje tylko ASCII.")
                }
            }
            if (t.startsWith("element vertex")) {
                vertexCount = t.split(Regex("\\s+")).last().toInt()
            }
            if (t == "end_header") {
                headerEnded = true
                break
            }
        }

        if (!headerEnded) throw IllegalArgumentException("Nieprawidłowy PLY: brak end_header.")
        if (!formatAscii) throw IllegalArgumentException("PLY nie jest ASCII.")
        if (vertexCount <= 0) return FloatArray(0)

        // --- data ---
        val step = if (vertexCount > maxPoints) (vertexCount / maxPoints).coerceAtLeast(1) else 1
        val outCount = (vertexCount / step).coerceAtMost(maxPoints)

        val data = FloatArray(outCount * 3)
        var outIdx = 0

        var i = 0
        while (i < vertexCount) {
            val line = br.readLine() ?: break

            if (i % step == 0) {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 3) {
                    val x = parts[0].toFloatOrNull()
                    val y = parts[1].toFloatOrNull()
                    val z = parts[2].toFloatOrNull()
                    if (x != null && y != null && z != null) {
                        data[outIdx++] = x
                        data[outIdx++] = y
                        data[outIdx++] = z
                    }
                }
            }
            i++
            if (outIdx >= data.size) break
        }

        return if (outIdx == data.size) data else data.copyOf(outIdx)
    }
}

class PointCloudRenderer : GLSurfaceView.Renderer {

    private var pointsBuffer: FloatBuffer? = null
    private var pointsCount: Int = 0

    private val mvp = FloatArray(16)
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private var program = 0

    private var aPos = 0
    private var uMvp = 0

    /**
     * Normalizuje chmurę punktów: centruje + skaluje do sensownego rozmiaru,
     * żeby „losowy pociąg z neta” nie był 10 km od kamery.
     */
    fun setPointsNormalized(points: FloatArray) {
        if (points.isEmpty()) return

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        var i = 0
        while (i + 2 < points.size) {
            val x = points[i]
            val y = points[i + 1]
            val z = points[i + 2]
            minX = min(minX, x); maxX = max(maxX, x)
            minY = min(minY, y); maxY = max(maxY, y)
            minZ = min(minZ, z); maxZ = max(maxZ, z)
            i += 3
        }

        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f

        val sizeX = maxX - minX
        val sizeY = maxY - minY
        val sizeZ = maxZ - minZ
        val maxSize = max(1e-6f, max(sizeX, max(sizeY, sizeZ)))
        val scale = 2.0f / maxSize

        val normalized = FloatArray(points.size)
        i = 0
        while (i + 2 < points.size) {
            normalized[i]     = (points[i]     - cx) * scale
            normalized[i + 1] = (points[i + 1] - cy) * scale
            normalized[i + 2] = (points[i + 2] - cz) * scale
            i += 3
        }

        pointsCount = normalized.size / 3
        pointsBuffer = ByteBuffer
            .allocateDirect(normalized.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(normalized)
                position(0)
            }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.06f, 0.06f, 0.06f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        val vs = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                gl_PointSize = 2.5;
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0, 0.78, 0.0, 1.0);
            }
        """.trimIndent()

        program = linkProgram(vs, fs)
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")

        Matrix.setIdentityM(model, 0)
        Matrix.setLookAtM(
            view, 0,
            0f, 0f, 3.0f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / max(1, height).toFloat()
        Matrix.perspectiveM(proj, 0, 60f, aspect, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val buf = pointsBuffer ?: return
        if (pointsCount <= 0) return

        Matrix.multiplyMM(mvp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, mvp, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)

        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 3 * 4, buf)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointsCount)
        GLES20.glDisableVertexAttribArray(aPos)
    }

    private fun linkProgram(vs: String, fs: String): Int {
        val v = compileShader(GLES20.GL_VERTEX_SHADER, vs)
        val f = compileShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        return p
    }

    private fun compileShader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        return s
    }
}