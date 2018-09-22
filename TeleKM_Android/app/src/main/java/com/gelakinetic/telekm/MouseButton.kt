package com.gelakinetic.telekm

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat


class MouseButton(context: Context, attributeSet: AttributeSet) : com.google.android.material.button.MaterialButton(context, attributeSet) {

    // Variable for a listener for events
    private var mMouseButtonListener: ((MotionEvent) -> Unit)? = null

    // Variables for vibration
    private var mVibrator: Vibrator? = null
    private val mVibrateTimeMs: Long = 50

    // Variables for drawing the cursor
    private var mDrawCursor: Boolean = false
    private val touchPoint: PointF = PointF(0f, 0f)
    private var mCursorIsVisible: Boolean = false
    private val touchpointRadiusPx: Float = convertDpToPixel(32f)

    private val mCursorStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.colorPrimaryDark)
        isAntiAlias = true
    }

    private val mCursorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.colorPrimary)
        isAntiAlias = true
    }

    private var rippleRadiusScalar: Float = 0f
    private val rippleAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration(300)

    init {
        rippleAnimator.interpolator = AccelerateDecelerateInterpolator()
        rippleAnimator.addUpdateListener { animation ->
            rippleRadiusScalar = animation.animatedFraction
            invalidate()
        }
        rippleAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                mCursorIsVisible = true
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
            }
        })
    }

    private val rippleFadeAnimator: ValueAnimator = ValueAnimator.ofInt(255, 0).setDuration(300)

    init {
        rippleFadeAnimator.interpolator = AccelerateDecelerateInterpolator()
        rippleFadeAnimator.addUpdateListener { animation ->
            mCursorFillPaint.alpha = animation.animatedValue as Int
            mCursorStrokePaint.alpha = animation.animatedValue as Int
            invalidate()
        }
        rippleFadeAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                mCursorIsVisible = false
            }

            override fun onAnimationCancel(animation: Animator?) {
            }
        })
    }

    /***********************************************************************************************
     *
     * Event functions
     *
     */

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (isEnabled) {
            mMouseButtonListener?.invoke(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    vibrate(mVibrateTimeMs)
                    startDrawingCursor(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    updateCursorPosition(event)
                }
                MotionEvent.ACTION_UP -> {
                    vibrate(mVibrateTimeMs)
                    stopDrawingCursor(event)
                }
            }
        }

        return super.onTouchEvent(event)
    }

    fun setMouseButtonListener(listener: (MotionEvent) -> Unit) {
        mMouseButtonListener = listener
    }

    /***********************************************************************************************
     *
     * Vibration functions
     *
     */

    fun setVibrate(b: Boolean, ctx: Context) {
        mVibrator = if (b) {
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        } else {
            null
        }
    }

    private fun vibrate(timeMs: Long) {
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator?.vibrate(VibrationEffect.createOneShot(timeMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            @Suppress("DEPRECATION")
            mVibrator?.vibrate(timeMs)
        }
    }

    /***********************************************************************************************
     *
     * Cursor Drawing functions
     *
     */

    override fun onDraw(canvas: Canvas) {
        // Draw the touch point
        if (mDrawCursor && mCursorIsVisible) {
            canvas.drawCircle(touchPoint.x, touchPoint.y, touchpointRadiusPx * rippleRadiusScalar, mCursorStrokePaint)
            canvas.drawCircle(touchPoint.x, touchPoint.y, touchpointRadiusPx * rippleRadiusScalar * 0.8f, mCursorFillPaint)
        }
    }

    private fun startDrawingCursor(event: MotionEvent) {
        if (mDrawCursor) {
            // Start drawing the cursor
            touchPoint.set(event.x, event.y)

            // Cancel any running ripple animations, then start the grow
            if (rippleAnimator.isRunning) {
                rippleAnimator.cancel()
            }
            if (rippleFadeAnimator.isRunning) {
                rippleFadeAnimator.cancel()
            }
            rippleAnimator.start()

            // Reset ripple variables
            rippleRadiusScalar = 0f
            mCursorStrokePaint.alpha = 255
            mCursorFillPaint.alpha = 255

            // Trigger a redraw
            invalidate()
        }
    }

    private fun updateCursorPosition(event: MotionEvent) {
        if (mDrawCursor) {
            // Update the touch point
            touchPoint.set(event.x, event.y)
            // Trigger a redraw
            invalidate()
        }
    }

    private fun stopDrawingCursor(event: MotionEvent) {
        if (mDrawCursor) {
            touchPoint.set(event.x, event.y)

            // Cancel any running ripple animations, then start the fade
            if (rippleAnimator.isRunning) {
                rippleAnimator.cancel()
            }
            if (rippleFadeAnimator.isRunning) {
                rippleFadeAnimator.cancel()
            }
            rippleFadeAnimator.start()

            // Trigger a redraw
            invalidate()
        }
    }

    private fun convertDpToPixel(dp: Float): Float {
        val metrics = Resources.getSystem().displayMetrics
        val px = dp * (metrics.densityDpi / 160f)
        return Math.round(px).toFloat()
    }

    fun setDrawCursor(b: Boolean) {
        mDrawCursor = b
    }

}