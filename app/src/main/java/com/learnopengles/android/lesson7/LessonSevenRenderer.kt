package com.learnopengles.android.lesson7

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.learnopengles.android.R
import com.learnopengles.android.common.RawResourceReader
import com.learnopengles.android.common.ShaderHelper
import com.learnopengles.android.common.ShapeBuilder
import com.learnopengles.android.common.TextureHelper
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.Executors
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class implements our custom renderer. Note that the GL10 parameter
 * passed in is unused for OpenGL ES 2.0 renderers -- the static class GLES20 is
 * used instead.
 */
class LessonSevenRenderer(
        private val mLessonSevenActivity: LessonSevenActivity,
        private val mGlSurfaceView: GLSurfaceView)
    : GLSurfaceView.Renderer {

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

    /**
     * Store the projection matrix. This is used to project the scene onto a 2D viewport.
     */
    private val mProjectionMatrix = FloatArray(16)

    /**
     * Allocate storage for the final combined matrix. This will be passed into the shader program.
     */
    private val mMVPMatrix = FloatArray(16)

    /**
     * Store the accumulated rotation.
     */
    private val mAccumulatedRotation = FloatArray(16)

    /**
     * Store the current rotation.
     */
    private val mCurrentRotation = FloatArray(16)

    /**
     * A temporary matrix.
     */
    private val mTemporaryMatrix = FloatArray(16)

    /**
     * Stores a copy of the model matrix specifically for the light position.
     */
    private val mLightModelMatrix = FloatArray(16)

    /**
     * This will be used to pass in the transformation matrix.
     */
    private var mMVPMatrixHandle: Int = 0

    /**
     * This will be used to pass in the modelview matrix.
     */
    private var mMVMatrixHandle: Int = 0

    /**
     * This will be used to pass in the light position.
     */
    private var mLightPosHandle: Int = 0

    /**
     * This will be used to pass in the texture.
     */
    private var mTextureUniformHandle: Int = 0

    /**
     * This will be used to pass in model position information.
     */
    private var mPositionHandle: Int = 0

    /**
     * This will be used to pass in model normal information.
     */
    private var mNormalHandle: Int = 0

    /**
     * This will be used to pass in model texture coordinate information.
     */
    private var mTextureCoordinateHandle: Int = 0

    /**
     * Additional info for cube generation.
     */
    private var mLastRequestedCubeFactor: Int = 0
    private var mActualCubeFactor: Int = 0

    /**
     * Control whether vertex buffer objects or client-side memory will be used for rendering.
     */
    private var mUseVBOs = true

    /**
     * Control whether strides will be used.
     */
    private var mUseStride = true

    /**
     * Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
     * we multiply this by our transformation matrices.
     */
    private val mLightPosInModelSpace = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

    /**
     * Used to hold the current position of the light in world space (after transformation via model matrix).
     */
    private val mLightPosInWorldSpace = FloatArray(4)

    /**
     * Used to hold the transformed position of the light in eye space (after transformation via modelview matrix)
     */
    private val mLightPosInEyeSpace = FloatArray(4)

    /**
     * This is a handle to our cube shading program.
     */
    private var mProgramHandle: Int = 0

    /**
     * These are handles to our texture data.
     */
    private var mAndroidDataHandle: Int = 0

    // These still work without volatile, but refreshes are not guaranteed to happen.
    @Volatile
    var mDeltaX: Float = 0.toFloat()
    @Volatile
    var mDeltaY: Float = 0.toFloat()

    /**
     * Thread executor for generating cube data in the background.
     */
    private val mSingleThreadedExecutor = Executors.newSingleThreadExecutor()

    /**
     * The current cubes object.
     */
    private var mCubes: Cubes? = null

    private fun generateCubes(cubeFactor: Int, toggleVbos: Boolean, toggleStride: Boolean) {
        mSingleThreadedExecutor.submit(GenDataRunnable(cubeFactor, toggleVbos, toggleStride))
    }

    internal inner class GenDataRunnable(
            private val mRequestedCubeFactor: Int, 
            private val mToggleVbos: Boolean, 
            private val mToggleStride: Boolean) : Runnable {

        override fun run() {
            try {
                // X, Y, Z
                // The normal is used in light calculations and is a vector which points
                // orthogonal to the plane of the surface. For a cube model, the normals
                // should be orthogonal to the points of each face.
                val cubeNormalData = floatArrayOf(
                        // Front face
                        0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,

                        // Right face
                        1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,

                        // Back face
                        0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,

                        // Left face
                        -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,

                        // Top face
                        0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,

                        // Bottom face
                        0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f)

                // S, T (or X, Y)
                // Texture coordinate data.
                // Because images have a Y axis pointing downward (values increase as you move down the image) while
                // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
                // What's more is that the texture coordinates are the same for every face.
                val cubeTextureCoordinateData = floatArrayOf(
                        // Front face
                        0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,

                        // Right face
                        0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,

                        // Back face
                        0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,

                        // Left face
                        0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,

                        // Top face
                        0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,

                        // Bottom face
                        0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f)

                val cubePositionData = FloatArray(108 * mRequestedCubeFactor * mRequestedCubeFactor * mRequestedCubeFactor)
                var cubePositionDataOffset = 0

                val segments = mRequestedCubeFactor + (mRequestedCubeFactor - 1)
                val minPosition = -1.0f
                val maxPosition = 1.0f
                val positionRange = maxPosition - minPosition

                for (x in 0 until mRequestedCubeFactor) {
                    for (y in 0 until mRequestedCubeFactor) {
                        for (z in 0 until mRequestedCubeFactor) {
                            val x1 = minPosition + positionRange / segments * (x * 2)
                            val x2 = minPosition + positionRange / segments * (x * 2 + 1)

                            val y1 = minPosition + positionRange / segments * (y * 2)
                            val y2 = minPosition + positionRange / segments * (y * 2 + 1)

                            val z1 = minPosition + positionRange / segments * (z * 2)
                            val z2 = minPosition + positionRange / segments * (z * 2 + 1)

                            // Define points for a cube.
                            // X, Y, Z
                            val p1p = floatArrayOf(x1, y2, z2)
                            val p2p = floatArrayOf(x2, y2, z2)
                            val p3p = floatArrayOf(x1, y1, z2)
                            val p4p = floatArrayOf(x2, y1, z2)
                            val p5p = floatArrayOf(x1, y2, z1)
                            val p6p = floatArrayOf(x2, y2, z1)
                            val p7p = floatArrayOf(x1, y1, z1)
                            val p8p = floatArrayOf(x2, y1, z1)

                            val thisCubePositionData = ShapeBuilder.generateCubeData(p1p, p2p, p3p, p4p, p5p, p6p, p7p, p8p,
                                    p1p.size)

                            System.arraycopy(thisCubePositionData, 0, cubePositionData, cubePositionDataOffset, thisCubePositionData.size)
                            cubePositionDataOffset += thisCubePositionData.size
                        }
                    }
                }

                // Run on the GL thread -- the same thread the other members of the renderer run in.
                mGlSurfaceView.queueEvent {
                    if (mCubes != null) {
                        mCubes!!.release()
                        mCubes = null
                    }

                    // Not supposed to manually call this, but Dalvik sometimes needs some additional prodding to clean up the heap.
                    System.gc()

                    try {
                        var useVbos = mUseVBOs
                        var useStride = mUseStride

                        if (mToggleVbos) {
                            useVbos = !useVbos
                        }

                        if (mToggleStride) {
                            useStride = !useStride
                        }

                        mCubes = if (useStride) {
                            if (useVbos) {
                                CubesWithVboWithStride(cubePositionData, cubeNormalData, cubeTextureCoordinateData, mRequestedCubeFactor)
                            } else {
                                CubesClientSideWithStride(cubePositionData, cubeNormalData, cubeTextureCoordinateData, mRequestedCubeFactor)
                            }
                        } else {
                            if (useVbos) {
                                CubesWithVbo(cubePositionData, cubeNormalData, cubeTextureCoordinateData, mRequestedCubeFactor)
                            } else {
                                CubesClientSide(cubePositionData, cubeNormalData, cubeTextureCoordinateData, mRequestedCubeFactor)
                            }
                        }

                        mUseVBOs = useVbos
                        mLessonSevenActivity.updateVboStatus(mUseVBOs)

                        mUseStride = useStride
                        mLessonSevenActivity.updateStrideStatus(mUseStride)

                        mActualCubeFactor = mRequestedCubeFactor
                    } catch (err: OutOfMemoryError) {
                        if (mCubes != null) {
                            mCubes!!.release()
                            mCubes = null
                        }

                        Timber.e("Out of Memory Error")
                    }
                }
            } catch (e: OutOfMemoryError) {
                Timber.e("Out of Memory Error")
            }
        }
    }

    fun decreaseCubeCount() {
        if (mLastRequestedCubeFactor > 1) {
            generateCubes(--mLastRequestedCubeFactor, toggleVbos = false, toggleStride = false)
        }
    }

    fun increaseCubeCount() {
        if (mLastRequestedCubeFactor < 16) {
            generateCubes(++mLastRequestedCubeFactor, toggleVbos = false, toggleStride = false)
        }
    }

    fun toggleVBOs() {
        generateCubes(mLastRequestedCubeFactor, toggleVbos = true, toggleStride = false)
    }

    fun toggleStride() {
        generateCubes(mLastRequestedCubeFactor, toggleVbos = false, toggleStride = true)
    }

    override fun onSurfaceCreated(glUnused: GL10, config: EGLConfig) {
        mActualCubeFactor = 3
        mLastRequestedCubeFactor = mActualCubeFactor
        generateCubes(mActualCubeFactor, toggleVbos = false, toggleStride = false)

        // Set the background clear color to black.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE)

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

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        val upX = 0.0f
        val upY = 1.0f
        val upZ = 0.0f

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ)

        val vertexShader = RawResourceReader.readTextFileFromRawResource(mLessonSevenActivity, R.raw.lesson_seven_vertex_shader)
        val fragmentShader = RawResourceReader.readTextFileFromRawResource(mLessonSevenActivity, R.raw.lesson_seven_fragment_shader)

        val vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader!!)
        val fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader!!)

        mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                arrayOf("a_Position", "a_Normal", "a_TexCoordinate"))

        // Load the texture
        mAndroidDataHandle = TextureHelper.loadTexture(mLessonSevenActivity, R.drawable.usb_android)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mAndroidDataHandle)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mAndroidDataHandle)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)

        // Initialize the accumulated rotation matrix
        Matrix.setIdentityM(mAccumulatedRotation, 0)
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
        val far = 1000.0f

        Matrix.frustumM(mProjectionMatrix, 0, left, ratio, bottom, top, near, far)
    }

    override fun onDrawFrame(glUnused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mProgramHandle)

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix")
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix")
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos")
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture")
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position")
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal")
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate")

        // Calculate position of the light. Push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0)
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -1.0f)

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0)
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0)

        // Draw a cube.
        // Translate the cube into the screen.
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -3.5f)

        // Set a matrix that contains the current rotation.
        Matrix.setIdentityM(mCurrentRotation, 0)
        Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0f, 1.0f, 0.0f)
        Matrix.rotateM(mCurrentRotation, 0, mDeltaY, 1.0f, 0.0f, 0.0f)
        mDeltaX = 0.0f
        mDeltaY = 0.0f

        // Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
        Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, mAccumulatedRotation, 0)
        System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16)

        // Rotate the cube taking the overall rotation into account.
        Matrix.multiplyMM(mTemporaryMatrix, 0, mModelMatrix, 0, mAccumulatedRotation, 0)
        System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16)

        // This multiplies the view matrix by the model matrix, and stores
        // the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0)

        // This multiplies the modelview matrix by the projection matrix,
        // and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16)

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)

        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2])

        // Pass in the texture information
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mAndroidDataHandle)

        // Tell the texture uniform sampler to use this texture in the
        // shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0)

        if (mCubes != null) {
            mCubes!!.render()
        }
    }

    internal abstract class Cubes {
        internal abstract fun render()

        internal abstract fun release()

        fun getBuffers(cubePositions: FloatArray, cubeNormals: FloatArray, cubeTextureCoordinates: FloatArray, generatedCubeFactor: Int): Array<FloatBuffer> {
            // First, copy cube information into client-side floating point buffers.
            val cubePositionsBuffer: FloatBuffer = ByteBuffer.allocateDirect(cubePositions.size * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
            val cubeNormalsBuffer: FloatBuffer = ByteBuffer.allocateDirect(cubeNormals.size * BYTES_PER_FLOAT * generatedCubeFactor * generatedCubeFactor * generatedCubeFactor)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
            val cubeTextureCoordinatesBuffer: FloatBuffer = ByteBuffer.allocateDirect(cubeTextureCoordinates.size * BYTES_PER_FLOAT * generatedCubeFactor * generatedCubeFactor * generatedCubeFactor)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()

            cubePositionsBuffer.put(cubePositions).position(0)

            for (i in 0 until generatedCubeFactor * generatedCubeFactor * generatedCubeFactor) {
                cubeNormalsBuffer.put(cubeNormals)
            }

            cubeNormalsBuffer.position(0)

            for (i in 0 until generatedCubeFactor * generatedCubeFactor * generatedCubeFactor) {
                cubeTextureCoordinatesBuffer.put(cubeTextureCoordinates)
            }

            cubeTextureCoordinatesBuffer.position(0)

            return arrayOf(cubePositionsBuffer, cubeNormalsBuffer, cubeTextureCoordinatesBuffer)
        }

        fun getInterleavedBuffer(cubePositions: FloatArray, cubeNormals: FloatArray, cubeTextureCoordinates: FloatArray, generatedCubeFactor: Int): FloatBuffer {
            val cubeDataLength = (cubePositions.size
                    + cubeNormals.size * generatedCubeFactor * generatedCubeFactor * generatedCubeFactor
                    + cubeTextureCoordinates.size * generatedCubeFactor * generatedCubeFactor * generatedCubeFactor)
            var cubePositionOffset = 0
            var cubeNormalOffset = 0
            var cubeTextureOffset = 0

            val cubeBuffer = ByteBuffer.allocateDirect(cubeDataLength * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()

            for (i in 0 until generatedCubeFactor * generatedCubeFactor * generatedCubeFactor) {
                for (v in 0..35) {
                    cubeBuffer.put(cubePositions, cubePositionOffset, POSITION_DATA_SIZE)
                    cubePositionOffset += POSITION_DATA_SIZE
                    cubeBuffer.put(cubeNormals, cubeNormalOffset, NORMAL_DATA_SIZE)
                    cubeNormalOffset += NORMAL_DATA_SIZE
                    cubeBuffer.put(cubeTextureCoordinates, cubeTextureOffset, TEXTURE_COORDINATE_DATA_SIZE)
                    cubeTextureOffset += TEXTURE_COORDINATE_DATA_SIZE
                }

                // The normal and texture data is repeated for each cube.
                cubeNormalOffset = 0
                cubeTextureOffset = 0
            }

            cubeBuffer.position(0)

            return cubeBuffer
        }
    }

    internal inner class CubesClientSide(cubePositions: FloatArray, cubeNormals: FloatArray, cubeTextureCoordinates: FloatArray, generatedCubeFactor: Int) : Cubes() {
        private var mCubePositions: FloatBuffer? = null
        private var mCubeNormals: FloatBuffer? = null
        private var mCubeTextureCoordinates: FloatBuffer? = null

        init {
            val buffers = getBuffers(cubePositions, cubeNormals, cubeTextureCoordinates, generatedCubeFactor)

            mCubePositions = buffers[0]
            mCubeNormals = buffers[1]
            mCubeTextureCoordinates = buffers[2]
        }

        override fun render() {
            // Pass in the position information
            GLES20.glEnableVertexAttribArray(mPositionHandle)
            GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubePositions)

            // Pass in the normal information
            GLES20.glEnableVertexAttribArray(mNormalHandle)
            GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeNormals)

            // Pass in the texture information
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false,
                    0, mCubeTextureCoordinates)

            // Draw the cubes.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mActualCubeFactor * mActualCubeFactor * mActualCubeFactor * 36)
        }

        override fun release() {
            mCubePositions!!.limit(0)
            mCubePositions = null
            mCubeNormals!!.limit(0)
            mCubeNormals = null
            mCubeTextureCoordinates!!.limit(0)
            mCubeTextureCoordinates = null
        }
    }

    internal inner class CubesClientSideWithStride(cubePositions: FloatArray, cubeNormals: FloatArray, cubeTextureCoordinates: FloatArray, generatedCubeFactor: Int) : Cubes() {
        private var mCubeBuffer: FloatBuffer? = null

        init {
            mCubeBuffer = getInterleavedBuffer(cubePositions, cubeNormals, cubeTextureCoordinates, generatedCubeFactor)
        }

        override fun render() {
            val stride = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE + TEXTURE_COORDINATE_DATA_SIZE) * BYTES_PER_FLOAT

            // Pass in the position information
            mCubeBuffer!!.position(0)
            GLES20.glEnableVertexAttribArray(mPositionHandle)
            GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, stride, mCubeBuffer)

            // Pass in the normal information
            mCubeBuffer!!.position(POSITION_DATA_SIZE)
            GLES20.glEnableVertexAttribArray(mNormalHandle)
            GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, stride, mCubeBuffer)

            // Pass in the texture information
            mCubeBuffer!!.position(POSITION_DATA_SIZE + NORMAL_DATA_SIZE)
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false,
                    stride, mCubeBuffer)

            // Draw the cubes.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mActualCubeFactor * mActualCubeFactor * mActualCubeFactor * 36)
        }

        override fun release() {
            mCubeBuffer!!.limit(0)
            mCubeBuffer = null
        }
    }

    internal inner class CubesWithVbo(cubePositions: FloatArray, cubeNormals: FloatArray, cubeTextureCoordinates: FloatArray, generatedCubeFactor: Int) : Cubes() {
        private val mCubePositionsBufferIdx: Int
        private val mCubeNormalsBufferIdx: Int
        private val mCubeTexCoordsBufferIdx: Int

        init {
            val floatBuffers = getBuffers(cubePositions, cubeNormals, cubeTextureCoordinates, generatedCubeFactor)

            val cubePositionsBuffer: FloatBuffer = floatBuffers[0]
            val cubeNormalsBuffer: FloatBuffer = floatBuffers[1]
            val cubeTextureCoordinatesBuffer: FloatBuffer = floatBuffers[2]

            // Second, copy these buffers into OpenGL's memory. After, we don't need to keep the client-side buffers around.
            val buffers = IntArray(3)
            GLES20.glGenBuffers(3, buffers, 0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, cubePositionsBuffer!!.capacity() * BYTES_PER_FLOAT, cubePositionsBuffer, GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, cubeNormalsBuffer!!.capacity() * BYTES_PER_FLOAT, cubeNormalsBuffer, GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, cubeTextureCoordinatesBuffer!!.capacity() * BYTES_PER_FLOAT, cubeTextureCoordinatesBuffer,
                    GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            mCubePositionsBufferIdx = buffers[0]
            mCubeNormalsBufferIdx = buffers[1]
            mCubeTexCoordsBufferIdx = buffers[2]

            cubePositionsBuffer.limit(0)
            cubeNormalsBuffer.limit(0)
            cubeTextureCoordinatesBuffer.limit(0)
        }

        override fun render() {
            // Pass in the position information
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubePositionsBufferIdx)
            GLES20.glEnableVertexAttribArray(mPositionHandle)
            GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0)

            // Pass in the normal information
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeNormalsBufferIdx)
            GLES20.glEnableVertexAttribArray(mNormalHandle)
            GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0)

            // Pass in the texture information
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeTexCoordsBufferIdx)
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false,
                    0, 0)

            // Clear the currently bound buffer (so future OpenGL calls do not use this buffer).
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            // Draw the cubes.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mActualCubeFactor * mActualCubeFactor * mActualCubeFactor * 36)
        }

        override fun release() {
            // Delete buffers from OpenGL's memory
            val buffersToDelete = intArrayOf(mCubePositionsBufferIdx, mCubeNormalsBufferIdx, mCubeTexCoordsBufferIdx)
            GLES20.glDeleteBuffers(buffersToDelete.size, buffersToDelete, 0)
        }
    }

    internal inner class CubesWithVboWithStride(cubePositions: FloatArray, cubeNormals: FloatArray, cubeTextureCoordinates: FloatArray, generatedCubeFactor: Int) : Cubes() {
        private val mCubeBufferIdx: Int

        init {
            val cubeBuffer: FloatBuffer = getInterleavedBuffer(cubePositions, cubeNormals, cubeTextureCoordinates, generatedCubeFactor)

            // Second, copy these buffers into OpenGL's memory. After, we don't need to keep the client-side buffers around.
            val buffers = IntArray(1)
            GLES20.glGenBuffers(1, buffers, 0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, cubeBuffer!!.capacity() * BYTES_PER_FLOAT, cubeBuffer, GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            mCubeBufferIdx = buffers[0]

            cubeBuffer.limit(0)
        }

        override fun render() {
            val stride = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE + TEXTURE_COORDINATE_DATA_SIZE) * BYTES_PER_FLOAT

            // Pass in the position information
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeBufferIdx)
            GLES20.glEnableVertexAttribArray(mPositionHandle)
            GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, stride, 0)

            // Pass in the normal information
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeBufferIdx)
            GLES20.glEnableVertexAttribArray(mNormalHandle)
            GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, stride, POSITION_DATA_SIZE * BYTES_PER_FLOAT)

            // Pass in the texture information
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeBufferIdx)
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false,
                    stride, (POSITION_DATA_SIZE + NORMAL_DATA_SIZE) * BYTES_PER_FLOAT)

            // Clear the currently bound buffer (so future OpenGL calls do not use this buffer).
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            // Draw the cubes.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mActualCubeFactor * mActualCubeFactor * mActualCubeFactor * 36)
        }

        override fun release() {
            // Delete buffers from OpenGL's memory
            val buffersToDelete = intArrayOf(mCubeBufferIdx)
            GLES20.glDeleteBuffers(buffersToDelete.size, buffersToDelete, 0)
        }
    }

    companion object {
        /**
         * Size of the position data in elements.
         */
        private const val POSITION_DATA_SIZE = 3

        /**
         * Size of the normal data in elements.
         */
        private const val NORMAL_DATA_SIZE = 3

        /**
         * Size of the texture coordinate data in elements.
         */
        private const val TEXTURE_COORDINATE_DATA_SIZE = 2

        /**
         * How many bytes per float.
         */
        private const val BYTES_PER_FLOAT = 4
    }
}
