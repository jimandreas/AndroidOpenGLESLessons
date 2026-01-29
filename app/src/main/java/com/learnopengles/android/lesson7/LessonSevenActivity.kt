package com.learnopengles.android.lesson7

import android.app.Activity
import android.app.ActivityManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.learnopengles.android.R

class LessonSevenActivity : Activity() {
    /** Hold a reference to our GLSurfaceView  */
    private lateinit var mGLSurfaceView: LessonSevenGLSurfaceView
    private var mRenderer: LessonSevenRenderer? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.lesson_seven)

        mGLSurfaceView = findViewById<View>(R.id.gl_surface_view) as LessonSevenGLSurfaceView

        // Check if the system supports OpenGL ES 2.0.
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2)


            // Set the renderer to our demo renderer, defined below.
            mRenderer = LessonSevenRenderer(this, mGLSurfaceView)
            mGLSurfaceView.setRenderer(mRenderer!!, resources.displayMetrics.density)
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return
        }

        findViewById<View>(R.id.button_decrease_num_cubes).setOnClickListener { decreaseCubeCount() }

        findViewById<View>(R.id.button_increase_num_cubes).setOnClickListener { increaseCubeCount() }

        findViewById<View>(R.id.button_switch_VBOs).setOnClickListener { toggleVBOs() }

        findViewById<View>(R.id.button_switch_stride).setOnClickListener { toggleStride() }
    }

    override fun onResume() {
        // The activity must call the GL surface view's onResume() on activity
        // onResume().
        super.onResume()
        mGLSurfaceView.onResume()
    }

    override fun onPause() {
        // The activity must call the GL surface view's onPause() on activity
        // onPause().
        super.onPause()
        mGLSurfaceView.onPause()
    }

    private fun decreaseCubeCount() {
        mGLSurfaceView.queueEvent { mRenderer!!.decreaseCubeCount() }
    }

    private fun increaseCubeCount() {
        mGLSurfaceView.queueEvent { mRenderer!!.increaseCubeCount() }
    }

    private fun toggleVBOs() {
        mGLSurfaceView.queueEvent { mRenderer!!.toggleVBOs() }
    }

    private fun toggleStride() {
        mGLSurfaceView.queueEvent { mRenderer!!.toggleStride() }
    }

    fun updateVboStatus(usingVbos: Boolean) {
        runOnUiThread {
            if (usingVbos) {
                (findViewById<View>(R.id.button_switch_VBOs) as Button).setText(R.string.lesson_seven_using_VBOs)
            } else {
                (findViewById<View>(R.id.button_switch_VBOs) as Button).setText(R.string.lesson_seven_not_using_VBOs)
            }
        }
    }

    fun updateStrideStatus(useStride: Boolean) {
        runOnUiThread {
            if (useStride) {
                (findViewById<View>(R.id.button_switch_stride) as Button).setText(R.string.lesson_seven_using_stride)
            } else {
                (findViewById<View>(R.id.button_switch_stride) as Button).setText(R.string.lesson_seven_not_using_stride)
            }
        }
    }
}