package com.learnopengles.android.lesson5

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class LessonFiveGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private var mRenderer: LessonFiveRenderer? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (mRenderer != null) {
                    // Ensure we call switchMode() on the OpenGL thread.
                    // queueEvent() is a method of GLSurfaceView that will do this for us.
                    queueEvent { mRenderer!!.switchMode() }

                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun setRenderer(renderer: LessonFiveRenderer) {
        mRenderer = renderer
        super.setRenderer(renderer)
    }
}
