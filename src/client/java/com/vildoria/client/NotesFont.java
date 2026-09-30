package com.vildoria.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

final class NotesFont {
	private static final Identifier MONOSPACE = Identifier.fromNamespaceAndPath("minecraft", "uniform");

	private NotesFont() {
	}

	static Component apply(Component text) {
		FontDescription font = new FontDescription.Resource(MONOSPACE);
		return text.copy().withStyle(style -> style.withFont(font));
	}

	static Component literal(String text) {
		return apply(Component.literal(text));
	}

	static FormattedCharSequence format(String text, int cursorPosition) {
		return literal(text).getVisualOrderText();
	}
}