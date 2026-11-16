package dev.nickez.keystrokes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

public final class KeystrokesHud {
	private static final int W = 0, A = 1, S = 2, D = 3, SPACE = 4, LMB = 5, RMB = 6, SNEAK = 7, SPRINT = 8;

	private final float[] anim = new float[9];
	private final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
	private final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
	private boolean prevLeft, prevRight;
	private long lastNanos = System.nanoTime();

	/** Normal in-game HUD rendering (skipped while the customization menu draws its own preview). */
	public void render(DrawContext ctx, MinecraftClient mc) {
		if (mc.currentScreen instanceof CustomizeScreen) return;
		draw(ctx, mc, false);
	}

	/** Animated demo used by the customization menu. */
	public void renderPreview(DrawContext ctx, MinecraftClient mc) {
		draw(ctx, mc, true);
	}

	private void draw(DrawContext ctx, MinecraftClient mc, boolean preview) {
		KeystrokesConfig c = KeystrokesConfig.get();

		long now = System.nanoTime();
		float dt = Math.min((now - lastNanos) / 1_000_000_000f, 0.1f);
		lastNanos = now;

		if (!preview && (!c.enabled || mc.player == null || mc.options.hudHidden)) return;

		boolean[] down = new boolean[9];
		if (preview) {
			long phase = System.currentTimeMillis() / 450;
			for (int i = 0; i < down.length; i++) down[i] = ((phase + i * 2L) % 9) < 2;
		} else {
			pollMouse(mc);
			down[W] = mc.options.forwardKey.isPressed();
			down[A] = mc.options.leftKey.isPressed();
			down[S] = mc.options.backKey.isPressed();
			down[D] = mc.options.rightKey.isPressed();
			down[SPACE] = mc.options.jumpKey.isPressed();
			down[LMB] = prevLeft;
			down[RMB] = prevRight;
			down[SNEAK] = mc.options.sneakKey.isPressed();
			down[SPRINT] = mc.options.sprintKey.isPressed() || mc.player.isSprinting();
		}

		// Exponential smoothing: fast on press, slightly slower fade-out on release.
		for (int i = 0; i < anim.length; i++) {
			float target = down[i] ? 1f : 0f;
			float speed = down[i] ? c.smoothSpeed : c.smoothSpeed * 0.6f;
			anim[i] += (target - anim[i]) * (1f - (float) Math.exp(-speed * dt));
		}

		TextRenderer tr = mc.textRenderer;
		int k = c.keySize, g = c.gap;
		int totalW = 3 * k + 2 * g;
		int halfW = (totalW - g) / 2;

		ctx.getMatrices().pushMatrix();
		ctx.getMatrices().translate(c.x, c.y);
		ctx.getMatrices().scale(c.scale, c.scale);

		int cy = 0;
		if (c.showWasd) {
			key(ctx, tr, c, k + g, cy, k, k, anim[W], "W", null);
			cy += k + g;
			key(ctx, tr, c, 0, cy, k, k, anim[A], "A", null);
			key(ctx, tr, c, k + g, cy, k, k, anim[S], "S", null);
			key(ctx, tr, c, 2 * (k + g), cy, k, k, anim[D], "D", null);
			cy += k + g;
		}
		if (c.showMouse) {
			int lc = preview ? 9 : cps(leftClicks, now), rc = preview ? 6 : cps(rightClicks, now);
			key(ctx, tr, c, 0, cy, halfW, k, anim[LMB], "LMB", c.showCps ? lc + " CPS" : null);
			key(ctx, tr, c, halfW + g, cy, totalW - halfW - g, k, anim[RMB], "RMB", c.showCps ? rc + " CPS" : null);
			cy += k + g;
		}
		if (c.showSpace) {
			int h = Math.max(12, (int) (k * 0.6f));
			key(ctx, tr, c, 0, cy, totalW, h, anim[SPACE], null, null);
			cy += h + g;
		}
		if (c.showMovementMods) {
			int h = Math.max(12, (int) (k * 0.6f));
			key(ctx, tr, c, 0, cy, halfW, h, anim[SNEAK], "Sneak", null);
			key(ctx, tr, c, halfW + g, cy, totalW - halfW - g, h, anim[SPRINT], "Sprint", null);
			cy += h + g;
		}
		if (c.showTotalCps) {
			int total = preview ? 15 : cps(leftClicks, now) + cps(rightClicks, now);
			String t = "CPS: " + total;
			ctx.drawText(tr, t, (totalW - tr.getWidth(t)) / 2, cy + 2, 0xFFFFFFFF, true);
		}

		ctx.getMatrices().popMatrix();
	}

	private void key(DrawContext ctx, TextRenderer tr, KeystrokesConfig c,
			int x, int y, int w, int h, float a, String label, String sub) {
		float cx = x + w / 2f, cyy = y + h / 2f;
		boolean pop = c.pressPop;
		if (pop) {
			float s = 1f - 0.07f * a;
			ctx.getMatrices().pushMatrix();
			ctx.getMatrices().translate(cx, cyy);
			ctx.getMatrices().scale(s, s);
			ctx.getMatrices().translate(-cx, -cyy);
		}

		int pressedRgb = c.rainbow
			? MathHelper.hsvToRgb(((System.currentTimeMillis() % 4000L) / 4000f + x * 0.004f) % 1f, 0.65f, 1f)
			: c.pressedColor;
		int idle = ((int) (c.opacity * 255) << 24);
		int pressed = ((int) (Math.min(1f, c.opacity + 0.45f) * 255) << 24) | (pressedRgb & 0xFFFFFF);
		int bg = lerp(idle, pressed, a);
		int fg = lerp(0xFF000000 | c.idleTextColor, 0xFF000000 | c.pressedTextColor, a);

		if (c.rounded && w > 4 && h > 4) {
			ctx.fill(x + 1, y, x + w - 1, y + h, bg);
			ctx.fill(x, y + 1, x + w, y + h - 1, bg);
		} else {
			ctx.fill(x, y, x + w, y + h, bg);
		}

		if (label == null) {
			// Space bar: draw a centered line
			ctx.fill(x + w / 4, (int) cyy, x + w - w / 4, (int) cyy + 1, fg);
		} else if (sub == null) {
			ctx.drawText(tr, label, (int) (cx - tr.getWidth(label) / 2f), (int) (cyy - 4), fg, c.textShadow);
		} else {
			int ty = y + (h - 17) / 2;
			ctx.drawText(tr, label, (int) (cx - tr.getWidth(label) / 2f), ty, fg, c.textShadow);
			ctx.drawText(tr, sub, (int) (cx - tr.getWidth(sub) / 2f), ty + 9, fg, c.textShadow);
		}

		if (pop) ctx.getMatrices().popMatrix();
	}

	/** Polls the physical mouse buttons directly so clicks are counted even when no keybind is involved. */
	private void pollMouse(MinecraftClient mc) {
		long window = mc.getWindow().getHandle();
		boolean inGame = mc.currentScreen == null;
		boolean left = inGame && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean right = inGame && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		long ms = System.nanoTime();
		if (left && !prevLeft) leftClicks.addLast(ms);
		if (right && !prevRight) rightClicks.addLast(ms);
		prevLeft = left;
		prevRight = right;
	}

	private static int cps(ArrayDeque<Long> clicks, long now) {
		while (!clicks.isEmpty() && now - clicks.peekFirst() > 1_000_000_000L) clicks.removeFirst();
		return clicks.size();
	}

	private static int lerp(int a, int b, float t) {
		int aa = lerpC(a >>> 24, b >>> 24, t);
		int rr = lerpC((a >> 16) & 255, (b >> 16) & 255, t);
		int gg = lerpC((a >> 8) & 255, (b >> 8) & 255, t);
		int bb = lerpC(a & 255, b & 255, t);
		return (aa << 24) | (rr << 16) | (gg << 8) | bb;
	}

	private static int lerpC(int a, int b, float t) {
		return Math.max(0, Math.min(255, Math.round(a + (b - a) * t)));
	}
}
