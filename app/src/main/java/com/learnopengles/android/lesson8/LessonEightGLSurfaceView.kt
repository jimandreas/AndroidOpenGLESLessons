package com.learnopengles.android.lesson8

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Toast

import com.learnopengles.android.R

class LessonEightGLSurfaceView : GLSurfaceView, ErrorHandler {
    private var renderer: LessonEightRenderer? = null

    // Offsets for touch events
    private var previousX: Float = 0.toFloat()
    private var previousY: Float = 0.toFloat()

    private var density: Float = 0.toFloat()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun handleError(errorType: ErrorHandler.ErrorType, cause: String) {
        // Queue on UI thread.
        post {
            val text: String

            when (errorType) {
                ErrorHandler.ErrorType.BUFFER_CREATION_ERROR -> text =
                        String.format(context.resources.getString(
                                R.string.lesson_eight_error_could_not_create_vbo), cause)
            }

            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            val x = event.x
            val y = event.y

            if (event.action == MotionEvent.ACTION_MOVE) {
                if (renderer != null) {
                    val deltaX = (x - previousX) / density / 2f
                    val deltaY = (y - previousY) / density / 2f

                    renderer!!.deltaX += deltaX
                    renderer!!.deltaY += deltaY
                }
            }

            previousX = x
            previousY = y

            return true
        } else {
            return super.onTouchEvent(event)
        }
    }

    // Hides superclass method.
    fun setRenderer(renderer: LessonEightRenderer, density: Float) {
        this.renderer = renderer
        this.density = density
        super.setRenderer(renderer)
    }
}
