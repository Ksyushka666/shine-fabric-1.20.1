package com.bloom.client.ambience;

import com.bloom.BloomMod;
import com.bloom.client.config.ExperimentalConfigManager;
import com.bloom.particle.ShineParticleTypes;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Client-only, bounded ambience scheduler that never touches render targets. */
public final class ShineAmbienceManager {
    private static final int TICK_INTERVAL = 12;
    private static final double MAX_HORIZONTAL_DISTANCE = 18.0;
    private static final RandomSource RANDOM = RandomSource.create(0x5348494E45L);
    private static int tickCounter;
    private static boolean warned;

    private ShineAmbienceManager() {}

    public static void tick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) return;
        if (!ExperimentalConfigManager.enabled() || client.isPaused() || client.screen != null) return;
        if (++tickCounter % TICK_INTERVAL != 0 || client.level.getGameTime() < 20L) return;
        if (client.player.isSpectator() || RANDOM.nextFloat() > 0.18F) return;

        SimpleParticleType type = chooseType(client.level);
        if (type == null) return;
        double x = client.player.getX() + (RANDOM.nextDouble() - 0.5) * MAX_HORIZONTAL_DISTANCE;
        double z = client.player.getZ() + (RANDOM.nextDouble() - 0.5) * MAX_HORIZONTAL_DISTANCE;
        double y = client.player.getY() + 1.0 + RANDOM.nextDouble() * 3.5;
        if (!client.level.isLoaded(BlockPos.containing(x, y, z))) return;
        client.level.addParticle(type, x, y, z, (RANDOM.nextDouble() - 0.5) * 0.01,
            0.005 + RANDOM.nextDouble() * 0.012, (RANDOM.nextDouble() - 0.5) * 0.01);
    }

    private static SimpleParticleType chooseType(ClientLevel level) {
        try {
            Map<String, SimpleParticleType> types = ShineParticleTypes.all();
            if (level.isDay() && RANDOM.nextFloat() < 0.55F) {
                return types.get("world_ambience_leaf");
            }
            return types.get("world_ambience_firefly");
        } catch (RuntimeException exception) {
            if (!warned) {
                BloomMod.LOGGER.debug("Shine ambience particle selection unavailable", exception);
                warned = true;
            }
            return null;
        }
    }

    public static void reset() {
        tickCounter = 0;
        warned = false;
    }
}
