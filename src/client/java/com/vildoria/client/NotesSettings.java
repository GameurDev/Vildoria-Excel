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
	private static int theme;
	private static int font;
	private static int density = 1;
	private static int textColor = 0;
	private static boolean gridLines = true;

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
			theme = clampIndex(parseSize(properties.getProperty("theme"), theme), 4);
			font = clampIndex(parseSize(properties.getProperty("font"), font), 4);
			density = clampIndex(parseSize(properties.getProperty("density"), density), 3);
			textColor = clampIndex(parseSize(properties.getProperty("textColor"), textColor), 4);
			gridLines = Boolean.parseBoolean(properties.getProperty("gridLines", Boolean.toString(gridLines)));
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

	static int density() {
		return density;
	}

	static int textColor() {
		return textColor;
	}

	static int textColorValue() {
		return switch (textColor) {
			case 1 -> 0xffeaf7ff;
			case 2 -> 0xfff4e5b6;
			case 3 -> 0xffd8ffe7;
			default -> 0xff1d2f2d;
		};
	}

	static Palette palette() {
		return switch (theme) {
			case 1 -> new Palette(0xff0b1116, 0xff152331, 0xff1d2f3a, 0xff41d0b9,
					0xffeaf6f5, 0xff8b9ea4, 0xff243946, 0xff1b2d39, 0xff2a655e,
					0xff425b67, 0xff2d4655);
			case 2 -> new Palette(0xffe7e0d2, 0xfff7f2e8, 0xff3a4f45, 0xffbf6a44,
					0xff2d2926, 0xff736d65, 0xffefe5d4, 0xfffffdf8, 0xfff8d7a8,
					0xffd0c3b3, 0xfff4ecd9);
			case 3 -> new Palette(0xff120f1d, 0xff1d1a2d, 0xff2b2540, 0xff7a5cff,
					0xffefeaff, 0xffb3a9d9, 0xff312863, 0xff221c39, 0xff7c6fff,
					0xff4a3f73, 0xff3d3570);
			default -> new Palette(0xff111b1a, 0xffedf5f0, 0xff1c4a3f, 0xff36c68c,
					0xff1c2f2b, 0xff6d7d75, 0xffdfeae4, 0xfff7fbfa, 0xffd8f2e8,
					0xffadc2b8, 0xffe4efe9);
		};
	}

	static int font() {
		return font;
	}

	static void cycleFont() {
		font = (font + 1) % 4;
		save();
	}

	static void cycleTheme() {
		theme = (theme + 1) % 4;
		save();
	}

	static void cycleDensity() {
		density = (density + 1) % 3;
		save();
	}

	static void cycleTextColor() {
		textColor = (textColor + 1) % 4;
		save();
	}

	static String themeKey() {
		return switch (theme) {
			case 1 -> "config.vildoria.theme.midnight";
			case 2 -> "config.vildoria.theme.paper";
			case 3 -> "config.vildoria.theme.neon";
			default -> "config.vildoria.theme.forest";
		};
	}

	static String fontKey() {
		return switch (font) {
			case 1 -> "config.vildoria.font.uniform";
			case 2 -> "config.vildoria.font.alt";
			case 3 -> "config.vildoria.font.mono";
			default -> "config.vildoria.font.default";
		};
	}

	static String densityKey() {
		return switch (density) {
			case 0 -> "config.vildoria.density.compact";
			case 2 -> "config.vildoria.density.spacious";
			default -> "config.vildoria.density.standard";
		};
	}

	static String textColorKey() {
		return switch (textColor) {
			case 1 -> "config.vildoria.text.blue";
			case 2 -> "config.vildoria.text.gold";
			case 3 -> "config.vildoria.text.green";
			default -> "config.vildoria.text.default";
		};
	}

	static boolean gridLines() {
		return gridLines;
	}

	static void toggleGridLines() {
		gridLines = !gridLines;
		save();
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

	static void decreaseWidth() {
		width = previousSize(width);
		save();
	}

	static void increaseHeight() {
		height = nextSize(height);
		save();
	}

	static void decreaseHeight() {
		height = previousSize(height);
		save();
	}

	private static int nextSize(int value) {
		return value >= MAX_SIZE ? MIN_SIZE : value + SIZE_STEP;
	}

	private static int previousSize(int value) {
		return value <= MIN_SIZE ? MAX_SIZE : value - SIZE_STEP;
	}

	static void reset() {
		width = 512;
		height = 512;
		theme = 0;
		font = 0;
		density = 1;
		textColor = 0;
		gridLines = true;
		save();
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

	private static int clampIndex(int value, int count) {
		return Math.max(0, Math.min(count - 1, value));
	}

	private static void save() {
		Properties properties = new Properties();
		properties.setProperty("width", Integer.toString(width));
		properties.setProperty("height", Integer.toString(height));
		properties.setProperty("theme", Integer.toString(theme));
		properties.setProperty("font", Integer.toString(font));
		properties.setProperty("density", Integer.toString(density));
		properties.setProperty("textColor", Integer.toString(textColor));
		properties.setProperty("gridLines", Boolean.toString(gridLines));
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

	record Palette(int background, int panel, int header, int accent, int text, int muted,
		       int gridHeader, int cell, int selected, int border, int button) {
	}
}