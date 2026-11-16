package dev.nickez.keystrokes;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** The NickezKeystrokes customization menu (default key: N). */
public class CustomizeScreen extends Screen {
	private enum Tab {
		KEYS("Keys"), LOOK("Look"), LAYOUT("Layout"), CONTROLS("Controls");
		final String label;
		Tab(String label) { this.label = label; }
	}

	private static final String[] COLOR_NAMES = {"White", "Cyan", "Pink", "Green", "Gold", "Red", "Purple"};
	private static final int[] COLOR_VALUES = {0xFFFFFF, 0x55FFFF, 0xFF66CC, 0x55FF55, 0xFFC107, 0xFF5555, 0xAA66FF};

	private static Tab current = Tab.KEYS;

	private int px, py, pw, ph, cw, contentY;
	private boolean rebinding;
	private ButtonWidget rebindButton;

	public CustomizeScreen() {
		super(Text.literal("NickezKeystrokes"));
	}

	@Override
	protected void init() {
		KeystrokesConfig c = KeystrokesConfig.get();
		pw = Math.min(316, width - 8);
		ph = height - 20;
		px = Math.max(4, width - pw - 10);
		py = 10;
		cw = (pw - 20 - 4) / 2;
		contentY = py + 56;

		// Tabs
		int tabW = (pw - 20 - 3 * 4) / 4;
		for (int i = 0; i < Tab.values().length; i++) {
			Tab t = Tab.values()[i];
			ButtonWidget b = ButtonWidget.builder(Text.literal(t.label), btn -> {
				current = t;
				rebinding = false;
				clearAndInit();
			}).dimensions(px + 10 + i * (tabW + 4), py + 26, tabW, 20).build();
			b.active = (t != current);
			addDrawableChild(b);
		}

		int sw = client.getWindow().getScaledWidth();
		int sh = client.getWindow().getScaledHeight();

		switch (current) {
			case KEYS -> {
				toggle(0, "Keystrokes", () -> c.enabled, v -> c.enabled = v);
				toggle(1, "WASD", () -> c.showWasd, v -> c.showWasd = v);
				toggle(2, "Space Bar", () -> c.showSpace, v -> c.showSpace = v);
				toggle(3, "Mouse Buttons", () -> c.showMouse, v -> c.showMouse = v);
				toggle(4, "CPS", () -> c.showCps, v -> c.showCps = v);
				toggle(5, "Total CPS", () -> c.showTotalCps, v -> c.showTotalCps = v);
				toggle(6, "Sneak / Sprint", () -> c.showMovementMods, v -> c.showMovementMods = v);
			}
			case LOOK -> {
				toggle(0, "Rainbow", () -> c.rainbow, v -> c.rainbow = v);
				toggle(1, "Press Pop", () -> c.pressPop, v -> c.pressPop = v);
				toggle(2, "Text Shadow", () -> c.textShadow, v -> c.textShadow = v);
				toggle(3, "Rounded", () -> c.rounded, v -> c.rounded = v);
				colorCycle(4, "Press Color", () -> c.pressedColor, v -> c.pressedColor = v);
				colorCycle(5, "Text Color", () -> c.idleTextColor, v -> c.idleTextColor = v);
				slider(6, "Opacity", 0, 1, false, () -> c.opacity, v -> c.opacity = (float) v);
				slider(7, "Smoothness", 4, 60, true, () -> c.smoothSpeed, v -> c.smoothSpeed = (float) v);
			}
			case LAYOUT -> {
				slider(0, "Scale", 0.5, 4, false, () -> c.scale, v -> c.scale = (float) v);
				slider(1, "Key Size", 14, 48, true, () -> c.keySize, v -> c.keySize = (int) v);
				slider(2, "Gap", 0, 8, true, () -> c.gap, v -> c.gap = (int) v);
				slider(3, "X", 0, sw, true, () -> c.x, v -> c.x = (int) v);
				slider(4, "Y", 0, sh, true, () -> c.y, v -> c.y = (int) v);
			}
			case CONTROLS -> {
				rebindButton = ButtonWidget.builder(rebindLabel(), b -> {
					rebinding = true;
					b.setMessage(Text.literal("§ePress a key...  (Esc = cancel)"));
				}).dimensions(px + 10, contentY, pw - 20, 20).build();
				addDrawableChild(rebindButton);

				addDrawableChild(ButtonWidget.builder(Text.literal("Reset Menu Key to N"), b -> {
					NickezKeystrokes.openMenuKey.setBoundKey(NickezKeystrokes.openMenuKey.getDefaultKey());
					KeyBinding.updateKeysByCode();
					client.options.write();
					rebinding = false;
					rebindButton.setMessage(rebindLabel());
				}).dimensions(px + 10, contentY + 24, pw - 20, 20).build());

				addDrawableChild(ButtonWidget.builder(Text.literal("§cReset All Settings"), b -> {
					c.reset();
					clearAndInit();
				}).dimensions(px + 10, contentY + 56, pw - 20, 20).build());
			}
		}

		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
			.dimensions(px + 10, py + ph - 28, pw - 20, 20).build());
	}

	// ---- widget helpers -------------------------------------------------------------------

	private int colX(int idx) { return px + 10 + (idx % 2) * (cw + 4); }
	private int rowY(int idx) { return contentY + (idx / 2) * 24; }

	private void toggle(int idx, String name, BooleanSupplier get, Consumer<Boolean> set) {
		addDrawableChild(ButtonWidget.builder(toggleLabel(name, get.getAsBoolean()), b -> {
			boolean v = !get.getAsBoolean();
			set.accept(v);
			b.setMessage(toggleLabel(name, v));
		}).dimensions(colX(idx), rowY(idx), cw, 20).build());
	}

	private static Text toggleLabel(String name, boolean on) {
		return Text.literal(name + ": " + (on ? "§aON" : "§cOFF"));
	}

	private void colorCycle(int idx, String name, IntSupplier get, IntConsumer set) {
		addDrawableChild(ButtonWidget.builder(colorLabel(name, get.getAsInt()), b -> {
			int next = 0;
			for (int i = 0; i < COLOR_VALUES.length; i++) {
				if (COLOR_VALUES[i] == (get.getAsInt() & 0xFFFFFF)) { next = (i + 1) % COLOR_VALUES.length; break; }
			}
			set.accept(COLOR_VALUES[next]);
			b.setMessage(colorLabel(name, COLOR_VALUES[next]));
		}).dimensions(colX(idx), rowY(idx), cw, 20).build());
	}

	private static Text colorLabel(String name, int rgb) {
		String n = "Custom";
		for (int i = 0; i < COLOR_VALUES.length; i++) if (COLOR_VALUES[i] == (rgb & 0xFFFFFF)) n = COLOR_NAMES[i];
		return Text.literal(name + ": ").append(Text.literal(n).withColor(rgb & 0xFFFFFF));
	}

	private void slider(int idx, String name, double min, double max, boolean integer,
			DoubleSupplier get, DoubleConsumer set) {
		addDrawableChild(new ConfigSlider(colX(idx), rowY(idx), cw, 20, name, min, max, integer, get, set));
	}

	private Text rebindLabel() {
		return Text.literal("Menu Key: §b[" + NickezKeystrokes.openMenuKey.getBoundKeyLocalizedText().getString() + "]§r  (click to change)");
	}

	// ---- input ----------------------------------------------------------------------------

	@Override
	public boolean keyPressed(KeyInput input) {
		if (rebinding) {
			int key = input.key();
			rebinding = false;
			if (key != GLFW.GLFW_KEY_ESCAPE) {
				NickezKeystrokes.openMenuKey.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(key));
				KeyBinding.updateKeysByCode();
				client.options.write();
			}
			if (rebindButton != null) rebindButton.setMessage(rebindLabel());
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public void removed() {
		KeystrokesConfig.get().clamp();
		KeystrokesConfig.save();
	}

	// ---- drawing --------------------------------------------------------------------------

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);

		// Panel
		ctx.fill(px, py, px + pw, py + ph, 0xD0101018);
		ctx.fill(px, py, px + pw, py + 1, 0x80FFFFFF);
		ctx.fill(px, py + ph - 1, px + pw, py + ph, 0x80FFFFFF);
		ctx.fill(px, py, px + 1, py + ph, 0x80FFFFFF);
		ctx.fill(px + pw - 1, py, px + pw, py + ph, 0x80FFFFFF);

		// Animated rainbow accent bar
		long t = System.currentTimeMillis();
		for (int x = 0; x < pw; x += 4) {
			int rgb = MathHelper.hsvToRgb(((t % 5000L) / 5000f + x / (float) pw) % 1f, 0.6f, 1f);
			ctx.fill(px + x, py + 1, Math.min(px + x + 4, px + pw), py + 4, 0xFF000000 | rgb);
		}
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);

		String title = "NickezKeystrokes";
		ctx.drawText(textRenderer, title, px + (pw - textRenderer.getWidth(title)) / 2, py + 10, 0xFFFFFFFF, true);

		// Live, animated preview of the HUD with the current settings.
		NickezKeystrokes.HUD.renderPreview(ctx, client);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	// ---- slider ---------------------------------------------------------------------------

	private static final class ConfigSlider extends SliderWidget {
		private final String name;
		private final double min, max;
		private final boolean integer;
		private final DoubleConsumer set;

		ConfigSlider(int x, int y, int w, int h, String name, double min, double max, boolean integer,
				DoubleSupplier get, DoubleConsumer set) {
			super(x, y, w, h, Text.empty(), (MathHelper.clamp(get.getAsDouble(), min, max) - min) / (max - min));
			this.name = name;
			this.min = min;
			this.max = max;
			this.integer = integer;
			this.set = set;
			updateMessage();
		}

		private double actual() {
			double v = min + value * (max - min);
			return integer ? Math.round(v) : v;
		}

		@Override
		protected void updateMessage() {
			double v = actual();
			setMessage(Text.literal(name + ": " + (integer ? String.valueOf((long) v) : String.format("%.2f", v))));
		}

		@Override
		protected void applyValue() {
			set.accept(actual());
		}
	}
}
