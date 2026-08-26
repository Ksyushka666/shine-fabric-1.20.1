package com.bloom.client;

import com.bloom.BloomMod;
import com.bloom.client.config.BloomConfig;
import com.bloom.client.config.ExperimentalConfigManager;
import com.bloom.client.render.BloomPostProcessor;
import com.bloom.client.particle.ShineParticleRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class BloomClient implements ClientModInitializer {
	private static boolean clientInitialized;
	private static boolean reloadListenerRegistered;
	private static final KeyMapping TOGGLE_BLOOM_KEY = KeyBindingHelper.registerKeyBinding(
		new KeyMapping(
			"key.shine.toggle",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			"key.categories.shine"
		)
	);

	@Override
	public void onInitializeClient() {
		if (clientInitialized) {
			BloomMod.LOGGER.debug("Shine client initializer called more than once; skipping duplicate registrations");
			return;
		}
		clientInitialized = true;
		BloomConfig.load();
		ExperimentalConfigManager.load();
		ShineParticleRegistry.registerFactories();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (TOGGLE_BLOOM_KEY.consumeClick()) {
				boolean enabled = BloomPostProcessor.toggleFromKeybind();
				BloomMod.LOGGER.info("Shine post-processing {}", enabled ? "enabled" : "disabled");
			}
		});

		WorldRenderEvents.START.register(BloomPostProcessor::prepareSourceIfEnabled);
		WorldRenderEvents.END.register(BloomPostProcessor::renderIfEnabled);
		registerResourceReloadListener();
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BloomPostProcessor.shutdown());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BloomPostProcessor.shutdown());

	}

	private static void registerResourceReloadListener() {
		if (reloadListenerRegistered) return;
		reloadListenerRegistered = true;
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return new ResourceLocation("shine", "client_resource_reload");
			}

			@Override
			public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
				BloomPostProcessor.shutdown();
				ExperimentalConfigManager.reload();
			}
			});
	}
}