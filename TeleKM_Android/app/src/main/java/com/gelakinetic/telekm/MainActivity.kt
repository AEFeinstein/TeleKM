package com.gelakinetic.telekm

import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ContentLoadingProgressBar
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    companion object {
        internal const val TCP_TAG = "TCP"
        internal const val UDP_TAG = "UDP"

        internal const val UDP_BROADCAST_MESSAGE = "Anybody out there?"
        internal const val LEFT_UP_MESSAGE = "LEFT_UP"
        internal const val LEFT_DOWN_MESSAGE = "LEFT_DOWN"
        internal const val RIGHT_UP_MESSAGE = "RIGHT_UP"
        internal const val RIGHT_DOWN_MESSAGE = "RIGHT_DOWN"
        internal const val VOL_DOWN_MESSAGE = "VOL_DOWN"
        internal const val VOL_UP_MESSAGE = "VOL_UP"
        internal const val VOL_MUTE_MESSAGE = "VOL_MUTE"
        internal const val DISCONNECT_MESSAGE = "<EOF>"
        internal const val MOUSE_MESSAGE = "<mv %f %f >"
        internal const val WHEEL_MESSAGE = "<scr %f >"
        internal const val KEYBOARD_MESSAGE = "<kb %d %b %b %b %b >"
    }

    private val mLastTouchpadPoint: PointF = PointF(0f, 0f)
    private val mLastScrollPoint: PointF = PointF(0f, 0f)

    private val mUdpUtils: UdpUtils = UdpUtils(this)
    private var mTcpUtils: TcpUtils? = null

    private var mSnackbar: Snackbar? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                mTcpUtils?.sendTcpMessage(VOL_DOWN_MESSAGE, false)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                mTcpUtils?.sendTcpMessage(VOL_UP_MESSAGE, false)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                mTcpUtils?.sendTcpMessage(VOL_MUTE_MESSAGE, false)
                return true
            }
            else -> {
                return super.onKeyDown(keyCode, event)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the main view
        setContentView(R.layout.activity_main)
        setSupportActionBar(this.findViewById(R.id.toolbar))

        // Set up the left button actions
        this.findViewById<MouseButton>(R.id.left_click)?.setMouseButtonListener { motionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mTcpUtils?.sendTcpMessage(LEFT_DOWN_MESSAGE, false)
                }
                MotionEvent.ACTION_UP -> {
                    mTcpUtils?.sendTcpMessage(LEFT_UP_MESSAGE, false)
                }
            }
        }
        this.findViewById<MouseButton>(R.id.left_click)?.setVibrate(true, this)

        // Set up the right button actions
        this.findViewById<MouseButton>(R.id.right_click)?.setMouseButtonListener { motionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mTcpUtils?.sendTcpMessage(RIGHT_DOWN_MESSAGE, false)
                }
                MotionEvent.ACTION_UP -> {
                    mTcpUtils?.sendTcpMessage(RIGHT_UP_MESSAGE, false)
                }
            }
        }
        this.findViewById<MouseButton>(R.id.right_click)?.setVibrate(true, this)

        // Set up the touchpad
        this.findViewById<MouseButton>(R.id.touchpad)?.setMouseButtonListener { motionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Set the new point as the last touched point. There;s no movement here
                    mLastTouchpadPoint.set(motionEvent.x, motionEvent.y)
                }
                MotionEvent.ACTION_MOVE -> {
                    // Figure out how much the touch moved
                    val deltaX: Float = motionEvent.x - mLastTouchpadPoint.x
                    val deltaY: Float = motionEvent.y - mLastTouchpadPoint.y

                    // Send the movement to the server over UDP
                    mUdpUtils.sendUdpPacket(mTcpUtils?.getServerAddress(), false, String.format(MOUSE_MESSAGE, deltaX, deltaY))

                    // Set the new point as the last touched point
                    mLastTouchpadPoint.set(motionEvent.x, motionEvent.y)
                }
            }
        }
        this.findViewById<MouseButton>(R.id.touchpad)?.setDrawCursor(true)

        // Set up the touchpad
        this.findViewById<MouseButton>(R.id.scroll_bar)?.setMouseButtonListener { motionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Set the new point as the last touched point. There;s no movement here
                    mLastScrollPoint.set(motionEvent.x, motionEvent.y)
                }
                MotionEvent.ACTION_MOVE -> {
                    // Figure out how much the touch moved
                    val deltaY: Float = (motionEvent.y - mLastScrollPoint.y)

                    // Send the movement to the server over UDP
                    mUdpUtils.sendUdpPacket(mTcpUtils?.getServerAddress(), false, String.format(WHEEL_MESSAGE, deltaY))

                    // Set the new point as the last touched point
                    mLastScrollPoint.set(motionEvent.x, motionEvent.y)
                }
            }
        }
        this.findViewById<MouseButton>(R.id.scroll_bar)?.setDrawCursor(true)

        // Set up the keyboard
        this.findViewById<EmbeddedKeyboardView>(R.id.keyboard_view)?.setKeyboardListener { key, isShifted, isControlled, isSupered, isAlted ->
            mTcpUtils?.sendTcpMessage(String.format(KEYBOARD_MESSAGE, key, isShifted, isControlled, isSupered, isAlted), false)
        }
    }

    override fun onResume() {
        super.onResume()
        startConnectionProcess()
    }

    override fun onPause() {
        super.onPause()
        stopConnectionProcess()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, MousePreferenceActivity::class.java))
                true
            }
            R.id.action_about -> {
                AboutDialogFragment().show(supportFragmentManager, "AboutFragment")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun setTcpServerAddress(hostAddress: String) {
        if (null == mTcpUtils) {
            mTcpUtils = TcpUtils(this, hostAddress)
        }
    }

    internal fun showSnackbar(resId: Int) {
        showSnackbar(getString(resId))
    }

    private fun showSnackbar(message: CharSequence) {
        mSnackbar?.dismiss()
        mSnackbar = Snackbar.make(findViewById<View>(R.id.content_main), message, Snackbar.LENGTH_LONG)
        mSnackbar?.show()
    }

    internal fun setEnabled(isEnabled: Boolean) {
        runOnUiThread {
            this.findViewById<MouseButton>(R.id.left_click)?.isEnabled = isEnabled
            this.findViewById<MouseButton>(R.id.right_click)?.isEnabled = isEnabled
            this.findViewById<MouseButton>(R.id.touchpad)?.isEnabled = isEnabled
            this.findViewById<MouseButton>(R.id.scroll_bar)?.isEnabled = isEnabled
            this.findViewById<EmbeddedKeyboardView>(R.id.keyboard_view)?.isEnabled = isEnabled

            if (isEnabled) {
                this.findViewById<ContentLoadingProgressBar>(R.id.progress_circle)?.hide()
            } else {
                this.findViewById<ContentLoadingProgressBar>(R.id.progress_circle)?.show()
            }
        }
    }

    internal fun startConnectionProcess() {
        // Start listening for a response with the server IP
        mUdpUtils.startUdpUtils()
        mTcpUtils?.stopTcpClient()
        mTcpUtils = null
        setEnabled(false)
    }

    private fun stopConnectionProcess() {
        mUdpUtils.stopUdpUtils()
        mTcpUtils?.stopTcpClient()
    }
}
