using System;
using System.Drawing;
using System.IO;
using System.Reflection;
using System.Threading;
using System.Windows.Forms;

namespace TeleKM
{
    static class Program
    {
        public static NotifyIcon notifyIcon { get; private set; }

        /// <summary>
        /// The main entry point for the application.
        /// </summary>
        [STAThread]
        static void Main()
        {
            KMInterface mouseInterface = new KMInterface();

            // Start UDP server
            UdpServer udpServer = new UdpServer(mouseInterface);
            Thread udpServerThread = new Thread(new ThreadStart(udpServer.StartUDPServer));
            udpServerThread.IsBackground = true;
            udpServerThread.Start();

            // start TCP server
            AsynchronousSocketListener tcpServer = new AsynchronousSocketListener(mouseInterface);
            Thread tcpServerThread = new Thread(new ThreadStart(tcpServer.StartTCPServer));
            tcpServerThread.IsBackground = true;
            tcpServerThread.Start();

            DisplayNotifyIcon();
            Application.Run();
        }

        static void DisplayNotifyIcon()
        {
            try
            {
                notifyIcon = new NotifyIcon();

                // Initialize the notification icon
                // notifyIcon.Icon = new System.Drawing.Icon(Path.GetFullPath(@"mouse.ico"));
                notifyIcon.Icon = Icon.ExtractAssociatedIcon(Assembly.GetExecutingAssembly().Location);
                notifyIcon.Text = "TeleKM";
                notifyIcon.Visible = true;

                // Initialize the menu item
                MenuItem menuItemExit = new MenuItem();
                menuItemExit.Index = 0;
                menuItemExit.Text = "Exit";
                menuItemExit.Click += new EventHandler(menuItemExit_Click);

                MenuItem menuItemAbout = new MenuItem();
                menuItemAbout.Index = 1;
                menuItemAbout.Text = "About";
                menuItemAbout.Click += new EventHandler(menuItemAbout_Click);

                // Initialize the context menu
                ContextMenu notifyIconContextMenu = new ContextMenu();
                notifyIconContextMenu.MenuItems.AddRange(
                            new MenuItem[] { menuItemAbout, menuItemExit });

                // Add the context menu to the notification icon
                notifyIcon.ContextMenu = notifyIconContextMenu;
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        private static void menuItemExit_Click(object Sender, EventArgs e)
        {
            // Close the form, which closes the application.
            notifyIcon.Visible = false;
            Application.Exit();
        }

        private static void menuItemAbout_Click(object Sender, EventArgs e)
        {
            DialogResult res = MessageBox.Show("Description", "Title");
        }
    }
}
