using System;
using System.Net;
using System.Net.Sockets;
using System.Text;

namespace TeleKM
{
    class UdpServer
    {
        private KMInterface mouseInterface;

        public UdpServer(KMInterface mouseInterface)
        {
            this.mouseInterface = mouseInterface;
        }

        public void StartUDPServer()
        {
            Console.WriteLine("Starting server thread...");

            byte[] data = new byte[1024];
            IPEndPoint ipep = new IPEndPoint(IPAddress.Any, 9050);
            try
            {
                UdpClient newsock = new UdpClient(ipep);

                Console.WriteLine("Waiting for a client...");

                IPEndPoint sender = new IPEndPoint(IPAddress.Any, 0);

                float errorX = 0;
                float errorY = 0;
                float cumulataveScroll = 0;

                while (true)
                {
                    data = newsock.Receive(ref sender);
                    Console.WriteLine("Message received from {0}:", sender.ToString());
                    Console.WriteLine(Encoding.ASCII.GetString(data, 0, data.Length));

                    string payload = Encoding.ASCII.GetString(data, 0, data.Length);
                    if (payload.Equals("Anybody out there?"))
                    {
                        //SendBroadcast();
                        SendUdpPacket(IPAddress.Parse(sender.ToString().Split(':')[0]));
                        SendUdpPacket(IPAddress.Broadcast);
                    }
                    else if (payload.Contains("<mv") && payload.Contains(">"))
                    {
                        string[] parts = payload.Split(' ');
                        float deltaX = float.Parse(parts[1]) + errorX;
                        float deltaY = float.Parse(parts[2]) + errorY;
                        mouseInterface.TranslateCursor((int)Math.Round(deltaX), (int)Math.Round(deltaY));
                        errorX = deltaX - (int)Math.Round(deltaX);
                        errorY = deltaY - (int)Math.Round(deltaY);
                    }
                    else if (payload.Contains("<scr") && payload.Contains(">"))
                    {
                        string[] parts = payload.Split(' ');
                        cumulataveScroll += float.Parse(parts[1]);
                        if (cumulataveScroll > 1)
                        {
                            mouseInterface.DoMouseEvent(KMInterface.MouseEvent.WHEEL_UP);
                            cumulataveScroll -= 1;
                        }
                        else if (cumulataveScroll < 1)
                        {
                            mouseInterface.DoMouseEvent(KMInterface.MouseEvent.WHEEL_DOWN);
                            cumulataveScroll += 1;
                        }
                    }
                }
            }
            catch (SocketException ex)
            {
                Console.WriteLine(ex.Message);
                return;
            }
        }

        private void SendUdpPacket(IPAddress ipAddr)
        {
            try
            {
                Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
                IPEndPoint endPoint = new IPEndPoint(ipAddr, 9050);

                string text = "Connect to me!";
                byte[] send_buffer = Encoding.ASCII.GetBytes(text);

                sock.SendTo(send_buffer, endPoint);
                sock.Close();
            }
            catch (SocketException e)
            {
                Console.Write(e.ErrorCode);
            }
        }
    }
}
