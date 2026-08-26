package com.bloom.client.selection;

import com.bloom.client.config.BloomConfig;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class BloomSelection {
    private static final Set<ResourceLocation> SELECTED_FLUID_IDS = Set.of(
        new ResourceLocation("minecraft", "lava"),
        new ResourceLocation("minecraft", "flowing_lava")
    );

    private BloomSelection() {
    }

    public static double getBlockSourceStrength(BlockState blockState) {
        BloomConfig.Data config = BloomConfig.get();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (blockId == null) return 0.0;

        Double stateOverride = config.stateSourceStrengthOverrides.get(blockId.toString());
        if (stateOverride != null) return clampSourceStrength(stateOverride);

        Double override = config.blockStrengthOverrides.get(blockId.toString());
        if (override != null) return clampSourceStrength(override);

        double fallback = blockState.getLightEmission() > 0 ? config.defaultLightSourceStrength : config.defaultNonLightStrength;
        return clampSourceStrength(fallback);
    }

    public static double getFluidSourceStrength(FluidState fluidState) {
        if (fluidState.isEmpty()) return 0.0;

        BloomConfig.Data config = BloomConfig.get();
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluidState.getType());
        if (fluidId == null) return 0.0;
        Double override = config.blockStrengthOverrides.get(fluidId.toString());
        if (override != null) return clampSourceStrength(override);

        Block legacyFluidBlock = fluidState.createLegacyBlock().getBlock();
        ResourceLocation legacyBlockId = BuiltInRegistries.BLOCK.getKey(legacyFluidBlock);
        if (legacyBlockId != null) {
            Double legacyOverride = config.blockStrengthOverrides.get(legacyBlockId.toString());
            if (legacyOverride != null) return clampSourceStrength(legacyOverride);
        }

        double fallback = SELECTED_FLUID_IDS.contains(fluidId) ? config.defaultLightSourceStrength : config.defaultNonLightStrength;
        return clampSourceStrength(fallback);
    }

    public static double getEntitySourceStrength(ResourceLocation textureId) {
        BloomConfig.Data config = BloomConfig.get();
        if (textureId != null) {
            Double override = config.entityTextureStrengthOverrides.get(textureId.toString());
            if (override != null) return clampSourceStrength(override);
        }
        return clampSourceStrength(config.defaultEntityTextureStrength);
    }

    public static double getParticleSourceStrength(ResourceLocation particleId) {
        BloomConfig.Data config = BloomConfig.get();
        if (particleId != null) {
            Double override = config.particleStrengthOverrides.get(particleId.toString());
            if (override != null) return clampSourceStrength(override);
        }
        return clampSourceStrength(config.defaultParticleStrength);
    }

    private static double clampSourceStrength(double value) {
        if (!Double.isFinite(value)) return BloomConfig.MIN_SOURCE_STRENGTH;
        return Math.max(BloomConfig.MIN_SOURCE_STRENGTH, Math.min(BloomConfig.MAX_SOURCE_STRENGTH, value));
    }
}
