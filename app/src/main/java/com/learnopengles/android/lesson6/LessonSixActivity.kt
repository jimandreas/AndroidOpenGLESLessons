@file:Suppress("DEPRECATION")

package com.learnopengles.android.lesson6

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Dialog
import android.opengl.GLES20
import android.os.Bundle
import android.view.View
import com.learnopengles.android.R

class LessonSixActivity : Activity() {
    /** Hold a reference to our GLSurfaceView  */
    private lateinit var mGLSurfaceView: LessonSixGLSurfaceView
    private var mRenderer: LessonSixRenderer? = null

    private var mMinSetting = -1
    private var mMagSetting = -1

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.lesson_six)

        mGLSurfaceView = findViewById<View>(R.id.gl_surface_view) as LessonSixGLSurfaceView

        // Check if the system supports OpenGL ES 2.0.
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2)


            // Set the renderer to our demo renderer, defined below.
            mRenderer = LessonSixRenderer(this)
            mGLSurfaceView.setRenderer(mRenderer!!, resources.displayMetrics.density)
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return
        }

        findViewById<View>(R.id.button_set_min_filter).setOnClickListener { showDialog(MIN_DIALOG) }

        findViewById<View>(R.id.button_set_mag_filter).setOnClickListener { showDialog(MAG_DIALOG) }

        // Restore previous settings
        if (savedInstanceState != null) {
            mMinSetting = savedInstanceState.getInt(MIN_SETTING, -1)
            mMagSetting = savedInstanceState.getInt(MAG_SETTING, -1)

            if (mMinSetting != -1) {
                setMinSetting(mMinSetting)
            }
            if (mMagSetting != -1) {
                setMagSetting(mMagSetting)
            }
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(MIN_SETTING, mMinSetting)
        outState.putInt(MAG_SETTING, mMagSetting)
    }

    private fun setMinSetting(item: Int) {
        mMinSetting = item

        mGLSurfaceView.queueEvent {
            val filter: Int = when (item) {
                0 -> GLES20.GL_NEAREST
                1 -> GLES20.GL_LINEAR
                2 -> GLES20.GL_NEAREST_MIPMAP_NEAREST
                3 -> GLES20.GL_NEAREST_MIPMAP_LINEAR
                4 -> GLES20.GL_LINEAR_MIPMAP_NEAREST
                else -> GLES20.GL_LINEAR_MIPMAP_LINEAR
            }
            mRenderer!!.setMinFilter(filter)
        }
    }

    private fun setMagSetting(item: Int) {
        mMagSetting = item

        mGLSurfaceView.queueEvent {
            val filter: Int = if (item == 0) {
                GLES20.GL_NEAREST
            } else {
                GLES20.GL_LINEAR
            }
            mRenderer!!.setMagFilter(filter)
        }
    }

    @Deprecated("Deprecated in API level 8", ReplaceWith("DialogFragment"))
    override fun onCreateDialog(id: Int): Dialog? {
        val dialog: Dialog?

        when (id) {
            MIN_DIALOG -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getText(R.string.lesson_six_set_min_filter_message))
                builder.setItems(resources.getStringArray(R.array.lesson_six_min_filter_types)) { _, item -> setMinSetting(item) }

                dialog = builder.create()
            }
            MAG_DIALOG -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getText(R.string.lesson_six_set_mag_filter_message))
                builder.setItems(resources.getStringArray(R.array.lesson_six_mag_filter_types)) { _, item -> setMagSetting(item) }

                dialog = builder.create()
            }
            else -> dialog = null
        }

        return dialog
    }

    companion object {
        private const val MIN_DIALOG = 1
        private const val MAG_DIALOG = 2

        private const val MIN_SETTING = "min_setting"
        private const val MAG_SETTING = "mag_setting"
    }
}