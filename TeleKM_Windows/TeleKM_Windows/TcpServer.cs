using TeleKM;
using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;

// State object for reading client data asynchronously  
public class StateObject
{
    // Client  socket.  
    public Socket workSocket = null;
    // Size of receive buffer.  
    public const int BufferSize = 1024;
    // Receive buffer.  
    public byte[] buffer = new byte[BufferSize];
    // Received data string.  
    public StringBuilder sb = new StringBuilder();
}

public class AsynchronousSocketListener
{
    // Thread signal.  
    public ManualResetEvent allDone = new ManualResetEvent(false);
    private KMInterface mMouseInterface;

    public AsynchronousSocketListener(KMInterface mouseInterface)
    {
        mMouseInterface = mouseInterface;
    }

    public void StartTCPServer()
    {
        this.StartListening();
    }

    public void StartListening()
    {
        // Get the local IP address by sending a test packet to a DNS server and noting the outgoing socket IP
        IPAddress localIP;
        using (Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, 0))
        {
            socket.Connect("8.8.8.8", 65530);
            localIP = (socket.LocalEndPoint as IPEndPoint).Address;
        }

        // Establish the local endpoint for the socket.  
        // The DNS name of the computer  
        // running the listener is "host.contoso.com".  
        IPEndPoint localEndPoint = new IPEndPoint(localIP, 9050);

        // Create a TCP/IP socket.  
        Socket listener = new Socket(localIP.AddressFamily,
            SocketType.Stream, ProtocolType.Tcp);

        // Bind the socket to the local endpoint and listen for incoming connections.  
        try
        {
            listener.Bind(localEndPoint);
            listener.Listen(100);

            while (true)
            {
                // Set the event to nonsignaled state.  
                allDone.Reset();

                // Start an asynchronous socket to listen for connections.  
                Console.WriteLine("Waiting for a connection...");
                listener.BeginAccept(
                    new AsyncCallback(AcceptCallback),
                    listener);

                // Wait until a connection is made before continuing.  
                allDone.WaitOne();
            }

        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
        }

        Console.WriteLine("\nPress ENTER to continue...");
        Console.Read();

    }

    public void AcceptCallback(IAsyncResult ar)
    {
        // Signal the main thread to continue.  
        allDone.Set();

        // Get the socket that handles the client request.  
        Socket listener = (Socket)ar.AsyncState;
        Socket handler = listener.EndAccept(ar);

        // Create the state object.  
        StateObject state = new StateObject();
        state.workSocket = handler;
        handler.BeginReceive(state.buffer, 0, StateObject.BufferSize, 0,
            new AsyncCallback(ReadCallback), state);
    }

    public void ReadCallback(IAsyncResult ar)
    {
        try
        {
            String content = String.Empty;

            // Retrieve the state object and the handler socket  
            // from the asynchronous state object.  
            StateObject state = (StateObject)ar.AsyncState;
            Socket handler = state.workSocket;

            KMInterface mouseInterface = new KMInterface();

            // Read data from the client socket. 
            int bytesRead = handler.EndReceive(ar);
            if (bytesRead > 0)
            {
                // There  might be more data, so store the data received so far.  
                state.sb.Append(Encoding.ASCII.GetString(
                    state.buffer, 0, bytesRead));

                // Check for end-of-file tag. If it is not there, read   
                // more data.  
                content = state.sb.ToString();
                if (content.Contains("<EOF>"))
                {
                    Console.WriteLine("Client disconnected");
                    handler.Shutdown(SocketShutdown.Both);
                }
                else
                {
                    if (content.Contains("LEFT_UP"))
                    {
                        Console.WriteLine("Left Up");
                        mouseInterface.DoMouseEvent(KMInterface.MouseEvent.LEFT_UP);
                        state.sb.Clear();
                    }
                    else if (content.Contains("LEFT_DOWN"))
                    {
                        Console.WriteLine("Left Down");
                        mouseInterface.DoMouseEvent(KMInterface.MouseEvent.LEFT_DOWN);
                        state.sb.Clear();
                    }
                    else if (content.Contains("RIGHT_UP"))
                    {
                        Console.WriteLine("Right Up");
                        mouseInterface.DoMouseEvent(KMInterface.MouseEvent.RIGHT_UP);
                        state.sb.Clear();
                    }
                    else if (content.Contains("RIGHT_DOWN"))
                    {
                        Console.WriteLine("Right Down");
                        mouseInterface.DoMouseEvent(KMInterface.MouseEvent.RIGHT_DOWN);
                        state.sb.Clear();
                    }
                    else if (content.Contains("VOL_UP"))
                    {
                        Console.WriteLine("Volume Up");
                        mouseInterface.DoVolumeEvent(KMInterface.VolumeEvent.VOL_UP);
                        state.sb.Clear();
                    }
                    else if (content.Contains("VOL_DOWN"))
                    {
                        Console.WriteLine("Volume Down");
                        mouseInterface.DoVolumeEvent(KMInterface.VolumeEvent.VOL_UP);
                        state.sb.Clear();
                    }
                    else if (content.Contains("VOL_MUTE"))
                    {
                        Console.WriteLine("Volume Mute");
                        mouseInterface.DoVolumeEvent(KMInterface.VolumeEvent.VOL_MUTE);
                        state.sb.Clear();
                    }
                    else if (content.Contains("<kb") && content.Contains(">"))
                    {
                        content = content.Substring(content.IndexOf("<kb")).Trim();
                        string[] parts = content.Split(' ');
                        int codePoint = int.Parse(parts[1]);
                        bool isShifted = bool.Parse(parts[2]);
                        bool isControlled = bool.Parse(parts[3]);
                        bool isSupered = bool.Parse(parts[4]);
                        bool isAlted = bool.Parse(parts[5]);
                        mouseInterface.KeyboardEvent(codePoint, isShifted, isControlled, isSupered, isAlted);
                        state.sb.Clear();
                    }

                    // Not all data received. Get more.
                    handler.BeginReceive(state.buffer, 0, StateObject.BufferSize, 0, new AsyncCallback(ReadCallback), state);
                }
            }
        }
        catch (SocketException e)
        {
            Console.WriteLine("SOCKET FAILURE " + e.Message);
        }

    }

    private void Send(Socket handler, String data)
    {
        // Convert the string data to byte data using ASCII encoding.  
        byte[] byteData = Encoding.ASCII.GetBytes(data);

        // Begin sending the data to the remote device.  
        handler.BeginSend(byteData, 0, byteData.Length, 0,
            new AsyncCallback(SendCallback), handler);
    }

    private void SendCallback(IAsyncResult ar)
    {
        try
        {
            // Retrieve the socket from the state object.  
            Socket handler = (Socket)ar.AsyncState;

            // Complete sending the data to the remote device.  
            int bytesSent = handler.EndSend(ar);
            Console.WriteLine("Sent {0} bytes to client.", bytesSent);

            handler.Shutdown(SocketShutdown.Both);
            handler.Close();

        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
        }
    }
}