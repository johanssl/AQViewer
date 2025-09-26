package org.fmi.aq.enfuser.ftools;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class ConsoleControl {

    // Only load Windows native libs if on Windows
    private static final boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("win");

    // Interfaces for Windows APIs
    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
        boolean AllocConsole();
        boolean FreeConsole();
        long GetConsoleWindow();
    }

    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);
        boolean ShowWindow(long hWnd, int nCmdShow);
    }

    private static final int SW_HIDE = 0;
    private static final int SW_SHOW = 5;

    private static boolean consoleVisible = false;
    private static boolean warnedNonWindows = false;

    /**
     * Ensure a console exists and streams are hooked up.
     */
    public static void ensureConsole() {
        if (!IS_WINDOWS) {
            warnNonWindows();
            return;
        }

        long hWnd = Kernel32.INSTANCE.GetConsoleWindow();
        if (hWnd == 0) {
            Kernel32.INSTANCE.AllocConsole();
            reconnectStreams();
            consoleVisible = true;
            System.out.println("[ConsoleControl] Console allocated.");
        }
    }

    /**
     * Toggle console visibility (Windows only).
     */
    public static void toggleConsole() {
        if (!IS_WINDOWS) {
            warnNonWindows();
            return;
        }

        long hWnd = Kernel32.INSTANCE.GetConsoleWindow();
        if (hWnd == 0) {
            // No console yet
            ensureConsole();
        } else {
            if (consoleVisible) {
                User32.INSTANCE.ShowWindow(hWnd, SW_HIDE);
                consoleVisible = false;
            } else {
                User32.INSTANCE.ShowWindow(hWnd, SW_SHOW);
                consoleVisible = true;
            }
        }
    }

    /**
     * Redirect System.out, System.err, and System.in
     * to the Windows console.
     */
    private static void reconnectStreams() {
        try {
            PrintStream ps = new PrintStream(new FileOutputStream("CONOUT$"));
            System.setOut(ps);
            System.setErr(ps);
            InputStream is = new FileInputStream("CONIN$");
            System.setIn(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void warnNonWindows() {
        if (!warnedNonWindows) {
            System.err.println("[ConsoleControl] Console toggling not supported on this OS.");
            warnedNonWindows = true;
        }
    }
}