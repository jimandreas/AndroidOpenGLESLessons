package com.learnopengles.android.lesson3

import com.learnopengles.android.lesson2.LessonTwoRenderer

/**
 * This class implements our custom renderer as an extension of LessonTwo.
 */
class LessonThreeRenderer : LessonTwoRenderer() {

    override val vertexShader : String = """
        uniform mat4 u_MVPMatrix;      		// A constant representing the combined model/view/projection matrix.
        uniform mat4 u_MVMatrix;       		// A constant representing the combined model/view matrix.
                
        attribute vec4 a_Position;     		// Per-vertex position information we will pass in.
        attribute vec4 a_Color;        		// Per-vertex color information we will pass in.
        attribute vec3 a_Normal;       		// Per-vertex normal information we will pass in.
        
        varying vec3 v_Position;       		// This will be passed into the fragment shader.
        varying vec4 v_Color;          		// This will be passed into the fragment shader.
        varying vec3 v_Normal;         		// This will be passed into the fragment shader.

        void main()                                                 	
        {
           v_Position = vec3(u_MVMatrix * a_Position);   // Transform the vertex into eye space.
           v_Color = a_Color;                             // Pass through the color.
           v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));       // Transform the normal's orientation into eye space.
        
           // gl_Position is a special variable used to store the final position.
           // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
        
           gl_Position = u_MVPMatrix * a_Position;                       		  
        }   
        """.trimIndent()

    override val fragmentShader = """
        precision mediump float;       		// Set the default precision to medium. We don't need as high of a 
													// precision in the fragment shader.
        uniform vec3 u_LightPos;       	    // The position of the light in eye space.
        
        varying vec3 v_Position;				// Interpolated position for this fragment.
        varying vec4 v_Color;          		// This is the color from the vertex shader interpolated across the 
                                                // triangle per fragment.
        varying vec3 v_Normal;         		// Interpolated normal for this fragment.

        void main()                    		
        {                              
        
         float distance = length(u_LightPos - v_Position);   // Will be used for attenuation.               
        
         vec3 lightVector = normalize(u_LightPos - v_Position);   // Get a lighting direction vector from the light to the vertex.
                    	
         // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
         // pointing in the same direction then it will get max illumination.
        
         float diffuse = max(dot(v_Normal, lightVector), 0.1);
         diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));  // Add attenuation. 
         
         // Multiply the color by the diffuse illumination level to get final output color.
         
         gl_FragColor = v_Color * diffuse;                                  		
        }
        """.trimIndent()

}
