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

class UdpUtils(private val mainActivity: MainActivity) {

    private var mServerAddress: String = ""
    private var mConnecting: Boolean = false

    fun startUdpUtils() {

        mServerAddress = ""

        if (!mConnecting) {
            mConnecting = true
            // Start spinning the server
            doAsync {
                DatagramSocket(9050, InetAddress.getByName("0.0.0.0")).use { receiverSocket ->

                    Log.i(MainActivity.UDP_TAG, "Ready to receive broadcast packets!")
                    receiverSocket.broadcast = true

                    while (mServerAddress.isEmpty()) {

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

                            mServerAddress = packet.address.hostAddress

                            // Now that we know the IP address of the server, open a TCP connection
                            mainActivity.setTcpServerAddress(packet.address.hostAddress)

                            mConnecting = false
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

        doAsync {
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
                Handler().postDelayed({
                    if (mServerAddress.isEmpty()) {
                        sendUdpPacket(getBroadcastAddress(), true, MainActivity.UDP_BROADCAST_MESSAGE)
                    }
                }, 5000)
                if (isBroadcast) {
                    Log.v(MainActivity.UDP_TAG, "Broadcast Sent")
                    mainActivity.showSnackbar(R.string.searching_for_server)
                }
            }
        }
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
        // this kills loops in case the server was never received
        mServerAddress = "die now"
    }
}