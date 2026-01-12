@file:Suppress("unused")
package com.learnopengles.android.lesson8

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.learnopengles.android.R
import com.learnopengles.android.common.RawResourceReader
import com.learnopengles.android.common.ShaderHelper
import com.learnopengles.android.lesson8.ErrorHandler.ErrorType
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class implements our custom renderer. Note that the GL10 parameter
 * passed in is unused for OpenGL ES 2.0 renderers -- the static class GLES20 is
 * used instead.
 */
class LessonEightRenderer
/**
 * Initialize the model data.
 */
(
        /** References to other main objects.  */
        private val lessonEightActivity: LessonEightActivity, private val errorHandler: ErrorHandler)// GLES20 = new AndroidGL20();
    : GLSurfaceView.Renderer {

    /*
	 * Android's OpenGL bindings are broken until Gingerbread, so we use LibGDX
	 * bindings here.
	 */
    // private final AndroidGL20 GLES20;

    /**
     * Store the model matrix. This matrix is used to move models from object
     * space (where each model can be thought of being located at the center of
     * the universe) to world space.
     */
    private val modelMatrix = FloatArray(16)

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix
     * transforms world space to eye space; it positions things relative to our
     * eye.
     */
    private val viewMatrix = FloatArray(16)

    /**
     * Store the projection matrix. This is used to project the scene onto a 2D
     * viewport.
     */
    private val projectionMatrix = FloatArray(16)

    /**
     * Allocate storage for the final combined matrix. This will be passed into
     * the shader program.
     */
    private val mvpMatrix = FloatArray(16)

    /** Additional matrices.  */
    private val accumulatedRotation = FloatArray(16)
    private val currentRotation = FloatArray(16)
    private val lightModelMatrix = FloatArray(16)
    private val temporaryMatrix = FloatArray(16)

    /** OpenGL handles to our program uniforms.  */
    private var mvpMatrixUniform: Int = 0
    private var mvMatrixUniform: Int = 0
    private var lightPosUniform: Int = 0

    /** OpenGL handles to our program attributes.  */
    private var positionAttribute: Int = 0
    private var normalAttribute: Int = 0
    private var colorAttribute: Int = 0

    /**
     * Used to hold a light centered on the origin in model space. We need a 4th
     * coordinate so we can get translations to work when we multiply this by
     * our transformation matrices.
     */
    private val lightPosInModelSpace = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

    /**
     * Used to hold the current position of the light in world space (after
     * transformation via model matrix).
     */
    private val lightPosInWorldSpace = FloatArray(4)

    /**
     * Used to hold the transformed position of the light in eye space (after
     * transformation via modelview matrix)
     */
    private val lightPosInEyeSpace = FloatArray(4)

    /** This is a handle to our cube shading program.  */
    private var program: Int = 0

    /** Retain the most recent delta for touch events.  */
    // These still work without volatile, but refreshes are not guaranteed to
    // happen.
    @Volatile
    var deltaX: Float = 0.toFloat()
    @Volatile
    var deltaY: Float = 0.toFloat()

    /** The current heightmap object.  */
    private var heightMap: HeightMap? = null

    override fun onSurfaceCreated(glUnused: GL10, config: EGLConfig) {
        heightMap = HeightMap()

        // Set the background clear color to black.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Position the eye in front of the origin.
        val eyeX = 0.0f
        val eyeY = 0.0f
        val eyeZ = -0.5f

        // We are looking toward the distance
        val lookX = 0.0f
        val lookY = 0.0f
        val lookZ = -5.0f

        // Set our up vector. This is where our head would be pointing were we
        // holding the camera.
        val upX = 0.0f
        val upY = 1.0f
        val upZ = 0.0f

        // Set the view matrix. This matrix can be said to represent the camera
        // position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination
        // of a model and view matrix. In OpenGL 2, we can keep track of these
        // matrices separately if we choose.
        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ)

        val vertexShader = RawResourceReader.readTextFileFromRawResource(lessonEightActivity,
                R.raw.per_pixel_vertex_shader_no_tex)
        val fragmentShader = RawResourceReader.readTextFileFromRawResource(lessonEightActivity,
                R.raw.per_pixel_fragment_shader_no_tex)

        val vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader!!)
        val fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader!!)

        program = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, arrayOf(POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE))

        // Initialize the accumulated rotation matrix
        Matrix.setIdentityM(accumulatedRotation, 0)
    }

    override fun onSurfaceChanged(glUnused: GL10, width: Int, height: Int) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height)

        // Create a new perspective projection matrix. The height will stay the
        // same while the width will vary as per aspect ratio.
        val ratio = width.toFloat() / height
        val left = -ratio
        val bottom = -1.0f
        val top = 1.0f
        val near = 1.0f
        val far = 1000.0f

        Matrix.frustumM(projectionMatrix, 0, left, ratio, bottom, top, near, far)
    }

    override fun onDrawFrame(glUnused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(program)

        // Set program handles for cube drawing.
        mvpMatrixUniform = GLES20.glGetUniformLocation(program, MVP_MATRIX_UNIFORM)
        mvMatrixUniform = GLES20.glGetUniformLocation(program, MV_MATRIX_UNIFORM)
        lightPosUniform = GLES20.glGetUniformLocation(program, LIGHT_POSITION_UNIFORM)
        positionAttribute = GLES20.glGetAttribLocation(program, POSITION_ATTRIBUTE)
        normalAttribute = GLES20.glGetAttribLocation(program, NORMAL_ATTRIBUTE)
        colorAttribute = GLES20.glGetAttribLocation(program, COLOR_ATTRIBUTE)

        // Calculate position of the light. Push into the distance.
        Matrix.setIdentityM(lightModelMatrix, 0)
        Matrix.translateM(lightModelMatrix, 0, 0.0f, 7.5f, -8.0f)

        Matrix.multiplyMV(lightPosInWorldSpace, 0, lightModelMatrix, 0, lightPosInModelSpace, 0)
        Matrix.multiplyMV(lightPosInEyeSpace, 0, viewMatrix, 0, lightPosInWorldSpace, 0)

        // Draw the heightmap.
        // Translate the heightmap into the screen.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -12f)

        // Set a matrix that contains the current rotation.
        Matrix.setIdentityM(currentRotation, 0)
        Matrix.rotateM(currentRotation, 0, deltaX, 0.0f, 1.0f, 0.0f)
        Matrix.rotateM(currentRotation, 0, deltaY, 1.0f, 0.0f, 0.0f)
        deltaX = 0.0f
        deltaY = 0.0f

        // Multiply the current rotation by the accumulated rotation, and then
        // set the accumulated rotation to the result.
        Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotation, 0)
        System.arraycopy(temporaryMatrix, 0, accumulatedRotation, 0, 16)

        // Rotate the cube taking the overall rotation into account.
        Matrix.multiplyMM(temporaryMatrix, 0, modelMatrix, 0, accumulatedRotation, 0)
        System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16)

        // This multiplies the view matrix by the model matrix, and stores
        // the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0)

        // This multiplies the modelview matrix by the projection matrix,
        // and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16)

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0)

        // Pass in the light position in eye space.
        GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2])

        // Render the heightmap.
        heightMap!!.render()
    }

    internal inner class HeightMap {

        private val vbo = IntArray(1)
        private val ibo = IntArray(1)

        private var indexCount: Int = 0

        init {
            try {
                val floatsPerVertex = (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS
                        + COLOR_DATA_SIZE_IN_ELEMENTS)
                val xLength = SIZE_PER_SIDE
                val yLength = SIZE_PER_SIDE

                val heightMapVertexData = FloatArray(xLength * yLength * floatsPerVertex)

                var offset = 0

                // First, build the data for the vertex buffer
                for (y in 0 until yLength) {
                    for (x in 0 until xLength) {
                        val xRatio = x / (xLength - 1).toFloat()

                        // Build our heightmap from the top down, so that our triangles are counter-clockwise.
                        val yRatio = 1f - y / (yLength - 1).toFloat()

                        val xPosition = MIN_POSITION + xRatio * POSITION_RANGE
                        val yPosition = MIN_POSITION + yRatio * POSITION_RANGE

                        // Position
                        heightMapVertexData[offset++] = xPosition
                        heightMapVertexData[offset++] = yPosition
                        heightMapVertexData[offset++] = (xPosition * xPosition + yPosition * yPosition) / 10f

                        // Cheap normal using a derivative of the function.
                        // The slope for X will be 2X, for Y will be 2Y.
                        // Divide by 10 since the position's Z is also divided by 10.
                        val xSlope = 2 * xPosition / 10f
                        val ySlope = 2 * yPosition / 10f

                        // Calculate the normal using the cross product of the slopes.
                        val planeVectorX = floatArrayOf(1f, 0f, xSlope)
                        val planeVectorY = floatArrayOf(0f, 1f, ySlope)
                        val normalVector = floatArrayOf(planeVectorX[1] * planeVectorY[2] - planeVectorX[2] * planeVectorY[1], planeVectorX[2] * planeVectorY[0] - planeVectorX[0] * planeVectorY[2], planeVectorX[0] * planeVectorY[1] - planeVectorX[1] * planeVectorY[0])

                        // Normalize the normal
                        val length = Matrix.length(normalVector[0], normalVector[1], normalVector[2])

                        heightMapVertexData[offset++] = normalVector[0] / length
                        heightMapVertexData[offset++] = normalVector[1] / length
                        heightMapVertexData[offset++] = normalVector[2] / length

                        // Add some fancy colors.
                        heightMapVertexData[offset++] = xRatio
                        heightMapVertexData[offset++] = yRatio
                        heightMapVertexData[offset++] = 0.5f
                        heightMapVertexData[offset++] = 1f
                    }
                }

                // Now build the index data
                val numStripsRequired = yLength - 1
                val numDegensRequired = 2 * (numStripsRequired - 1)
                val verticesPerStrip = 2 * xLength

                val heightMapIndexData = ShortArray(verticesPerStrip * numStripsRequired + numDegensRequired)

                offset = 0

                for (y in 0 until yLength - 1) {
                    if (y > 0) {
                        // Degenerate begin: repeat first vertex
                        heightMapIndexData[offset++] = (y * yLength).toShort()
                    }

                    for (x in 0 until xLength) {
                        // One part of the strip
                        heightMapIndexData[offset++] = (y * yLength + x).toShort()
                        heightMapIndexData[offset++] = ((y + 1) * yLength + x).toShort()
                    }

                    if (y < yLength - 2) {
                        // Degenerate end: repeat last vertex
                        heightMapIndexData[offset++] = ((y + 1) * yLength + (xLength - 1)).toShort()
                    }
                }

                indexCount = heightMapIndexData.size

                val heightMapVertexDataBuffer = ByteBuffer
                        .allocateDirect(heightMapVertexData.size * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                        .asFloatBuffer()
                heightMapVertexDataBuffer.put(heightMapVertexData).position(0)

                val heightMapIndexDataBuffer = ByteBuffer
                        .allocateDirect(heightMapIndexData.size * BYTES_PER_SHORT).order(ByteOrder.nativeOrder())
                        .asShortBuffer()
                heightMapIndexDataBuffer.put(heightMapIndexData).position(0)

                GLES20.glGenBuffers(1, vbo, 0)
                GLES20.glGenBuffers(1, ibo, 0)

                if (vbo[0] > 0 && ibo[0] > 0) {
                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
                    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, heightMapVertexDataBuffer.capacity() * BYTES_PER_FLOAT,
                            heightMapVertexDataBuffer, GLES20.GL_STATIC_DRAW)

                    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0])
                    GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, heightMapIndexDataBuffer.capacity() * BYTES_PER_SHORT, heightMapIndexDataBuffer, GLES20.GL_STATIC_DRAW)

                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
                    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
                } else {
                    errorHandler.handleError(ErrorType.BUFFER_CREATION_ERROR, "glGenBuffers")
                }
            } catch (t: Throwable) {
                Timber.e(t, "Error while building HeightMap")
                errorHandler.handleError(ErrorType.BUFFER_CREATION_ERROR, t.localizedMessage ?: "Unknown error")
            }

        }

        fun render() {
            if (vbo[0] > 0 && ibo[0] > 0) {
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])

                // Bind Attributes
                GLES20.glVertexAttribPointer(positionAttribute, POSITION_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                        STRIDE, 0)
                GLES20.glEnableVertexAttribArray(positionAttribute)

                GLES20.glVertexAttribPointer(normalAttribute, NORMAL_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                        STRIDE, POSITION_DATA_SIZE_IN_ELEMENTS * BYTES_PER_FLOAT)
                GLES20.glEnableVertexAttribArray(normalAttribute)

                GLES20.glVertexAttribPointer(colorAttribute, COLOR_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                        STRIDE, (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT)
                GLES20.glEnableVertexAttribArray(colorAttribute)

                // Draw
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0])
                GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indexCount, GLES20.GL_UNSIGNED_SHORT, 0)

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            }
        }

        fun release() {
            if (vbo[0] > 0) {
                GLES20.glDeleteBuffers(vbo.size, vbo, 0)
                vbo[0] = 0
            }

            if (ibo[0] > 0) {
                GLES20.glDeleteBuffers(ibo.size, ibo, 0)
                ibo[0] = 0
            }
        }
    }

    companion object {
        private const val SIZE_PER_SIDE = 32
        private const val MIN_POSITION = -5f
        private const val POSITION_RANGE = 10f

        /** Identifiers for our uniforms and attributes inside the shaders.  */
        private const val MVP_MATRIX_UNIFORM = "u_MVPMatrix"
        private const val MV_MATRIX_UNIFORM = "u_MVMatrix"
        private const val LIGHT_POSITION_UNIFORM = "u_LightPos"

        private const val POSITION_ATTRIBUTE = "a_Position"
        private const val NORMAL_ATTRIBUTE = "a_Normal"
        private const val COLOR_ATTRIBUTE = "a_Color"

        /** Additional constants.  */
        private const val POSITION_DATA_SIZE_IN_ELEMENTS = 3
        private const val NORMAL_DATA_SIZE_IN_ELEMENTS = 3
        private const val COLOR_DATA_SIZE_IN_ELEMENTS = 4

        private const val BYTES_PER_FLOAT = 4
        private const val BYTES_PER_SHORT = 2

        private const val STRIDE = (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT
    }
}
