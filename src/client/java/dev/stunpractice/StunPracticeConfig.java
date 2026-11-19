package dev.stunpractice;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Settings stored in .minecraft/config/stunpractice.properties */
public final class StunPracticeConfig {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("stunpractice.properties");

	/** Master on/off switch. */
	public static volatile boolean enabled = true;
	/** Wait this long (ms) after your shield-breaking hit before the auto click. */
	public static volatile int delayMs = 60;
	/** How long the mouse button is held down per click (ms). */
	public static volatile int holdMs = 15;
	/** Number of auto clicks after your hit (1 = a double click in total). */
	public static volatile int clicks = 1;
	/** Gap between auto clicks when clicks > 1 (ms). */
	public static volatile int gapMs = 50;
	/** Ignore new triggers for this long after one fires (ms), so you don't chain-trigger. */
	public static volatile int cooldownMs = 300;

	private StunPracticeConfig() {}

	public static void load() {
		Properties p = new Properties();
		if (Files.exists(FILE)) {
			try (Reader r = Files.newBufferedReader(FILE)) {
				p.load(r);
			} catch (IOException e) {
				StunPracticeClient.LOG.warn("Couldn't read {}", FILE, e);
			}
		}
		enabled = Boolean.parseBoolean(p.getProperty("enabled", String.valueOf(enabled)));
		delayMs = intProp(p, "delayMs", delayMs, 0, 2000);
		holdMs = intProp(p, "holdMs", holdMs, 1, 500);
		clicks = intProp(p, "clicks", clicks, 1, 10);
		gapMs = intProp(p, "gapMs", gapMs, 0, 2000);
		cooldownMs = intProp(p, "cooldownMs", cooldownMs, 0, 5000);
		save(); // writes defaults on first run / fills in missing keys
	}

	public static void save() {
		Properties p = new Properties();
		p.setProperty("enabled", String.valueOf(enabled));
		p.setProperty("delayMs", String.valueOf(delayMs));
		p.setProperty("holdMs", String.valueOf(holdMs));
		p.setProperty("clicks", String.valueOf(clicks));
		p.setProperty("gapMs", String.valueOf(gapMs));
		p.setProperty("cooldownMs", String.valueOf(cooldownMs));
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE)) {
				p.store(w, "Stun Practice - times are in milliseconds. Edit here or use /stunpractice in game.");
			}
		} catch (IOException e) {
			StunPracticeClient.LOG.warn("Couldn't write {}", FILE, e);
		}
	}

	private static int intProp(Properties p, String key, int def, int min, int max) {
		try {
			return Math.clamp(Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim()), min, max);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public static String summary() {
		return "enabled=" + enabled + ", delay=" + delayMs + "ms, hold=" + holdMs + "ms, clicks=" + clicks
				+ ", gap=" + gapMs + "ms, cooldown=" + cooldownMs + "ms";
	}
}
