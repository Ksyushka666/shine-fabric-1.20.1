package com.bloom;

import com.bloom.particle.ShineParticleTypes;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BloomMod implements ModInitializer {
	public static final String MOD_ID = "shine";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ShineParticleTypes.register();
	}
}
