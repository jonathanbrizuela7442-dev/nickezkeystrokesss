package dev.nickez.keystrokes;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** In-game configuration: /nickez <option> [value] */
public final class KeystrokesCommand {
	private KeystrokesCommand() {}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			LiteralArgumentBuilder<FabricClientCommandSource> root = literal("nickez");

			root.executes(ctx -> {
				ctx.getSource().sendFeedback(Text.literal(
					"§bNickezKeystrokes §7- press N for the menu. Toggles: enabled, wasd, space, mouse, cps, totalcps, mods, rainbow, pop, shadow, rounded\n"
					+ "§7Other: pos <x> <y>, scale <0.5-4>, size <14-48>, gap <0-8>, opacity <0-1>, speed <4-60>, "
					+ "color <hex>, textcolor <hex>, pressedtext <hex>, reset"));
				return 1;
			});

			flag(root, "enabled", () -> KeystrokesConfig.get().enabled, v -> KeystrokesConfig.get().enabled = v);
			flag(root, "wasd", () -> KeystrokesConfig.get().showWasd, v -> KeystrokesConfig.get().showWasd = v);
			flag(root, "space", () -> KeystrokesConfig.get().showSpace, v -> KeystrokesConfig.get().showSpace = v);
			flag(root, "mouse", () -> KeystrokesConfig.get().showMouse, v -> KeystrokesConfig.get().showMouse = v);
			flag(root, "cps", () -> KeystrokesConfig.get().showCps, v -> KeystrokesConfig.get().showCps = v);
			flag(root, "totalcps", () -> KeystrokesConfig.get().showTotalCps, v -> KeystrokesConfig.get().showTotalCps = v);
			flag(root, "mods", () -> KeystrokesConfig.get().showMovementMods, v -> KeystrokesConfig.get().showMovementMods = v);
			flag(root, "rainbow", () -> KeystrokesConfig.get().rainbow, v -> KeystrokesConfig.get().rainbow = v);
			flag(root, "pop", () -> KeystrokesConfig.get().pressPop, v -> KeystrokesConfig.get().pressPop = v);
			flag(root, "shadow", () -> KeystrokesConfig.get().textShadow, v -> KeystrokesConfig.get().textShadow = v);
			flag(root, "rounded", () -> KeystrokesConfig.get().rounded, v -> KeystrokesConfig.get().rounded = v);

			root.then(literal("pos")
				.then(argument("x", IntegerArgumentType.integer(0, 4000))
					.then(argument("y", IntegerArgumentType.integer(0, 4000)).executes(ctx -> {
						KeystrokesConfig cfg = KeystrokesConfig.get();
						cfg.x = IntegerArgumentType.getInteger(ctx, "x");
						cfg.y = IntegerArgumentType.getInteger(ctx, "y");
						return done(ctx.getSource(), "position = " + cfg.x + ", " + cfg.y);
					}))));

			root.then(literal("scale").then(argument("value", FloatArgumentType.floatArg(0.5f, 4f)).executes(ctx -> {
				KeystrokesConfig.get().scale = FloatArgumentType.getFloat(ctx, "value");
				return done(ctx.getSource(), "scale = " + KeystrokesConfig.get().scale);
			})));
			root.then(literal("size").then(argument("value", IntegerArgumentType.integer(14, 48)).executes(ctx -> {
				KeystrokesConfig.get().keySize = IntegerArgumentType.getInteger(ctx, "value");
				return done(ctx.getSource(), "key size = " + KeystrokesConfig.get().keySize);
			})));
			root.then(literal("gap").then(argument("value", IntegerArgumentType.integer(0, 8)).executes(ctx -> {
				KeystrokesConfig.get().gap = IntegerArgumentType.getInteger(ctx, "value");
				return done(ctx.getSource(), "gap = " + KeystrokesConfig.get().gap);
			})));
			root.then(literal("opacity").then(argument("value", FloatArgumentType.floatArg(0f, 1f)).executes(ctx -> {
				KeystrokesConfig.get().opacity = FloatArgumentType.getFloat(ctx, "value");
				return done(ctx.getSource(), "opacity = " + KeystrokesConfig.get().opacity);
			})));
			root.then(literal("speed").then(argument("value", FloatArgumentType.floatArg(4f, 60f)).executes(ctx -> {
				KeystrokesConfig.get().smoothSpeed = FloatArgumentType.getFloat(ctx, "value");
				return done(ctx.getSource(), "smooth speed = " + KeystrokesConfig.get().smoothSpeed);
			})));

			colorOption(root, "color", v -> KeystrokesConfig.get().pressedColor = v);
			colorOption(root, "textcolor", v -> KeystrokesConfig.get().idleTextColor = v);
			colorOption(root, "pressedtext", v -> KeystrokesConfig.get().pressedTextColor = v);

			root.then(literal("reset").executes(ctx -> {
				KeystrokesConfig.get().reset();
				return done(ctx.getSource(), "reset to defaults");
			}));

			dispatcher.register(root);
		});
	}

	private static void flag(LiteralArgumentBuilder<FabricClientCommandSource> root, String name,
			BooleanSupplier get, Consumer<Boolean> set) {
		root.then(literal(name)
			.executes(ctx -> {
				boolean v = !get.getAsBoolean();
				set.accept(v);
				return done(ctx.getSource(), name + " = " + (v ? "ON" : "OFF"));
			})
			.then(argument("value", BoolArgumentType.bool()).executes(ctx -> {
				boolean v = BoolArgumentType.getBool(ctx, "value");
				set.accept(v);
				return done(ctx.getSource(), name + " = " + (v ? "ON" : "OFF"));
			})));
	}

	private static void colorOption(LiteralArgumentBuilder<FabricClientCommandSource> root, String name, Consumer<Integer> set) {
		root.then(literal(name).then(argument("hex", StringArgumentType.word()).executes(ctx -> {
			String s = StringArgumentType.getString(ctx, "hex").replace("#", "");
			try {
				int rgb = Integer.parseInt(s, 16) & 0xFFFFFF;
				set.accept(rgb);
				return done(ctx.getSource(), name + " = #" + String.format("%06X", rgb));
			} catch (NumberFormatException e) {
				ctx.getSource().sendFeedback(Text.literal("§cUse a hex colour like FF55AA"));
				return 0;
			}
		})));
	}

	private static int done(FabricClientCommandSource src, String msg) {
		KeystrokesConfig.get().clamp();
		KeystrokesConfig.save();
		src.sendFeedback(Text.literal("§bNickezKeystrokes §7" + msg));
		return 1;
	}
}
