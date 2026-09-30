package com.vildoria.client;

import com.vildoria.Vildoria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class NotesSettings {
	private static final int MIN_SIZE = 320;
	private static final int MAX_SIZE = 960;
	private static final int SIZE_STEP = 32;
	private static int width = 512;
	private static int height = 512;

	private NotesSettings() {
	}

	static void load() {
		Properties properties = new Properties();
		Path path = settingsPath();
		if (!Files.exists(path)) {
			return;
		}
		try (var input = Files.newInputStream(path)) {
			properties.load(input);
			width = clamp(parseSize(properties.getProperty("width"), width));
			height = clamp(parseSize(properties.getProperty("height"), height));
		} catch (IOException exception) {
			Vildoria.LOGGER.error("Could not load notes settings", exception);
		}
	}

	static int width() {
		return width;
	}

	static int height() {
		return height;
	}

	static int effectiveWidth(int screenWidth) {
		return Math.max(1, Math.min(width, screenWidth - 16));
	}

	static int effectiveHeight(int screenHeight) {
		return Math.max(1, Math.min(height, screenHeight - 16));
	}

	static void increaseWidth() {
		width = nextSize(width);
		save();
	}

	static void increaseHeight() {
		height = nextSize(height);
		save();
	}

	private static int nextSize(int value) {
		return value >= MAX_SIZE ? MIN_SIZE : value + SIZE_STEP;
	}

	private static int parseSize(String value, int fallback) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private static int clamp(int value) {
		return Math.max(MIN_SIZE, Math.min(MAX_SIZE, value));
	}

	private static void save() {
		Properties properties = new Properties();
		properties.setProperty("width", Integer.toString(width));
		properties.setProperty("height", Integer.toString(height));
		Path path = settingsPath();
		try {
			Files.createDirectories(path.getParent());
			try (var output = Files.newOutputStream(path)) {
				properties.store(output, "Vildoria notes settings");
			}
		} catch (IOException exception) {
			Vildoria.LOGGER.error("Could not save notes settings", exception);
		}
	}

	private static Path settingsPath() {
		return net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath()
				.resolve("config").resolve("vildoria").resolve("settings.properties");
	}
}