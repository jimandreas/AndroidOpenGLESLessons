package com.learnopengles.android.lesson2

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
open class LessonTwoRenderer : GLSurfaceView.Renderer {

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

    /**
     * Stores a copy of the model matrix specifically for the light position.
     */
    private val mLightModelMatrix = FloatArray(16)

    /** Store our model data in a float buffer.  */
    private val mCubePositions: FloatBuffer
    private val mCubeColors: FloatBuffer
    private val mCubeNormals: FloatBuffer

    /** This will be used to pass in the transformation matrix.  */
    private var mMVPMatrixHandle: Int = 0

    /** This will be used to pass in the modelview matrix.  */
    private var mMVMatrixHandle: Int = 0

    /** This will be used to pass in the light position.  */
    private var mLightPosHandle: Int = 0

    /** This will be used to pass in model position information.  */
    private var mPositionHandle: Int = 0

    /** This will be used to pass in model color information.  */
    private var mColorHandle: Int = 0

    /** This will be used to pass in model normal information.  */
    private var mNormalHandle: Int = 0

    /** How many bytes per float.  */
    private val mBytesPerFloat = 4

    /** Size of the position data in elements.  */
    private val mPositionDataSize = 3

    /** Size of the color data in elements.  */
    private val mColorDataSize = 4

    /** Size of the normal data in elements.  */
    private val mNormalDataSize = 3

    /** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
     * we multiply this by our transformation matrices.  */
    private val mLightPosInModelSpace = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

    /** Used to hold the current position of the light in world space (after transformation via model matrix).  */
    private val mLightPosInWorldSpace = FloatArray(4)

    /** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix)  */
    private val mLightPosInEyeSpace = FloatArray(4)

    /** This is a handle to our per-vertex cube shading program.  */
    private var mPerVertexProgramHandle: Int = 0

    /** This is a handle to our light point program.  */
    private var mPointProgramHandle: Int = 0

    // TODO: Explain why we normalize the vectors, explain some of the vector math behind it all. Explain what is eye space.
    
    protected open val vertexShader: String = """
    uniform mat4 u_MVPMatrix;      		// A constant representing the combined model/view/projection matrix.
    uniform mat4 u_MVMatrix;       		// A constant representing the combined model/view matrix.	
    uniform vec3 u_LightPos;       	    // The position of the light in eye space.
    
    attribute vec4 a_Position;     		// Per-vertex position information we will pass in.
    attribute vec4 a_Color;        		// Per-vertex color information we will pass in.
    attribute vec3 a_Normal;       		// Per-vertex normal information we will pass in.
    
    varying vec4 v_Color;          		// This will be passed into the fragment shader.
    
    void main()                     	// The entry point for our vertex shader.
    {                              		
    
        vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);          // Transform the vertex into eye space.           
        
        vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0)); // Transform the normal's orientation into eye space.
        
        float distance = length(u_LightPos - modelViewVertex);         // Will be used for attenuation.
        
        vec3 lightVector = normalize(u_LightPos - modelViewVertex);    // Get a lighting direction vector from the light to the vertex. 
          
        // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
        // pointing in the same direction then it will get max illumination.
        
        float diffuse = max(dot(modelViewNormal, lightVector), 0.1);        	  		  													  
        
        diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));  // Attenuate the light based on distance.
        
        v_Color = a_Color * diffuse;        // Multiply the color by the illumination level. It will be interpolated across the triangle.
                                             
        // gl_Position is a special variable used to store the final position.
        // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.	
            
        gl_Position = u_MVPMatrix * a_Position;                                 
      }                                                                 
        """.trimIndent()

    protected open val fragmentShader: String = """
    precision mediump float;       		// Set the default precision to medium. We don't need as high of a 
                                        // precision in the fragment shader.				
    varying vec4 v_Color;          		// This is the color from the vertex shader interpolated across the 
                                        // triangle per fragment.			  
    void main()                    		// The entry point for our fragment shader.
    {                              
      gl_FragColor = v_Color;     		// Pass the color directly through the pipeline.		  
    }
        """.trimIndent()

    /**
     * Initialize the model data.
     */
    init {
        // Define points for a cube.

        // X, Y, Z
        val cubePositionData = floatArrayOf(
                // In OpenGL counter-clockwise winding is default. This means that when we look at a triangle,
                // if the points are counter-clockwise we are looking at the "front". If not we are looking at
                // the back. OpenGL has an optimization where all back-facing triangles are culled, since they
                // usually represent the backside of an object and aren't visible anyways.

                // Front face
                -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,

                // Right face
                1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f,

                // Back face
                1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f,

                // Left face
                -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f,

                // Top face
                -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,

                // Bottom face
                1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f)

        // R, G, B, A
        val cubeColorData = floatArrayOf(
                // Front face (red)
                1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f,

                // Right face (green)
                0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f,

                // Back face (blue)
                0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,

                // Left face (yellow)
                1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,

                // Top face (cyan)
                0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f,

                // Bottom face (magenta)
                1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f)

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

        // Initialize the buffers.
        mCubePositions = ByteBuffer.allocateDirect(cubePositionData.size * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubePositions.put(cubePositionData).position(0)

        mCubeColors = ByteBuffer.allocateDirect(cubeColorData.size * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubeColors.put(cubeColorData).position(0)

        mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.size * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubeNormals.put(cubeNormalData).position(0)
    }

    override fun onSurfaceCreated(glUnused: GL10, config: EGLConfig) {
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

        val vertexShader = vertexShader
        val fragmentShader = fragmentShader

        val vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

        mPerVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                arrayOf("a_Position", "a_Color", "a_Normal"))

        // Define a simple shader program for our point.

        val pointVertexShader = """
		        uniform mat4 u_MVPMatrix;      
                attribute vec4 a_Position;     
                void main()                    
                {                              
                   gl_Position = u_MVPMatrix   
                               * a_Position;   
                   gl_PointSize = 5.0;         
                }                             
		""".trimIndent()

        val pointFragmentShader = """ 
		        precision mediump float;       
                void main()                    
                {                              
                   gl_FragColor = vec4(1.0,    
                   1.0, 1.0, 1.0);             
                }                              
		""".trimIndent()

        val pointVertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader)
        val pointFragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader)
        mPointProgramHandle = createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                arrayOf("a_Position"))
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
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Do a complete rotation every 10 seconds.
        val time = SystemClock.uptimeMillis() % 10000L
        val angleInDegrees = 360.0f / 10000.0f * time.toInt()

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mPerVertexProgramHandle)

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix")
        mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix")
        mLightPosHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LightPos")
        mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position")
        mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Color")
        mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Normal")

        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0)
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f)
        Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f)
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f)

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0)
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0)

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

        // Draw a point to indicate the light.
        GLES20.glUseProgram(mPointProgramHandle)
        drawLight()
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

        // Pass in the normal information
        mCubeNormals.position(0)
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
                0, mCubeNormals)

        GLES20.glEnableVertexAttribArray(mNormalHandle)

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0)

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)

        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2])

        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)
    }

    /**
     * Draws a point representing the position of the light.
     */
    private fun drawLight() {
        val pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix")
        val pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position")

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2])

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle)

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0)

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)
    }

    /**
     * Helper function to compile a shader.
     *
     * @param shaderType The shader type.
     * @param shaderSource The shader source code.
     * @return An OpenGL handle to the shader.
     */
    private fun compileShader(shaderType: Int, shaderSource: String): Int {
        var shaderHandle = GLES20.glCreateShader(shaderType)

        if (shaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderSource)

            // Compile the shader.
            GLES20.glCompileShader(shaderHandle)

            // Get the compilation status.
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Timber.e("Error compiling shader: %s", GLES20.glGetShaderInfoLog(shaderHandle))
                GLES20.glDeleteShader(shaderHandle)
                shaderHandle = 0
            }
        }

        if (shaderHandle == 0) {
            throw RuntimeException("Error creating shader.")
        }

        return shaderHandle
    }

    /**
     * Helper function to compile and link a program.
     *
     * @param vertexShaderHandle An OpenGL handle to an already-compiled vertex shader.
     * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
     * @param attributes Attributes that need to be bound to the program.
     * @return An OpenGL handle to the program.
     */
    private fun createAndLinkProgram(vertexShaderHandle: Int, fragmentShaderHandle: Int, attributes: Array<String>?): Int {
        var programHandle = GLES20.glCreateProgram()

        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle)

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle)

            // Bind attributes
            if (attributes != null) {
                val size = attributes.size
                for (i in 0 until size) {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i])
                }
            }

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle)

            // Get the link status.
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0)

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                Timber.e("Error compiling program: %s", GLES20.glGetProgramInfoLog(programHandle))
                GLES20.glDeleteProgram(programHandle)
                programHandle = 0
            }
        }

        if (programHandle == 0) {
            throw RuntimeException("Error creating program.")
        }

        return programHandle
    }
}
