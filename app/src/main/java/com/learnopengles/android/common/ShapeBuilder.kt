package com.learnopengles.android.common

object ShapeBuilder {
    private const val FRONT = 0
    private const val RIGHT = 1
    private const val BACK = 2
    private const val LEFT = 3
    private const val TOP = 4
    //        private const val BOTTOM = 5
    
    fun generateCubeData(point1: FloatArray,
                         point2: FloatArray,
                         point3: FloatArray,
                         point4: FloatArray,
                         point5: FloatArray,
                         point6: FloatArray,
                         point7: FloatArray,
                         point8: FloatArray,
                         elementsPerPoint: Int): FloatArray {
        // Given a cube with the points defined as follows:
        // front left top, front right top, front left bottom, front right bottom,
        // back left top, back right top, back left bottom, back right bottom,
        // return an array of 6 sides, 2 triangles per side, 3 vertices per triangle, and 4 floats per vertex.
        

        val size = elementsPerPoint * 6 * 6
        val cubeData = FloatArray(size)

        for (face in 0..5) {
            // Relative to the side, p1 = top left, p2 = top right, p3 = bottom left, p4 = bottom right
            val p1: FloatArray
            val p2: FloatArray
            val p3: FloatArray
            val p4: FloatArray

            // Select the points for this face
            when (face) {
                FRONT -> {
                    p1 = point1
                    p2 = point2
                    p3 = point3
                    p4 = point4
                }
                RIGHT -> {
                    p1 = point2
                    p2 = point6
                    p3 = point4
                    p4 = point8
                }
                BACK -> {
                    p1 = point6
                    p2 = point5
                    p3 = point8
                    p4 = point7
                }
                LEFT -> {
                    p1 = point5
                    p2 = point1
                    p3 = point7
                    p4 = point3
                }
                TOP -> {
                    p1 = point5
                    p2 = point6
                    p3 = point1
                    p4 = point2
                }
                else
                    // if (side == BOTTOM)
                -> {
                    p1 = point8
                    p2 = point7
                    p3 = point4
                    p4 = point3
                }
            }

            // In OpenGL counter-clockwise winding is default. This means that when we look at a triangle,
            // if the points are counter-clockwise we are looking at the "front". If not we are looking at
            // the back. OpenGL has an optimization where all back-facing triangles are culled, since they
            // usually represent the backside of an object and aren't visible anyways.

            // Build the triangles
            //  1---3,6
            //  | / |
            // 2,4--5
            var offset = face * elementsPerPoint * 6

            for (i in 0 until elementsPerPoint) {
                cubeData[offset++] = p1[i]
            }
            for (i in 0 until elementsPerPoint) {
                cubeData[offset++] = p3[i]
            }
            for (i in 0 until elementsPerPoint) {
                cubeData[offset++] = p2[i]
            }
            for (i in 0 until elementsPerPoint) {
                cubeData[offset++] = p3[i]
            }
            for (i in 0 until elementsPerPoint) {
                cubeData[offset++] = p4[i]
            }
            for (i in 0 until elementsPerPoint) {
                cubeData[offset++] = p2[i]
            }
        }

        return cubeData
    }
}
