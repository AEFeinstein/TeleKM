package com.gelakinetic.telekm

import android.content.Context
import android.content.res.Resources
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet

class EmbeddedKeyboardView(context: Context, thing: AttributeSet) : KeyboardView(context, thing) {

    var mKeyboardListener: ((Int, Boolean, Boolean, Boolean, Boolean) -> Unit)? = null

    private var isControlled: Boolean = false
    private var isSupered: Boolean = false
    private var isAlted: Boolean = false

    private val mKeyboardLayouts: IntArray = intArrayOf(R.xml.kbd_qwerty, R.xml.kbd_symbols)
    private var mCurrentKeyboardIdx: Int = 0

    init {
        this.keyboard = Keyboard(context, mKeyboardLayouts[mCurrentKeyboardIdx])
        this.onKeyboardActionListener = EmbeddedKeyboardActionListener(context, this)
        this.isPreviewEnabled = true
    }

    fun setKeyboardListener(listener: (Int, Boolean, Boolean, Boolean, Boolean) -> Unit) {
        mKeyboardListener = listener
    }

    private fun getNextKeyboard(): Keyboard {
        mCurrentKeyboardIdx = (mCurrentKeyboardIdx + 1) % mKeyboardLayouts.size
        return Keyboard(context, mKeyboardLayouts[mCurrentKeyboardIdx])
    }

    private class EmbeddedKeyboardActionListener(private val context: Context, private val embeddedKeyboardView: EmbeddedKeyboardView) : KeyboardView.OnKeyboardActionListener {

        private val timeMs: Long = 50
        private var mVibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        override fun onPress(primaryCode: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(VibrationEffect.createOneShot(timeMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                //deprecated in API 26
                @Suppress("DEPRECATION")
                mVibrator.vibrate(timeMs)
            }
        }

        override fun onRelease(primaryCode: Int) {
            val resources: Resources = context.resources
            when (primaryCode) {
                resources.getInteger(R.integer.KEYCODE_SHIFT) -> {
                    embeddedKeyboardView.isShifted = !embeddedKeyboardView.isShifted
                }
                resources.getInteger(R.integer.KEYCODE_CTRL) -> {
                    embeddedKeyboardView.isControlled = !embeddedKeyboardView.isControlled
                }
                resources.getInteger(R.integer.KEYCODE_SUPER) -> {
                    embeddedKeyboardView.isSupered = !embeddedKeyboardView.isSupered
                }
                resources.getInteger(R.integer.KEYCODE_ALT) -> {
                    embeddedKeyboardView.isAlted = !embeddedKeyboardView.isAlted
                }
                resources.getInteger(R.integer.KEYCODE_MODE_CHANGE) -> {
                    this.embeddedKeyboardView.keyboard = embeddedKeyboardView.getNextKeyboard()
                }
                else -> {
                    // Unicode chars
//                    if (embeddedKeyboardView.isShifted) {
//                        val keyPressed = Character.toUpperCase(primaryCode)
                    embeddedKeyboardView.mKeyboardListener?.invoke(primaryCode,
                            embeddedKeyboardView.isShifted,
                            embeddedKeyboardView.isControlled,
                            embeddedKeyboardView.isSupered,
                            embeddedKeyboardView.isAlted)
//                    }
                }
            }
        }

        override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
            // TODO anything?
        }

        override fun onText(text: CharSequence?) {
            // Unused
        }

        override fun swipeLeft() {
            // Unused
        }

        override fun swipeRight() {
            // Unused
        }

        override fun swipeUp() {
            // Unused
        }

        override fun swipeDown() {
            // Unused
        }
    }
}