package dev.stunpractice;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.IntConsumer;

public class StunPracticeClient implements ClientModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("stunpractice");

	private static volatile boolean busy;      // true while our own clicks are running
	private static volatile long lastTrigger;  // for cooldown
	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		StunPracticeConfig.load();
		OsMouse.init();

		// Hotkey to turn the mod on/off (default K, change it in Options > Controls > Key Binds > Miscellaneous).
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.stunpractice.toggle",
				InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyMapping.Category.MISC));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (toggleKey.consumeClick()) {
				StunPracticeConfig.enabled = !StunPracticeConfig.enabled;
				StunPracticeConfig.save();
				if (mc.player != null) {
					mc.player.displayClientMessage(Component.literal("Stun Practice: "
							+ (StunPracticeConfig.enabled ? "\u00a7aON" : "\u00a7cOFF")), true);
				}
			}
		});

		// Fires on the client when YOU left-click an entity (before vanilla handles the hit).
		// We only watch; we always return PASS so vanilla does its normal thing.
		AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
			Minecraft mc = Minecraft.getInstance();
			if (player != mc.player) return InteractionResult.PASS; // ignore integrated-server side
			if (!StunPracticeConfig.enabled || busy || !OsMouse.available()) return InteractionResult.PASS;
			if (!(target instanceof Player victim) || !victim.isBlocking()) return InteractionResult.PASS;
			if (!player.getMainHandItem().is(ItemTags.AXES)) return InteractionResult.PASS;

			long now = System.currentTimeMillis();
			if (now - lastTrigger < StunPracticeConfig.cooldownMs) return InteractionResult.PASS;
			lastTrigger = now;

			startAutoClicks();
			return InteractionResult.PASS;
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				ClientCommandManager.literal("stunpractice")
						.executes(ctx -> feedback(ctx, StunPracticeConfig.summary()))
						.then(ClientCommandManager.literal("toggle").executes(ctx -> {
							StunPracticeConfig.enabled = !StunPracticeConfig.enabled;
							StunPracticeConfig.save();
							return feedback(ctx, StunPracticeConfig.enabled ? "ON" : "OFF");
						}))
						.then(ClientCommandManager.literal("reload").executes(ctx -> {
							StunPracticeConfig.load();
							return feedback(ctx, "reloaded: " + StunPracticeConfig.summary());
						}))
						.then(intSetting("delay", 0, 2000, v -> StunPracticeConfig.delayMs = v))
						.then(intSetting("hold", 1, 500, v -> StunPracticeConfig.holdMs = v))
						.then(intSetting("clicks", 1, 10, v -> StunPracticeConfig.clicks = v))
						.then(intSetting("gap", 0, 2000, v -> StunPracticeConfig.gapMs = v))
						.then(intSetting("cooldown", 0, 5000, v -> StunPracticeConfig.cooldownMs = v))));
	}

	private static void startAutoClicks() {
		busy = true;
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(StunPracticeConfig.delayMs);
				for (int i = 0; i < StunPracticeConfig.clicks; i++) {
					if (i > 0) Thread.sleep(StunPracticeConfig.gapMs);
					Minecraft mc = Minecraft.getInstance();
					// Don't click into menus, chat, or other windows if you alt-tabbed/opened something.
					if (mc.screen != null || !mc.isWindowActive() || mc.player == null) break;
					OsMouse.click(StunPracticeConfig.holdMs);
				}
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			} finally {
				busy = false;
			}
		}, "stunpractice-clicker");
		t.setDaemon(true);
		t.start();
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> intSetting(String name, int min, int max, IntConsumer setter) {
		return ClientCommandManager.literal(name)
				.then(ClientCommandManager.argument("value", IntegerArgumentType.integer(min, max)).executes(ctx -> {
					int v = IntegerArgumentType.getInteger(ctx, "value");
					setter.accept(v);
					StunPracticeConfig.save();
					return feedback(ctx, name + " = " + v);
				}));
	}

	private static int feedback(CommandContext<FabricClientCommandSource> ctx, String msg) {
		ctx.getSource().sendFeedback(Component.literal("[Stun Practice] " + msg));
		return 1;
	}
}
