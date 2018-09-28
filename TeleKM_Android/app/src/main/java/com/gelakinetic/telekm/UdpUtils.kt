package com.gelakinetic.telekm

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.util.Log
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Future

class UdpUtils(private val mainActivity: MainActivity) {

    private var mServerFuture: Future<Unit>? = null
    private var mUdpFutures: ArrayList<Future<Unit>> = ArrayList()
    private val mHandler: Handler = Handler()

    fun startUdpUtils() {

        if (null == mServerFuture) {

            // Start spinning the server
            mServerFuture = doAsync {
                DatagramSocket(9050, InetAddress.getByName("0.0.0.0")).use { receiverSocket ->

                    Log.i(MainActivity.UDP_TAG, "Ready to receive broadcast packets!")
                    receiverSocket.broadcast = true

                    var serverAddress = ""
                    while (serverAddress.isEmpty()) {

                        //Receive a packet
                        val receiveBuffer = ByteArray(4096)
                        val packet = DatagramPacket(receiveBuffer, receiveBuffer.size)
                        receiverSocket.receive(packet)

                        //Packet received
                        val data = String(packet.data).trim { it <= ' ' }

                        if ("Connect to me!" == data) {
                            Log.i(MainActivity.UDP_TAG, "Packet received from: " + packet.address.hostAddress)
                            Log.i(MainActivity.UDP_TAG, "Packet received; data: $data")

                            uiThread {
                                mainActivity.showSnackbar(R.string.server_found)
                            }

                            serverAddress = packet.address.hostAddress

                            // Make sure another UDP broadcast isn't sent
                            mHandler.removeCallbacksAndMessages(null)

                            // Now that we know the IP address of the server, open a TCP connection
                            mainActivity.setTcpServerAddress(packet.address.hostAddress)
                        }
                    }
                    Log.i(MainActivity.UDP_TAG, "All done")
                }
            }

            // Start sending UDP broadcast packets
            sendUdpPacket(getBroadcastAddress(), true, MainActivity.UDP_BROADCAST_MESSAGE)
        }
    }

    // https://stackoverflow.com/questions/17308729/send-broadcast-udp-but-not-receive-it-on-other-android-devices
    internal fun sendUdpPacket(serverAddress: InetAddress?, isBroadcast: Boolean, message: String) {

        if (null == serverAddress) {
            return
        }

        var future: Future<Unit>? = null
        future = doAsync {
            val payload = message.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(payload, payload.size, serverAddress, 9050)

            val sock = DatagramSocket()
            if (isBroadcast) {
                sock.broadcast = true
            }
            sock.send(packet)

            sock.disconnect()
            sock.close()

            uiThread {
                if (isBroadcast) {
                    mHandler.postDelayed({
                        sendUdpPacket(getBroadcastAddress(), true, MainActivity.UDP_BROADCAST_MESSAGE)
                    }, 5000)

                    Log.v(MainActivity.UDP_TAG, "Broadcast Sent at " + System.currentTimeMillis() / 1000)
                    mainActivity.showSnackbar(R.string.searching_for_server)
                }
            }
            mUdpFutures.remove(future)
        }
        mUdpFutures.add(future)
    }

    private fun getBroadcastAddress(): InetAddress {
        val wifi = mainActivity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        // handle null somehow

        val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3)
            quads[k] = ((broadcast shr (k * 8)) and 0xFF).toByte()
        return InetAddress.getByAddress(quads)
    }

    fun stopUdpUtils() {
        // TODO lifecycle bug. changing orientation then connecting still sends broadcasts and doesnt set the UI
        // TODO handler was cleared, but perhaps it wasnt the handler that had the runnable???
        // this kills loops in case the server was never received
        mServerFuture?.cancel(true)
        mUdpFutures.forEach { future -> future.cancel(true) }
        mUdpFutures.clear()
        mHandler.removeCallbacksAndMessages(null)
        mServerFuture = null
    }
}