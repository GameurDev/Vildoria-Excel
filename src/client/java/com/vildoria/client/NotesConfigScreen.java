package com.vildoria.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotesConfigScreen extends Screen {
	private final Screen parent;

	public NotesConfigScreen(Screen parent) {
		super(Component.translatable("config.vildoria.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int buttonWidth = Math.min(280, width - 32);
		int left = (width - buttonWidth) / 2;
		int top = height / 2 - 44;
		addRenderableWidget(Button.builder(NotesFont.apply(widthLabel()), button -> {
			NotesSettings.increaseWidth();
			button.setMessage(NotesFont.apply(widthLabel()));
		}).bounds(left, top, buttonWidth, 22).build());
		addRenderableWidget(Button.builder(NotesFont.apply(heightLabel()), button -> {
			NotesSettings.increaseHeight();
			button.setMessage(NotesFont.apply(heightLabel()));
		}).bounds(left, top + 30, buttonWidth, 22).build());
		addRenderableWidget(Button.builder(NotesFont.apply(Component.translatable("config.vildoria.done")),
				button -> minecraft.setScreenAndShow(parent))
				.bounds(left, top + 78, buttonWidth, 22).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0xffe8ece8);
		graphics.fill(0, 0, width, 38, 0xff176b52);
		graphics.text(font, NotesFont.apply(title), 20, 14, 0xffffffff);
		graphics.centeredText(font, NotesFont.apply(Component.translatable("config.vildoria.key",
				VildoriaClient.getOpenNotesKey().getTranslatedKeyMessage())), width / 2, height / 2 + 47, 0xff263a31);
		graphics.centeredText(font, NotesFont.apply(Component.translatable("config.vildoria.key_hint")),
				width / 2, height / 2 + 64, 0xff52645a);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private Component widthLabel() {
		return NotesFont.apply(Component.translatable("config.vildoria.width", NotesSettings.width()));
	}

	private Component heightLabel() {
		return NotesFont.apply(Component.translatable("config.vildoria.height", NotesSettings.height()));
	}
}