package com.gelakinetic.telekm

import android.util.Log
import org.jetbrains.anko.doAsync
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException

class TcpUtils constructor(private val mainActivity: MainActivity, serverIp: String) {

    private var mTcpSocket: Socket
    private var mTcpOutput: DataOutputStream

    init {
        Log.i(MainActivity.TCP_TAG, "Starting TCP utils")
        mTcpSocket = Socket(serverIp, 9050)
        mTcpOutput = DataOutputStream(mTcpSocket.getOutputStream())
        Log.i(MainActivity.TCP_TAG, "Started TCP utils")
        mainActivity.showSnackbar(R.string.server_connected)
        mainActivity.setEnabled(true)
    }

    fun sendTcpMessage(message: String, shouldClose: Boolean) {
        doAsync {

            try {
                if (!mTcpSocket.isClosed) {
                    mTcpOutput.writeUTF(message)
                }

                if (shouldClose) {
                    mTcpOutput.close()
                    mTcpSocket.close()
                }
            } catch (e: SocketException) {
                // TODO tear down everything, try reconnecting
                mainActivity.startConnectionProcess()
            }
        }
    }

    fun stopTcpClient() {
        Log.i(MainActivity.TCP_TAG, "Stopping TCP utils")
        mainActivity.showSnackbar(R.string.server_disconnected)
        sendTcpMessage(MainActivity.DISCONNECT_MESSAGE, true)
    }

    fun getServerAddress(): InetAddress {
        return mTcpSocket.inetAddress
    }
}
