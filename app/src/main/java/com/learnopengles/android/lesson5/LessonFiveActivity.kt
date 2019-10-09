package com.learnopengles.android.lesson5

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.learnopengles.android.R

class LessonFiveActivity : Activity() {
    /** Hold a reference to our GLSurfaceView  */
    private lateinit var mGLSurfaceView: LessonFiveGLSurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mGLSurfaceView = LessonFiveGLSurfaceView(this)

        // Check if the system supports OpenGL ES 2.0.
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2)

            // Set the renderer to our demo renderer, defined below.
            mGLSurfaceView.setRenderer(LessonFiveRenderer(this))
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return
        }

        setContentView(mGLSurfaceView)

        // Show a short help message to the user.
        if (savedInstanceState == null || !savedInstanceState.getBoolean(SHOWED_TOAST, false)) {
            Toast.makeText(this, R.string.lesson_five_startup_toast, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume()
        mGLSurfaceView.onResume()
    }

    override fun onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause()
        mGLSurfaceView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(SHOWED_TOAST, true)
    }

    companion object {

        private const val SHOWED_TOAST = "showed_toast"
    }
}