package com.learnopengles.android.lesson6

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent

class LessonSixGLSurfaceView : GLSurfaceView {
    private var mRenderer: LessonSixRenderer? = null

    // Offsets for touch events
    private var mPreviousX: Float = 0.toFloat()
    private var mPreviousY: Float = 0.toFloat()

    private var mDensity: Float = 0.toFloat()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            val x = event.x
            val y = event.y

            if (event.action == MotionEvent.ACTION_MOVE) {
                if (mRenderer != null) {
                    val deltaX = (x - mPreviousX) / mDensity / 2f
                    val deltaY = (y - mPreviousY) / mDensity / 2f

                    mRenderer!!.mDeltaX += deltaX
                    mRenderer!!.mDeltaY += deltaY
                }
            }

            mPreviousX = x
            mPreviousY = y

            return true
        } else {
            return super.onTouchEvent(event)
        }
    }

    fun setRenderer(renderer: LessonSixRenderer, density: Float) {
        mRenderer = renderer
        mDensity = density
        super.setRenderer(renderer)
    }
}
