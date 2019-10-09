package com.learnopengles.android.lesson1

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock

/**
 * This class implements our custom renderer.
 * Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
class LessonOneRenderer : GLSurfaceView.Renderer {
    /**
     * Store the model matrix. This matrix is used to move models from object space
     * (where each model can be thought of being located at the center of the universe)
     * to world space.
     */
    private val mModelMatrix = FloatArray(16)

    /**
     * Store the view matrix. This can be thought of as our camera.
     * This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private val mViewMatrix = FloatArray(16)

    /** Store the projection matrix. This is used to project the scene onto a 2D viewport.  */
    private val mProjectionMatrix = FloatArray(16)

    /** Allocate storage for the final combined matrix. This will be passed into the shader program.  */
    private val mMVPMatrix = FloatArray(16)

    /** Store our model data in a float buffer.  */
    private val mTriangle1Vertices: FloatBuffer
    private val mTriangle2Vertices: FloatBuffer
    private val mTriangle3Vertices: FloatBuffer

    /** This will be used to pass in the transformation matrix.  */
    private var mMVPMatrixHandle: Int = 0

    /** This will be used to pass in model position information.  */
    private var mPositionHandle: Int = 0

    /** This will be used to pass in model color information.  */
    private var mColorHandle: Int = 0

    /** How many bytes per float.  */
    private val mBytesPerFloat = 4

    /** How many elements per vertex.  */
    private val mStrideBytes = 7 * mBytesPerFloat

    /** Offset of the position data.  */
    private val mPositionOffset = 0

    /** Size of the position data in elements.  */
    private val mPositionDataSize = 3

    /** Offset of the color data.  */
    private val mColorOffset = 3

    /** Size of the color data in elements.  */
    private val mColorDataSize = 4

    /**
     * Initialize the model data.
     */
    init {
        // Define points for equilateral triangles.

        // This triangle is red, green, and blue.
        val triangle1VerticesData = floatArrayOf(
                // X, Y, Z,
                // R, G, B, A
                -0.5f, -0.25f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,

                0.5f, -0.25f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f,

                0.0f, 0.559016994f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f)

        // This triangle is yellow, cyan, and magenta.
        val triangle2VerticesData = floatArrayOf(
                // X, Y, Z,
                // R, G, B, A
                -0.5f, -0.25f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,

                0.5f, -0.25f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,

                0.0f, 0.559016994f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f)

        // This triangle is white, gray, and black.
        val triangle3VerticesData = floatArrayOf(
                // X, Y, Z,
                // R, G, B, A
                -0.5f, -0.25f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,

                0.5f, -0.25f, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f,

                0.0f, 0.559016994f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)

        // Initialize the buffers.
        mTriangle1Vertices = ByteBuffer.allocateDirect(triangle1VerticesData.size * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mTriangle2Vertices = ByteBuffer.allocateDirect(triangle2VerticesData.size * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mTriangle3Vertices = ByteBuffer.allocateDirect(triangle3VerticesData.size * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()

        mTriangle1Vertices.put(triangle1VerticesData).position(0)
        mTriangle2Vertices.put(triangle2VerticesData).position(0)
        mTriangle3Vertices.put(triangle3VerticesData).position(0)
    }

    override fun onSurfaceCreated(glUnused: GL10, config: EGLConfig) {
        // Set the background clear color to gray.
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f)

        // Position the eye behind the origin.
        val eyeX = 0.0f
        val eyeY = 0.0f
        val eyeZ = 1.5f

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

        val vertexShader =
                """
                    
                uniform mat4 u_MVPMatrix;	// A constant representing the combined model/view/projection matrix.
                attribute vec4 a_Position;	// Per-vertex position information we will pass in.
                attribute vec4 a_Color;	// Per-vertex color information we will pass in.
                varying vec4 v_Color;	// This will be passed into the fragment shader.
                
                void main()             // The entry point for our vertex shader.
                {
                     v_Color = a_Color;	// Pass the color through to the fragment shader
                                        // It will be interpolated across the triangle.
                    gl_Position = u_MVPMatrix       // gl_Position is a special variable used to store the final position.
                       * a_Position;	// Multiply the vertex by the matrix to get the final point in
                }                       // normalized screen coordinates.
        """.trimIndent()

        val fragmentShader =
                """
                precision mediump float;	// Set the default precision to medium. We don't need as high of a
                                            // precision in the fragment shader.
                varying vec4 v_Color;	    // This is the color from the vertex shader interpolated across the
                                            // triangle per fragment.
                void main()                 // The entry point for our fragment shader.
                {
                    gl_FragColor = v_Color;	// Pass the color directly through the pipeline.
        
                }
                """.trimIndent()

        // Load in the vertex shader.
        var vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)

        if (vertexShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(vertexShaderHandle, vertexShader)

            // Compile the shader.
            GLES20.glCompileShader(vertexShaderHandle)

            // Get the compilation status.
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(vertexShaderHandle)
                vertexShaderHandle = 0
            }
        }

        if (vertexShaderHandle == 0) {
            throw RuntimeException("Error creating vertex shader.")
        }

        // Load in the fragment shader shader.
        var fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)

        if (fragmentShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader)

            // Compile the shader.
            GLES20.glCompileShader(fragmentShaderHandle)

            // Get the compilation status.
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(fragmentShaderHandle)
                fragmentShaderHandle = 0
            }
        }

        if (fragmentShaderHandle == 0) {
            throw RuntimeException("Error creating fragment shader.")
        }

        // Create a program object and store the handle to it.
        var programHandle = GLES20.glCreateProgram()

        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle)

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle)

            // Bind attributes
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position")
            GLES20.glBindAttribLocation(programHandle, 1, "a_Color")

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle)

            // Get the link status.
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0)

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle)
                programHandle = 0
            }
        }

        if (programHandle == 0) {
            throw RuntimeException("Error creating program.")
        }

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix")
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position")
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color")

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(programHandle)
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
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        // Do a complete rotation every 10 seconds.
        val time = SystemClock.uptimeMillis() % 10000L
        val angleInDegrees = 360.0f / 10000.0f * time.toInt()

        // Draw the triangle facing straight on.
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f)
        drawTriangle(mTriangle1Vertices)

        // Draw one translated a bit down and rotated to be flat on the ground.
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, 0.0f, -1.0f, 0.0f)
        Matrix.rotateM(mModelMatrix, 0, 90.0f, 1.0f, 0.0f, 0.0f)
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f)
        drawTriangle(mTriangle2Vertices)

        // Draw one translated a bit to the right and rotated to be facing to the left.
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, 1.0f, 0.0f, 0.0f)
        Matrix.rotateM(mModelMatrix, 0, 90.0f, 0.0f, 1.0f, 0.0f)
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f)
        drawTriangle(mTriangle3Vertices)
    }

    /**
     * Draws a triangle from the given vertex data.
     *
     * @param aTriangleBuffer The buffer containing the vertex data.
     */
    private fun drawTriangle(aTriangleBuffer: FloatBuffer) {
        // Pass in the position information
        aTriangleBuffer.position(mPositionOffset)
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer)

        GLES20.glEnableVertexAttribArray(mPositionHandle)

        // Pass in the color information
        aTriangleBuffer.position(mColorOffset)
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer)

        GLES20.glEnableVertexAttribArray(mColorHandle)

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
    }
}
