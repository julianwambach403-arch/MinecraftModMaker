package com.modmaker.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Shared plumbing for all ModMaker screens: parent navigation, simple text labels
 * (drawn with the 26.2 {@link GuiGraphicsExtractor} pipeline) and widget helpers.
 */
public abstract class BaseScreen extends Screen {
	protected static final int WHITE = 0xFFFFFFFF;
	protected static final int GRAY = 0xFFAAAAAA;
	protected static final int RED = 0xFFFF6666;
	protected static final int GREEN = 0xFF7FE87F;

	protected final Screen parent;
	private final List<Label> labels = new ArrayList<>();

	private record Label(Component text, int x, int y, int color) {
	}

	protected BaseScreen(Component title, Screen parent) {
		super(title);
		this.parent = parent;
	}

	@Override
	protected void init() {
		labels.clear();
	}

	protected void label(Component text, int x, int y) {
		labels.add(new Label(text, x, y, GRAY));
	}

	protected void label(Component text, int x, int y, int color) {
		labels.add(new Label(text, x, y, color));
	}

	protected Button addButton(int x, int y, int width, int height, Component text, Runnable action) {
		return addRenderableWidget(Button.builder(text, button -> action.run()).bounds(x, y, width, height).build());
	}

	protected EditBox addEditBox(int x, int y, int width, int height, String value, Component message) {
		EditBox box = new EditBox(font, x, y, width, height, message);
		box.setMaxLength(500);
		box.setValue(value == null ? "" : value);
		return addRenderableWidget(box);
	}

	/**
	 * A simple "cycle through values" button (replacement for CycleButton with less API surface).
	 * The label always shows {@code prefix: valueLabel}.
	 */
	protected <T> Button addCycleButton(int x, int y, int width, int height, Component prefix,
			List<T> values, T initial, Function<T, Component> labelFn, Consumer<T> onChange) {
		int startIndex = Math.max(0, values.indexOf(initial));
		int[] index = {startIndex};
		Button button = Button.builder(cycleLabel(prefix, labelFn.apply(values.get(startIndex))), b -> {
			index[0] = (index[0] + 1) % values.size();
			T value = values.get(index[0]);
			b.setMessage(cycleLabel(prefix, labelFn.apply(value)));
			onChange.accept(value);
		}).bounds(x, y, width, height).build();
		return addRenderableWidget(button);
	}

	private static Component cycleLabel(Component prefix, Component value) {
		return Component.empty().append(prefix).append(": ").append(value);
	}

	/** Toggle button flipping between on/off labels. */
	protected Button addToggleButton(int x, int y, int width, int height, Component prefix,
			boolean initial, Consumer<Boolean> onChange) {
		boolean[] state = {initial};
		Button button = Button.builder(toggleLabel(prefix, initial), b -> {
			state[0] = !state[0];
			b.setMessage(toggleLabel(prefix, state[0]));
			onChange.accept(state[0]);
		}).bounds(x, y, width, height).build();
		return addRenderableWidget(button);
	}

	private static Component toggleLabel(Component prefix, boolean on) {
		return Component.empty().append(prefix).append(": ")
				.append(Component.translatable(on ? "modmaker.gui.on" : "modmaker.gui.off"));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 12, WHITE);
		for (Label label : labels) {
			graphics.text(font, label.text(), label.x(), label.y(), label.color());
		}
		extractExtra(graphics, mouseX, mouseY, partialTick);
	}

	/** Extra per-screen drawing (item icons, texture previews, ...). */
	protected void extractExtra(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void onClose() {
		assert minecraft != null;
		minecraft.gui.setScreen(parent);
	}

	protected void open(Screen screen) {
		assert minecraft != null;
		minecraft.gui.setScreen(screen);
	}

	protected static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	protected static float parseFloat(String value, float fallback) {
		try {
			return Float.parseFloat(value.trim().replace(',', '.'));
		} catch (Exception e) {
			return fallback;
		}
	}
}
