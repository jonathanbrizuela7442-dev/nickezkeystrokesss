package dev.nickez.keystrokes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class KeystrokesConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("nickezkeystrokes.json");
	private static KeystrokesConfig instance = new KeystrokesConfig();

	// Master + toggles
	public boolean enabled = true;
	public boolean showWasd = true;
	public boolean showSpace = true;
	public boolean showMouse = true;      // LMB / RMB buttons
	public boolean showCps = true;        // CPS inside LMB / RMB
	public boolean showTotalCps = false;  // combined CPS line under the keys
	public boolean showMovementMods = false; // Sneak / Sprint bars

	// Look
	public boolean rainbow = false;
	public boolean pressPop = true;   // keys shrink slightly when pressed
	public boolean textShadow = false;
	public boolean rounded = true;
	public int pressedColor = 0xFFFFFF;
	public int idleTextColor = 0xFFFFFF;
	public int pressedTextColor = 0x101010;
	public float opacity = 0.40f;
	public float smoothSpeed = 22f;   // higher = snappier animation

	// Layout
	public int x = 8;
	public int y = 8;
	public int keySize = 24;
	public int gap = 2;
	public float scale = 1.0f;

	public static KeystrokesConfig get() {
		return instance;
	}

	public static void load() {
		try {
			if (Files.exists(PATH)) {
				KeystrokesConfig loaded = GSON.fromJson(Files.readString(PATH), KeystrokesConfig.class);
				if (loaded != null) instance = loaded;
			}
		} catch (Exception e) {
			NickezKeystrokes.LOGGER.warn("Could not read config, using defaults", e);
		}
		instance.clamp();
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(instance));
		} catch (IOException e) {
			NickezKeystrokes.LOGGER.warn("Could not save config", e);
		}
	}

	public void reset() {
		instance = new KeystrokesConfig();
		save();
	}

	public void clamp() {
		keySize = Math.max(14, Math.min(48, keySize));
		gap = Math.max(0, Math.min(8, gap));
		scale = Math.max(0.5f, Math.min(4f, scale));
		opacity = Math.max(0f, Math.min(1f, opacity));
		smoothSpeed = Math.max(4f, Math.min(60f, smoothSpeed));
		pressedColor &= 0xFFFFFF;
		idleTextColor &= 0xFFFFFF;
		pressedTextColor &= 0xFFFFFF;
	}
}
