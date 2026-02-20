package com.example.threedscanner.sensor

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PreviewRenderer(
    private val scanEngine: SimpleScanEngine,
    private val cameraPoseState: CameraPoseState
) : GLSurfaceView.Renderer {

    private var program = 0
    private var positionLoc = 0
    private var colorLoc = 0
    private var mvpLoc = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        
        val vShader = ScanRenderer.loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fShader = ScanRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = ScanRenderer.createProgram(vShader, fShader)
        
        positionLoc = GLES20.glGetAttribLocation(program, "a_Position")
        colorLoc = GLES20.glGetUniformLocation(program, "u_Color")
        mvpLoc = GLES20.glGetUniformLocation(program, "u_MvpMatrix")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        if (!cameraPoseState.hasData) return

        GLES20.glUseProgram(program)
        
        // Camera (Sync with AR View)
        val mvp = FloatArray(16)
        val proj = FloatArray(16)
        val view = FloatArray(16)
        
        cameraPoseState.getProjection(proj)
        cameraPoseState.getView(view)
        
        // MVP = Projection * View * Model(Identity)
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)
        
        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvp, 0)
        
        
        // Get Points (Live or Offline)
        val pointsArr = scanEngine.getDrawPoints()
        
        if (pointsArr != null && pointsArr.isNotEmpty()) {
            val numFloats = pointsArr.size
            val numPoints = numFloats / 3
            
            if (numPoints > 0) {
                 // Change color based on mode
                 // Orange for Result, Cyan for Live scan?
                 if (scanEngine.isScanning) {
                     GLES20.glUniform4f(colorLoc, 0f, 1f, 1f, 1f)
                 } else {
                     GLES20.glUniform4f(colorLoc, 1f, 0.5f, 0f, 1f)
                 }

                // Buffer setup
                val buffer = ByteBuffer.allocateDirect(numFloats * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                buffer.put(pointsArr)
                buffer.position(0)
                
                GLES20.glVertexAttribPointer(positionLoc, 3, GLES20.GL_FLOAT, false, 0, buffer)
                GLES20.glEnableVertexAttribArray(positionLoc)
                
                // Draw
                // Note: glPointSize in shader
                GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints)
                
                GLES20.glDisableVertexAttribArray(positionLoc)
            }
        }
    }

    companion object {
        const val VERTEX_SHADER = """
            uniform mat4 u_MvpMatrix;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_MvpMatrix * a_Position;
                gl_PointSize = 15.0; 
            }
        """
        const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                vec2 coord = gl_PointCoord - vec2(0.5);
                if(length(coord) > 0.5) {
                    discard;
                }
                gl_FragColor = u_Color;
            }
        """
    }
}
