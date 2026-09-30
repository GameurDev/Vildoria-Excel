package com.vildoria.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class VildoriaClient implements ClientModInitializer {
	private static KeyMapping openNotesKey;

	@Override
	public void onInitializeClient() {
		NotesSettings.load();
		openNotesKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vildoria.open_notes",
				GLFW.GLFW_KEY_N,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath("vildoria", "general"))
		));
		
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openNotesKey.consumeClick()) {
				client.setScreenAndShow(new NotesScreen());
			}
		});
	}

	public static KeyMapping getOpenNotesKey() {
		return openNotesKey;
	}
}