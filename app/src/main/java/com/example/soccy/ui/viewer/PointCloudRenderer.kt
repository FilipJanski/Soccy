package com.example.soccy.ui.viewer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class PointCloudRenderer : GLSurfaceView.Renderer {

    private var program = 0
    private var aPos = 0
    private var uMvp = 0

    private var pointBuffer: FloatBuffer? = null
    private var pointCount = 0

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    private var width = 1
    private var height = 1
    private var t = 0f

    fun setPoints(points: FloatArray) {
        pointCount = points.size / 3
        pointBuffer = ByteBuffer
            .allocateDirect(points.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(points)
                position(0)
            }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        val vs = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                gl_PointSize = 2.0;
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0, 0.8, 0.2, 1.0);
            }
        """.trimIndent()

        program = linkProgram(vs, fs)
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w.coerceAtLeast(1)
        height = h.coerceAtLeast(1)
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(proj, 0, 60f, ratio, 0.1f, 200f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val buf = pointBuffer ?: return
        if (pointCount <= 0) return


        t += 0.01f
        val camX = cos(t) * 2.5f
        val camZ = sin(t) * 2.5f
        Matrix.setLookAtM(view, 0, camX, 1.5f, camZ, 0f, 1f, 0f, 0f, 1f, 0f)

        Matrix.setIdentityM(model, 0)

        Matrix.multiplyMM(mvp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, mvp, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)

        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 3 * 4, buf)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointCount)

        GLES20.glDisableVertexAttribArray(aPos)
    }

    private fun linkProgram(vsSrc: String, fsSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSrc)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
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