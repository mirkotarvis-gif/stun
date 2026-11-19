package dev.stunpractice;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Settings screen opened from Mod Menu. */
public class StunPracticeScreen extends Screen {
	private record Field(EditBox box, int min, int max, IntConsumer setter) {}

	private final Screen parent;
	private final List<Field> fields = new ArrayList<>();
	private boolean enabled;

	public StunPracticeScreen(Screen parent) {
		super(Component.literal("Stun Practice"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		fields.clear();
		enabled = StunPracticeConfig.enabled;
		int cx = this.width / 2;
		int y = 30;

		addRenderableWidget(new StringWidget(0, 10, this.width, 12, this.title, this.font));

		addRenderableWidget(Button.builder(enabledText(), b -> {
			enabled = !enabled;
			b.setMessage(enabledText());
		}).bounds(cx - 100, y, 200, 20).build());
		y += 28;

		y = row("2nd click delay (ms)", 0, 2000, () -> StunPracticeConfig.delayMs, v -> StunPracticeConfig.delayMs = v, cx, y);
		y = row("Click hold (ms)", 1, 500, () -> StunPracticeConfig.holdMs, v -> StunPracticeConfig.holdMs = v, cx, y);
		y = row("Auto clicks", 1, 10, () -> StunPracticeConfig.clicks, v -> StunPracticeConfig.clicks = v, cx, y);
		y = row("Gap between clicks (ms)", 0, 2000, () -> StunPracticeConfig.gapMs, v -> StunPracticeConfig.gapMs = v, cx, y);
		y = row("Trigger cooldown (ms)", 0, 5000, () -> StunPracticeConfig.cooldownMs, v -> StunPracticeConfig.cooldownMs = v, cx, y);

		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
				.bounds(cx - 100, Math.max(y + 8, this.height - 28), 200, 20).build());
	}

	private int row(String label, int min, int max, IntSupplier getter, IntConsumer setter, int cx, int y) {
		addRenderableWidget(new StringWidget(cx - 150, y + 6, 140, 9, Component.literal(label), this.font));
		EditBox box = new EditBox(this.font, cx + 10, y, 80, 20, Component.literal(label));
		box.setFilter(s -> s.isEmpty() || s.matches("\\d{1,5}"));
		box.setValue(String.valueOf(getter.getAsInt()));
		addRenderableWidget(box);
		fields.add(new Field(box, min, max, setter));
		return y + 24;
	}

	private Component enabledText() {
		return Component.literal("Enabled: " + (enabled ? "ON" : "OFF"));
	}

	@Override
	public void onClose() {
		StunPracticeConfig.enabled = enabled;
		for (Field f : fields) {
			try {
				f.setter().accept(Math.clamp(Integer.parseInt(f.box().getValue()), f.min(), f.max()));
			} catch (NumberFormatException ignored) {
				// empty box: keep the old value
			}
		}
		StunPracticeConfig.save();
		this.minecraft.setScreen(parent);
	}
}
