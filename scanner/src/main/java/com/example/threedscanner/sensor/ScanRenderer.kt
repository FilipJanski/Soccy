package com.example.threedscanner.sensor

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ScanRenderer(
    private val context: Context,
    private val cameraPoseState: CameraPoseState
) : GLSurfaceView.Renderer {

    var session: Session? = null
    var scanEngine: SimpleScanEngine? = null
    
    // Background rendering
    private var backgroundTextureId = -1
    private val quadVertices: FloatBuffer
    private val quadTexCoord: FloatBuffer
    private var backgroundProgram = 0
    private var backgroundPositionLoc = 0
    private var backgroundTexCoordLoc = 0
    private var backgroundTextureLoc = 0

    // Bounding Box
    private val boxVertices: FloatBuffer
    private var boxProgram = 0
    private var boxPositionLoc = 0
    private var boxMvpLoc = 0
    private var boxColorLoc = 0

    private var displayRotation = 0
    private var width = 0
    private var height = 0
    private var viewportChanged = false

    init {
        // Quad for background
        val quadCoords = floatArrayOf(
            -1f, -1f, 0f,
             -1f, 1f, 0f,
             1f, -1f, 0f,
             1f, 1f, 0f
        )
        val quadTex = floatArrayOf(
            0f, 1f,
            0f, 0f,
            1f, 1f,
            1f, 0f
        )
        quadVertices = pointBuffer(quadCoords)
        quadTexCoord = pointBuffer(quadTex)

        // Bounding Box (Matching ScanEngine)
        // X: -0.25 to 0.25
        // Y: -0.25 to 0.25
        // Z: -0.60 to -0.10 (10cm to 60cm in front)
        val minX = -0.25f; val maxX = 0.25f
        val minY = -0.25f; val maxY = 0.25f
        val minZ = -0.60f; val maxZ = -0.10f
        
        val len = 0.05f 
        val boxCoords = floatArrayOf(
            // Bottom-Front-Left corner
            minX, minY, minZ, minX+len, minY, minZ, // X+
            minX, minY, minZ, minX, minY+len, minZ, // Y+
            minX, minY, minZ, minX, minY, minZ+len, // Z+
            
            // Bottom-Front-Right corner
            maxX, minY, minZ, maxX-len, minY, minZ,
            maxX, minY, minZ, maxX, minY+len, minZ,
            maxX, minY, minZ, maxX, minY, minZ+len,
            
            // Bottom-Back-Left corner
            minX, minY, maxZ, minX+len, minY, maxZ,
            minX, minY, maxZ, minX, minY+len, maxZ,
            minX, minY, maxZ, minX, minY, maxZ-len, // Z-
            
            // Bottom-Back-Right corner
            maxX, minY, maxZ, maxX-len, minY, maxZ,
            maxX, minY, maxZ, maxX, minY+len, maxZ,
            maxX, minY, maxZ, maxX, minY, maxZ-len,

            // Top-Front-Left corner
            minX, maxY, minZ, minX+len, maxY, minZ,
            minX, maxY, minZ, minX, maxY-len, minZ, // Y-
            minX, maxY, minZ, minX, maxY, minZ+len,

            // Top-Front-Right corner
            maxX, maxY, minZ, maxX-len, maxY, minZ,
            maxX, maxY, minZ, maxX, maxY-len, minZ,
            maxX, maxY, minZ, maxX, maxY, minZ+len,

            // Top-Back-Left corner
            minX, maxY, maxZ, minX+len, maxY, maxZ,
            minX, maxY, maxZ, minX, maxY-len, maxZ,
            minX, maxY, maxZ, minX, maxY, maxZ-len,

            // Top-Back-Right corner
            maxX, maxY, maxZ, maxX-len, maxY, maxZ,
            maxX, maxY, maxZ, maxX, maxY-len, maxZ,
            maxX, maxY, maxZ, maxX, maxY, maxZ-len
        )
        boxVertices = pointBuffer(boxCoords)
    }

    private fun pointBuffer(arr: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(arr.size * 4)
        bb.order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(arr)
        fb.position(0)
        return fb
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        backgroundTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, backgroundTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

        // Background Shader
        val bgVShader = loadShader(GLES20.GL_VERTEX_SHADER, BG_VERTEX_SHADER)
        val bgFShader = loadShader(GLES20.GL_FRAGMENT_SHADER, BG_FRAGMENT_SHADER)
        backgroundProgram = createProgram(bgVShader, bgFShader)
        backgroundPositionLoc = GLES20.glGetAttribLocation(backgroundProgram, "a_Position")
        backgroundTexCoordLoc = GLES20.glGetAttribLocation(backgroundProgram, "a_TexCoord")
        backgroundTextureLoc = GLES20.glGetUniformLocation(backgroundProgram, "u_Texture")

        // Box Shader
        val boxVShader = loadShader(GLES20.GL_VERTEX_SHADER, BOX_VERTEX_SHADER)
        val boxFShader = loadShader(GLES20.GL_FRAGMENT_SHADER, BOX_FRAGMENT_SHADER)
        boxProgram = createProgram(boxVShader, boxFShader)
        boxPositionLoc = GLES20.glGetAttribLocation(boxProgram, "a_Position")
        boxMvpLoc = GLES20.glGetUniformLocation(boxProgram, "u_MvpMatrix")
        boxColorLoc = GLES20.glGetUniformLocation(boxProgram, "u_Color")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        viewportChanged = true
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        val s = session ?: return
        
        if (viewportChanged) {
            val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val displayManager = context.getSystemService(android.hardware.display.DisplayManager::class.java)
                displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.view.WindowManager::class.java).defaultDisplay
            }
            displayRotation = display?.rotation ?: 0
            s.setDisplayGeometry(displayRotation, width, height)
            viewportChanged = false
        }

        try {
            s.setCameraTextureName(backgroundTextureId)
            val frame = s.update()
            
            // Draw Background
            drawBackground(frame)
            
            // Process Scan
            scanEngine?.processFrame(frame)
            
            // Draw Box
            drawBox(frame)
            
            // Sync Pose for Preview
            updateCameraPose(frame.camera)
            
        } catch (e: Exception) {
            // Handle loss
        }
    }
    
    private fun drawBackground(frame: Frame) {
        GLES20.glUseProgram(backgroundProgram)
        GLES20.glDepthMask(false)
        
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, backgroundTextureId)
        
        GLES20.glVertexAttribPointer(backgroundPositionLoc, 3, GLES20.GL_FLOAT, false, 0, quadVertices)
        GLES20.glVertexAttribPointer(backgroundTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, quadTexCoord)
        
        GLES20.glEnableVertexAttribArray(backgroundPositionLoc)
        GLES20.glEnableVertexAttribArray(backgroundTexCoordLoc)
        
        // Transform tex coords
        val transformedTexCoords = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer()
        
        @Suppress("DEPRECATION")
        frame.transformDisplayUvCoords(quadTexCoord, transformedTexCoords)
        
        GLES20.glVertexAttribPointer(backgroundTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, transformedTexCoords)
        
        GLES20.glUniform1i(backgroundTextureLoc, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        GLES20.glDepthMask(true)
        GLES20.glDisableVertexAttribArray(backgroundPositionLoc)
        GLES20.glDisableVertexAttribArray(backgroundTexCoordLoc)
    }
    
    private fun drawBox(frame: Frame) {
        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) return

        val projMtx = FloatArray(16)
        camera.getProjectionMatrix(projMtx, 0, 0.1f, 100f)
        val viewMtx = FloatArray(16)
        camera.getViewMatrix(viewMtx, 0)
        val vpMtx = FloatArray(16)
        android.opengl.Matrix.multiplyMM(vpMtx, 0, projMtx, 0, viewMtx, 0)

        GLES20.glUseProgram(boxProgram)
        GLES20.glLineWidth(5f)

        GLES20.glVertexAttribPointer(boxPositionLoc, 3, GLES20.GL_FLOAT, false, 0, boxVertices)
        GLES20.glEnableVertexAttribArray(boxPositionLoc)

        GLES20.glUniformMatrix4fv(boxMvpLoc, 1, false, vpMtx, 0)
        GLES20.glUniform4f(boxColorLoc, 0f, 1f, 0f, 1f) // Green box

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 48)

        GLES20.glDisableVertexAttribArray(boxPositionLoc)
    }



    private fun updateCameraPose(camera: com.google.ar.core.Camera) {
        val projMtx = FloatArray(16)
        camera.getProjectionMatrix(projMtx, 0, 0.1f, 100f)
        val viewMtx = FloatArray(16)
        camera.getViewMatrix(viewMtx, 0)
        
        cameraPoseState.update(viewMtx, projMtx)
    }

    // Shaders
    companion object {
        const val BG_VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """
        const val BG_FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES u_Texture;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """
        
        const val BOX_VERTEX_SHADER = """
            uniform mat4 u_MvpMatrix;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_MvpMatrix * a_Position;
            }
        """
        const val BOX_FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """
        
        fun loadShader(type: Int, code: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
            return shader
        }
        
        fun createProgram(v: Int, f: Int): Int {
            val p = GLES20.glCreateProgram()
            GLES20.glAttachShader(p, v)
            GLES20.glAttachShader(p, f)
            GLES20.glLinkProgram(p)
            return p
        }
    }
    
    // Helper object for OES constant
    object GLES11Ext {
        const val GL_TEXTURE_EXTERNAL_OES = 0x8D65
    }
}
