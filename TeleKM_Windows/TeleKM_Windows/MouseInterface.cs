using System.Collections.Generic;
using System.Runtime.InteropServices;
using WindowsInput;
using WindowsInput.Native;

namespace TeleKM
{
    public class KMInterface
    {
        [DllImport("user32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        static extern bool SetCursorPos(int x, int y);

        [DllImport("user32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        static extern bool GetCursorPos(out POINT lpPoint);

        struct FIXED
        {
            public short fract;
            public short value;
        }

        struct POINT
        {
            public FIXED x;
            public FIXED y;
        }

        private enum NonstandardKeyCodes
        {
            KEYCODE_SHIFT = -1,
            KEYCODE_MODE_CHANGE = -2,
            KEYCODE_CANCEL = -3,
            KEYCODE_DONE = -4,
            KEYCODE_DELETE = -5,
            KEYCODE_ALT = -6,
            KEYCODE_CTRL = -7,
            KEYCODE_SUPER = -8
        }

        public enum MouseEvent
        {
            LEFT_DOWN,
            LEFT_UP,
            RIGHT_DOWN,
            RIGHT_UP,
            WHEEL_DOWN,
            WHEEL_UP
        }

        public enum VolumeEvent
        {
            VOL_UP,
            VOL_DOWN,
            VOL_MUTE,
        }

        private InputSimulator mInputSimulator;

        public KMInterface()
        {
            mInputSimulator = new InputSimulator();
            ((MouseSimulator)mInputSimulator.Mouse).MouseWheelClickSize = 60;
        }

        public void TranslateCursor(int x, int y)
        {
            POINT pos;
            GetCursorPos(out pos);
            SetCursorPos(pos.x.fract + x, pos.y.fract + y);
        }

        public void DoMouseEvent(MouseEvent mouseEvent)
        {
            switch (mouseEvent)
            {
                case MouseEvent.LEFT_DOWN:
                    mInputSimulator.Mouse.LeftButtonDown();
                    break;
                case MouseEvent.LEFT_UP:
                    mInputSimulator.Mouse.LeftButtonUp();
                    break;
                case MouseEvent.RIGHT_DOWN:
                    mInputSimulator.Mouse.RightButtonDown();
                    break;
                case MouseEvent.RIGHT_UP:
                    mInputSimulator.Mouse.RightButtonUp();
                    break;
                case MouseEvent.WHEEL_DOWN:
                    mInputSimulator.Mouse.VerticalScroll(-1);
                    break;
                case MouseEvent.WHEEL_UP:
                    mInputSimulator.Mouse.VerticalScroll(1);
                    break;
            }
        }

        public void DoVolumeEvent(VolumeEvent volumeEvent)
        {
            VirtualKeyCode key = VirtualKeyCode.NO_KEY;
            switch (volumeEvent)
            {
                case VolumeEvent.VOL_UP:
                    key = VirtualKeyCode.VOLUME_UP;
                    break;
                case VolumeEvent.VOL_DOWN:
                    key = VirtualKeyCode.VOLUME_DOWN;
                    break;
                case VolumeEvent.VOL_MUTE:
                    key = VirtualKeyCode.VOLUME_MUTE;
                    break;
            }
            if (VirtualKeyCode.NO_KEY != key)
            {
                mInputSimulator.Keyboard.KeyPress(key);
            }
        }

        internal void KeyboardEvent(int codePoint, bool isShifted, bool isControlled, bool isSupered, bool isAlted)
        {
            if (codePoint >= 0)
            {
                bool shiftOverride;
                VirtualKeyCode key = UnicodeToKeyCode(codePoint, out shiftOverride);
                if (VirtualKeyCode.NO_KEY != key)
                {
                    List<VirtualKeyCode> modifiers = new List<VirtualKeyCode>(4);

                    if (isShifted || shiftOverride)
                    {
                        modifiers.Add(VirtualKeyCode.SHIFT);
                    }
                    if (isControlled)
                    {
                        modifiers.Add(VirtualKeyCode.CONTROL);
                    }
                    if (isSupered)
                    {
                        modifiers.Add(VirtualKeyCode.LWIN);
                    }
                    if (isAlted)
                    {
                        modifiers.Add(VirtualKeyCode.MENU);
                    }
                    mInputSimulator.Keyboard.ModifiedKeyStroke(modifiers, key);
                }
            }
            else
            {
                switch ((NonstandardKeyCodes)codePoint)
                {
                    case NonstandardKeyCodes.KEYCODE_ALT:
                    case NonstandardKeyCodes.KEYCODE_CTRL:
                    case NonstandardKeyCodes.KEYCODE_SUPER:
                    case NonstandardKeyCodes.KEYCODE_SHIFT:
                    case NonstandardKeyCodes.KEYCODE_MODE_CHANGE:
                        {
                            // These should never be received, but are used
                            break;
                        }
                    case NonstandardKeyCodes.KEYCODE_CANCEL:
                    case NonstandardKeyCodes.KEYCODE_DONE:
                    case NonstandardKeyCodes.KEYCODE_DELETE:
                        {
                            // These should never be sent
                            break;
                        }
                }
            }
        }

        internal VirtualKeyCode UnicodeToKeyCode(int codepoint, out bool isShifted)
        {
            isShifted = false;
            switch (char.ConvertFromUtf32(codepoint))
            {
                case "A":
                case "a":
                    return VirtualKeyCode.VK_A;
                case "B":
                case "b":
                    return VirtualKeyCode.VK_B;
                case "C":
                case "c":
                    return VirtualKeyCode.VK_C;
                case "D":
                case "d":
                    return VirtualKeyCode.VK_D;
                case "E":
                case "e":
                    return VirtualKeyCode.VK_E;
                case "F":
                case "f":
                    return VirtualKeyCode.VK_F;
                case "G":
                case "g":
                    return VirtualKeyCode.VK_G;
                case "H":
                case "h":
                    return VirtualKeyCode.VK_H;
                case "I":
                case "i":
                    return VirtualKeyCode.VK_I;
                case "J":
                case "j":
                    return VirtualKeyCode.VK_J;
                case "K":
                case "k":
                    return VirtualKeyCode.VK_K;
                case "L":
                case "l":
                    return VirtualKeyCode.VK_L;
                case "M":
                case "m":
                    return VirtualKeyCode.VK_M;
                case "N":
                case "n":
                    return VirtualKeyCode.VK_N;
                case "O":
                case "o":
                    return VirtualKeyCode.VK_O;
                case "P":
                case "p":
                    return VirtualKeyCode.VK_P;
                case "Q":
                case "q":
                    return VirtualKeyCode.VK_Q;
                case "R":
                case "r":
                    return VirtualKeyCode.VK_R;
                case "S":
                case "s":
                    return VirtualKeyCode.VK_S;
                case "T":
                case "t":
                    return VirtualKeyCode.VK_T;
                case "U":
                case "u":
                    return VirtualKeyCode.VK_U;
                case "V":
                case "v":
                    return VirtualKeyCode.VK_V;
                case "W":
                case "w":
                    return VirtualKeyCode.VK_W;
                case "X":
                case "x":
                    return VirtualKeyCode.VK_X;
                case "Y":
                case "y":
                    return VirtualKeyCode.VK_Y;
                case "Z":
                case "z":
                    return VirtualKeyCode.VK_Z;
                case "0":
                    return VirtualKeyCode.VK_0;
                case "1":
                    return VirtualKeyCode.VK_1;
                case "2":
                    return VirtualKeyCode.VK_2;
                case "3":
                    return VirtualKeyCode.VK_3;
                case "4":
                    return VirtualKeyCode.VK_4;
                case "5":
                    return VirtualKeyCode.VK_5;
                case "6":
                    return VirtualKeyCode.VK_6;
                case "7":
                    return VirtualKeyCode.VK_7;
                case "8":
                    return VirtualKeyCode.VK_8;
                case "9":
                    return VirtualKeyCode.VK_9;
                case " ":
                    return VirtualKeyCode.SPACE;
                case "!":
                    isShifted = true;
                    return VirtualKeyCode.VK_1;
                case "\"":
                    isShifted = true;
                    return VirtualKeyCode.OEM_7;
                case "#":
                    isShifted = true;
                    return VirtualKeyCode.VK_3;
                case "$":
                    isShifted = true;
                    return VirtualKeyCode.VK_4;
                case "%":
                    isShifted = true;
                    return VirtualKeyCode.VK_5;
                case "&":
                    isShifted = true;
                    return VirtualKeyCode.VK_7;
                case "'":
                    return VirtualKeyCode.OEM_7;
                case "(":
                    isShifted = true;
                    return VirtualKeyCode.VK_9;
                case ")":
                    isShifted = true;
                    return VirtualKeyCode.VK_0;
                case "*":
                    isShifted = true;
                    return VirtualKeyCode.VK_8;
                case "+":
                    isShifted = true;
                    return VirtualKeyCode.OEM_PLUS;
                case ",":
                    return VirtualKeyCode.OEM_COMMA;
                case "-":
                    return VirtualKeyCode.OEM_MINUS;
                case ".":
                    return VirtualKeyCode.OEM_PERIOD;
                case "/":
                    return VirtualKeyCode.OEM_2;
                case ":":
                    isShifted = true;
                    return VirtualKeyCode.OEM_1;
                case ";":
                    return VirtualKeyCode.OEM_1;
                case "<":
                    isShifted = true;
                    return VirtualKeyCode.OEM_COMMA;
                case "=":
                    return VirtualKeyCode.OEM_PLUS;
                case ">":
                    isShifted = true;
                    return VirtualKeyCode.OEM_PERIOD;
                case "?":
                    isShifted = true;
                    return VirtualKeyCode.OEM_2;
                case "@":
                    isShifted = true;
                    return VirtualKeyCode.VK_2;
                case "[":
                    return VirtualKeyCode.OEM_4;
                case "\\":
                    return VirtualKeyCode.OEM_5;
                case "]":
                    return VirtualKeyCode.OEM_6;
                case "^":
                    isShifted = true;
                    return VirtualKeyCode.VK_6;
                case "_":
                    isShifted = true;
                    return VirtualKeyCode.OEM_MINUS;
                case "`":
                    return VirtualKeyCode.OEM_3;
                case "{":
                    isShifted = true;
                    return VirtualKeyCode.OEM_4;
                case "|":
                    isShifted = true;
                    return VirtualKeyCode.OEM_5;
                case "}":
                    isShifted = true;
                    return VirtualKeyCode.OEM_6;
                case "~":
                    isShifted = true;
                    return VirtualKeyCode.OEM_3;
                default:
                    switch (codepoint)
                    {
                        case 8:
                            return VirtualKeyCode.BACK;
                        case 9:
                            return VirtualKeyCode.TAB;
                        case 10:
                            return VirtualKeyCode.RETURN;
                    }
                    return VirtualKeyCode.NO_KEY;
            }
        }
    }
}
