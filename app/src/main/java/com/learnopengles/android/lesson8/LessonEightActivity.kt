package com.learnopengles.android.lesson8

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Bundle

class LessonEightActivity : Activity() {
    private lateinit var glSurfaceView: LessonEightGLSurfaceView
    private var renderer: LessonEightRenderer? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glSurfaceView = LessonEightGLSurfaceView(this)

        setContentView(glSurfaceView)

        // Check if the system supports OpenGL ES 2.0.
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            glSurfaceView.setEGLContextClientVersion(2)


            // Set the renderer to our demo renderer, defined below.
            renderer = LessonEightRenderer(this, glSurfaceView)
            glSurfaceView.setRenderer(renderer!!, resources.displayMetrics.density)
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return
        }
    }

    override fun onResume() {
        // The activity must call the GL surface view's onResume() on activity
        // onResume().
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        // The activity must call the GL surface view's onPause() on activity
        // onPause().
        super.onPause()
        glSurfaceView.onPause()
    }
}