package dev.nickez.keystrokes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NickezKeystrokes implements ClientModInitializer {
	public static final String MOD_ID = "nickezkeystrokes";
	public static final Logger LOGGER = LoggerFactory.getLogger("NickezKeystrokes");

	public static final KeystrokesHud HUD = new KeystrokesHud();
	public static KeyBinding openMenuKey;

	@Override
	public void onInitializeClient() {
		KeystrokesConfig.load();

		HudElementRegistry.addLast(
			Identifier.of(MOD_ID, "keystrokes"),
			(context, tickCounter) -> HUD.render(context, MinecraftClient.getInstance())
		);

		// Default key: N. Rebind in Options > Controls > Key Binds, or inside the menu itself (Controls tab).
		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key." + MOD_ID + ".open_menu",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_N,
			category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.wasPressed()) {
				if (client.player != null && client.currentScreen == null) {
					client.setScreen(new CustomizeScreen());
				}
			}
		});

		KeystrokesCommand.register();
		LOGGER.info("NickezKeystrokes loaded. Press N (rebindable) to open the menu.");
	}
}
