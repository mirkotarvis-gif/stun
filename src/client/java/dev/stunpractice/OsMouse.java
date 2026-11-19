package dev.stunpractice;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.awt.Robot;
import java.awt.event.InputEvent;

/**
 * Presses the real left mouse button at the operating-system level, exactly as if you clicked.
 * The game then handles that click through its normal input path; this mod never builds or sends packets.
 *
 * Windows: user32 mouse_event via JNA (JNA ships with Minecraft).
 * macOS / Linux (X11): java.awt.Robot.
 */
final class OsMouse {
	private interface User32 extends Library {
		int MOUSEEVENTF_LEFTDOWN = 0x0002;
		int MOUSEEVENTF_LEFTUP = 0x0004;

		void mouse_event(int dwFlags, int dx, int dy, int dwData, Pointer dwExtraInfo);
	}

	private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
	private static User32 user32;
	private static Robot robot;
	private static boolean initFailed;

	private OsMouse() {}

	static synchronized void init() {
		try {
			if (WINDOWS) {
				user32 = Native.load("user32", User32.class);
			} else {
				// Minecraft may run AWT headless; Robot needs it off.
				System.setProperty("java.awt.headless", "false");
				robot = new Robot();
			}
			StunPracticeClient.LOG.info("Stun Practice: OS mouse ready ({})", WINDOWS ? "Windows user32" : "AWT Robot");
		} catch (Throwable t) {
			initFailed = true;
			StunPracticeClient.LOG.error("Stun Practice: couldn't get OS mouse access, auto click disabled", t);
		}
	}

	static boolean available() {
		return !initFailed && (user32 != null || robot != null);
	}

	/** Blocking: press, hold for holdMs, release. Call off the render thread. */
	static void click(int holdMs) throws InterruptedException {
		down();
		try {
			Thread.sleep(holdMs);
		} finally {
			up(); // always release, even if interrupted, so the button never gets stuck
		}
	}

	private static void down() {
		if (user32 != null) user32.mouse_event(User32.MOUSEEVENTF_LEFTDOWN, 0, 0, 0, null);
		else if (robot != null) robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void up() {
		if (user32 != null) user32.mouse_event(User32.MOUSEEVENTF_LEFTUP, 0, 0, 0, null);
		else if (robot != null) robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}
}
