package aethereal.util;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import org.lwjgl.glfw.GLFWNativeWin32;

/** Applies the Windows dark title bar colors to the GLFW window. */
public final class WindowTitleBar {
    private static final int DWMWA_BORDER_COLOR = 34;
    private static final int DWMWA_CAPTION_COLOR = 35;
    private static final int DWMWA_TEXT_COLOR = 36;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;

    private WindowTitleBar() {
    }

    public static void apply(long glfwWindow) {
        if (glfwWindow == 0L || !System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }

        try {
            long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
            if (hwnd == 0L) {
                return;
            }

            Function setWindowAttribute = NativeLibrary.getInstance("dwmapi").getFunction("DwmSetWindowAttribute");
            try (Memory value = new Memory(Integer.BYTES)) {
                value.setInt(0L, 1);
                setWindowAttribute.invokeInt(new Object[]{Pointer.createConstant(hwnd), DWMWA_USE_IMMERSIVE_DARK_MODE, value, Integer.BYTES});

                value.setInt(0L, 0x00000000); // COLORREF black
                setWindowAttribute.invokeInt(new Object[]{Pointer.createConstant(hwnd), DWMWA_CAPTION_COLOR, value, Integer.BYTES});

                value.setInt(0L, 0x00FFFFFF); // COLORREF white
                setWindowAttribute.invokeInt(new Object[]{Pointer.createConstant(hwnd), DWMWA_TEXT_COLOR, value, Integer.BYTES});

                value.setInt(0L, 0x00000000);
                setWindowAttribute.invokeInt(new Object[]{Pointer.createConstant(hwnd), DWMWA_BORDER_COLOR, value, Integer.BYTES});
            }
        } catch (Throwable ignored) {
            // Keep window creation working on unsupported Windows versions or other platforms.
        }
    }

    /** Refreshes the non-client frame after the window is visible. */
    public static void refresh(long glfwWindow) {
        apply(glfwWindow);
        if (glfwWindow == 0L || !System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        try {
            long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
            if (hwnd == 0L) return;
            // RDW_INVALIDATE | RDW_UPDATENOW | RDW_FRAME
            NativeLibrary.getInstance("user32").getFunction("RedrawWindow").invokeInt(
                    new Object[]{Pointer.createConstant(hwnd), Pointer.NULL, Pointer.NULL, 0x0501});
        } catch (Throwable ignored) {
            // The title bar is optional; unsupported systems retain their normal frame.
        }
    }
}
