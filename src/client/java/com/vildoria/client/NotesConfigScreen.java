package com.vildoria.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotesConfigScreen extends Screen {
	private final Screen parent;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;

	public NotesConfigScreen(Screen parent) {
		super(Component.translatable("config.vildoria.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		panelWidth = Math.min(680, width - 28);
		panelHeight = Math.min(500, height - 28);
		panelLeft = (width - panelWidth) / 2;
		panelTop = (height - panelHeight) / 2;

		addSizeControls(panelTop + 108, true);
		addSizeControls(panelTop + 144, false);
		addOptionButton(panelTop + 214, Component.translatable("config.vildoria.theme_label"),
				Component.translatable(NotesSettings.themeKey()), NotesSettings::cycleTheme);
		addOptionButton(panelTop + 250, Component.translatable("config.vildoria.font_label"),
				Component.translatable(NotesSettings.fontKey()), NotesSettings::cycleFont);
		addOptionButton(panelTop + 286, Component.translatable("config.vildoria.density_label"),
				Component.translatable(NotesSettings.densityKey()), NotesSettings::cycleDensity);
		addOptionButton(panelTop + 322, Component.translatable("config.vildoria.text_label"),
				Component.translatable(NotesSettings.textColorKey()), NotesSettings::cycleTextColor);
		addOptionButton(panelTop + 358, Component.translatable("config.vildoria.grid_label"),
				gridLabel(), NotesSettings::toggleGridLines);

		addRenderableWidget(Button.builder(NotesFont.apply(Component.translatable("config.vildoria.reset")),
				button -> {
					NotesSettings.reset();
					rebuildWidgets();
				}).bounds(panelLeft + 22, panelTop + panelHeight - 38, 132, 22).build());
		addRenderableWidget(Button.builder(NotesFont.apply(Component.translatable("config.vildoria.done")),
				button -> minecraft.setScreenAndShow(parent))
				.bounds(panelLeft + panelWidth - 154, panelTop + panelHeight - 38, 132, 22).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		NotesSettings.Palette palette = NotesSettings.palette();
		graphics.fill(0, 0, width, height, palette.background());
		graphics.fill(panelLeft - 6, panelTop - 6, panelLeft + panelWidth + 6, panelTop + panelHeight + 6, palette.panel());
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, palette.panel());
		graphics.outline(panelLeft, panelTop, panelWidth, panelHeight, palette.border());
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 62, palette.header());
		graphics.fill(panelLeft, panelTop + 58, panelLeft + panelWidth, panelTop + 61, palette.accent());
		graphics.text(font, NotesFont.apply(title), panelLeft + 22, panelTop + 15, 0xffffffff);
		graphics.text(font, NotesFont.apply(Component.translatable("config.vildoria.subtitle")),
				panelLeft + 22, panelTop + 34, 0xffd4e5dc);

		drawSection(graphics, "config.vildoria.layout", panelTop + 84, palette);
		drawSettingLabel(graphics, "config.vildoria.width_label", panelTop + 113, palette);
		drawSettingLabel(graphics, "config.vildoria.height_label", panelTop + 148, palette);
		drawSection(graphics, "config.vildoria.appearance", panelTop + 196, palette);
		drawSettingLabel(graphics, "config.vildoria.theme_label", panelTop + 214, palette);
		drawSettingLabel(graphics, "config.vildoria.font_label", panelTop + 250, palette);
		drawSettingLabel(graphics, "config.vildoria.density_label", panelTop + 286, palette);
		drawSettingLabel(graphics, "config.vildoria.text_label", panelTop + 322, palette);
		drawSettingLabel(graphics, "config.vildoria.grid_label", panelTop + 358, palette);

		graphics.text(font, NotesFont.apply(Component.translatable("config.vildoria.key",
				VildoriaClient.getOpenNotesKey().getTranslatedKeyMessage())),
				panelLeft + 22, panelTop + panelHeight - 61, palette.muted());
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private void addSizeControls(int y, boolean horizontal) {
		int controlWidth = 30;
		int valueWidth = 92;
		int gap = 4;
		int x = panelLeft + panelWidth - 22 - (controlWidth * 2 + valueWidth + gap * 2);
		Button[] buttons = new Button[2];
		Button valueButton = Button.builder(NotesFont.apply(sizeLabel(horizontal)), button -> {
			if (horizontal) {
				NotesSettings.increaseWidth();
			} else {
				NotesSettings.increaseHeight();
			}
			updateSizeMessage(horizontal, buttons[0], buttons[1]);
		}).bounds(x + controlWidth + gap, y, valueWidth, 22).build();
		Button minusButton = Button.builder(NotesFont.literal("-"), button -> {
			if (horizontal) {
				NotesSettings.decreaseWidth();
			} else {
				NotesSettings.decreaseHeight();
			}
			updateSizeMessage(horizontal, buttons[0], buttons[1]);
		}).bounds(x, y, controlWidth, 22).build();
		Button plusButton = Button.builder(NotesFont.literal("+"), button -> {
			if (horizontal) {
				NotesSettings.increaseWidth();
			} else {
				NotesSettings.increaseHeight();
			}
			updateSizeMessage(horizontal, buttons[0], buttons[1]);
		}).bounds(x + controlWidth + gap + valueWidth + gap, y, controlWidth, 22).build();
		buttons[0] = minusButton;
		buttons[1] = valueButton;
		addRenderableWidget(minusButton);
		addRenderableWidget(valueButton);
		addRenderableWidget(plusButton);
	}

	private void updateSizeMessage(boolean horizontal, Button minusButton, Button valueButton) {
		if (minusButton != null) {
			minusButton.setMessage(NotesFont.literal("-"));
		}
		if (valueButton != null) {
			valueButton.setMessage(NotesFont.apply(sizeLabel(horizontal)));
		}
	}

	private Component sizeLabel(boolean horizontal) {
		int size = horizontal ? NotesSettings.width() : NotesSettings.height();
		return Component.translatable("config.vildoria.value_px", size);
	}

	private void addOptionButton(int y, Component label, Component value, Runnable action) {
		int buttonWidth = Math.min(220, panelWidth - 210);
		int x = panelLeft + panelWidth - buttonWidth - 22;
		if (label != null) {
			// kept for readability; the button itself is the interactive value selector
		}
		addRenderableWidget(Button.builder(NotesFont.apply(value), button -> {
			action.run();
			button.setMessage(NotesFont.apply(currentValue(y)));
		}).bounds(x, y, buttonWidth, 22).build());
	}

	private Component currentValue(int y) {
		if (y == panelTop + 214) {
			return Component.translatable(NotesSettings.themeKey());
		}
		if (y == panelTop + 250) {
			return Component.translatable(NotesSettings.fontKey());
		}
		if (y == panelTop + 286) {
			return Component.translatable(NotesSettings.densityKey());
		}
		if (y == panelTop + 322) {
			return Component.translatable(NotesSettings.textColorKey());
		}
		return gridLabel();
	}

	private Component gridLabel() {
		return Component.translatable(NotesSettings.gridLines() ? "config.vildoria.grid.on" : "config.vildoria.grid.off");
	}

	private void drawSection(GuiGraphicsExtractor graphics, String key, int y, NotesSettings.Palette palette) {
		graphics.text(font, NotesFont.apply(Component.translatable(key)), panelLeft + 22, y, palette.accent());
		graphics.fill(panelLeft + 22, y + 13, panelLeft + panelWidth - 22, y + 14, palette.border());
	}

	private void drawSettingLabel(GuiGraphicsExtractor graphics, String key, int y,
				      NotesSettings.Palette palette) {
		graphics.text(font, NotesFont.apply(Component.translatable(key)), panelLeft + 22, y, palette.text());
	}
}