package com.learnopengles.android.lesson5

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import com.learnopengles.android.R
import com.learnopengles.android.common.RawResourceReader
import com.learnopengles.android.common.ShaderHelper
import com.learnopengles.android.common.ShapeBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
class LessonFiveRenderer(private val mActivityContext: Context) : GLSurfaceView.Renderer {

    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private val mModelMatrix = FloatArray(16)

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private val mViewMatrix = FloatArray(16)

    /** Store the projection matrix. This is used to project the scene onto a 2D viewport.  */
    private val mProjectionMatrix = FloatArray(16)

    /** Allocate storage for the final combined matrix. This will be passed into the shader program.  */
    private val mMVPMatrix = FloatArray(16)

    /** Store our model data in a float buffer.  */
    private val mCubePositions: FloatBuffer
    private val mCubeColors: FloatBuffer

    /** This will be used to pass in the transformation matrix.  */
    private var mMVPMatrixHandle: Int = 0

    /** This will be used to pass in model position information.  */
    private var mPositionHandle: Int = 0

    /** This will be used to pass in model color information.  */
    private var mColorHandle: Int = 0

    /** How many bytes per float.  */
    private val mBytesPerFloat = 4

    /** Size of the position data in elements.  */
    private val mPositionDataSize = 3

    /** Size of the color data in elements.  */
    private val mColorDataSize = 4

    /** This is a handle to our cube shading program.  */
    private var mProgramHandle: Int = 0

    /** This will be used to switch between blending mode and regular mode.  */
    private var mBlending = true

    private val vertexShader: String?
        get() = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.color_vertex_shader)

    private val fragmentShader: String?
        get() = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.color_fragment_shader)

    init {

        // Define points for a cube.
        // X, Y, Z
        val p1p = floatArrayOf(-1.0f, 1.0f, 1.0f)
        val p2p = floatArrayOf(1.0f, 1.0f, 1.0f)
        val p3p = floatArrayOf(-1.0f, -1.0f, 1.0f)
        val p4p = floatArrayOf(1.0f, -1.0f, 1.0f)
        val p5p = floatArrayOf(-1.0f, 1.0f, -1.0f)
        val p6p = floatArrayOf(1.0f, 1.0f, -1.0f)
        val p7p = floatArrayOf(-1.0f, -1.0f, -1.0f)
        val p8p = floatArrayOf(1.0f, -1.0f, -1.0f)

        val cubePositionData = ShapeBuilder.generateCubeData(p1p, p2p, p3p, p4p, p5p, p6p, p7p, p8p, p1p.size)

        // Points of the cube: color information
        // R, G, B, A
        val p1c = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)        // red
        val p2c = floatArrayOf(1.0f, 0.0f, 1.0f, 1.0f)        // magenta
        val p3c = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)        // black
        val p4c = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)        // blue
        val p5c = floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f)        // yellow
        val p6c = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)        // white
        val p7c = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)        // green
        val p8c = floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f)        // cyan

        val cubeColorData = ShapeBuilder.generateCubeData(p1c, p2c, p3c, p4c, p5c, p6c, p7c, p8c, p1c.size)

        // Initialize the buffers.
        mCubePositions = ByteBuffer.allocateDirect(cubePositionData.size * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubePositions.put(cubePositionData).position(0)

        mCubeColors = ByteBuffer.allocateDirect(cubeColorData.size * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubeColors.put(cubeColorData).position(0)
    }

    fun switchMode() {
        mBlending = !mBlending

        if (mBlending) {
            // No culling of back faces
            GLES20.glDisable(GLES20.GL_CULL_FACE)

            // No depth testing
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)

            // Enable blending
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)
        } else {
            // Cull back faces
            GLES20.glEnable(GLES20.GL_CULL_FACE)

            // Enable depth testing
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

            // Disable blending
            GLES20.glDisable(GLES20.GL_BLEND)
        }
    }

    override fun onSurfaceCreated(glUnused: GL10, config: EGLConfig) {
        // Set the background clear color to black.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        // No culling of back faces
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        // No depth testing
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        // Enable blending
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)
        //		GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);

        // Position the eye in front of the origin.
        val eyeX = 0.0f
        val eyeY = 0.0f
        val eyeZ = -0.5f

        // We are looking toward the distance
        val lookX = 0.0f
        val lookY = 0.0f
        val lookZ = -5.0f

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        val upX = 0.0f
        val upY = 1.0f
        val upZ = 0.0f

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ)

        val vertexShader = vertexShader
        val fragmentShader = fragmentShader

        val vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

        mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                arrayOf("a_Position", "a_Color"))
    }

    override fun onSurfaceChanged(glUnused: GL10, width: Int, height: Int) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height)

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        val ratio = width.toFloat() / height
        val left = -ratio
        val bottom = -1.0f
        val top = 1.0f
        val near = 1.0f
        val far = 10.0f

        Matrix.frustumM(mProjectionMatrix, 0, left, ratio, bottom, top, near, far)
    }

    override fun onDrawFrame(glUnused: GL10) {
        if (mBlending) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        } else {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        }

        // Do a complete rotation every 10 seconds.
        val time = SystemClock.uptimeMillis() % 10000L
        val angleInDegrees = 360.0f / 10000.0f * time.toInt()

        // Set our program
        GLES20.glUseProgram(mProgramHandle)

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix")
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position")
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color")

        // Draw some cubes.
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, 4.0f, 0.0f, -7.0f)
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 0.0f, 0.0f)
        drawCube()

        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, -4.0f, 0.0f, -7.0f)
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f)
        drawCube()

        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, 0.0f, 4.0f, -7.0f)
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f)
        drawCube()

        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, 0.0f, -4.0f, -7.0f)
        drawCube()

        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f)
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0.0f)
        drawCube()
    }

    /**
     * Draws a cube.
     */
    private fun drawCube() {
        // Pass in the position information
        mCubePositions.position(0)
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, mCubePositions)

        GLES20.glEnableVertexAttribArray(mPositionHandle)

        // Pass in the color information
        mCubeColors.position(0)
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                0, mCubeColors)

        GLES20.glEnableVertexAttribArray(mColorHandle)

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)

        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)
    }
}
