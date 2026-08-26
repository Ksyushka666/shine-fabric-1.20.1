package com.bloom.client.particle;

import java.util.Map;
import java.util.Random;
import com.bloom.particle.ShineParticleTypes;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Client-only registration of sprite-sheet factories for all common Shine particle types. */
public final class ShineParticleRegistry {
    private static volatile boolean factoriesRegistered;
    private ShineParticleRegistry() {}

    public static synchronized void registerFactories() {
        if (factoriesRegistered) return;
        Map<String, SimpleParticleType> types = ShineParticleTypes.all();
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        for (SimpleParticleType type : types.values()) {
            registry.register(type, ShineTexturedParticle.Provider::new);
        }
        factoriesRegistered = true;
    }

    public static int registeredCount() {
        return ShineParticleTypes.count();
    }

    private static final class ShineTexturedParticle extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final Random random = new Random();

        private ShineTexturedParticle(ClientLevel level, double x, double y, double z,
                                      double velocityX, double velocityY, double velocityZ,
                                      SpriteSet sprites) {
            super(level, x, y, z, velocityX, velocityY, velocityZ);
            this.sprites = sprites;
            this.friction = 0.96F;
            this.gravity = 0.0F;
            this.quadSize = 0.2F;
            this.lifetime = 20 + this.random.nextInt(20);
            this.setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            this.setSpriteFromAge(this.sprites);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        private static final class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            private Provider(SpriteSet sprites) { this.sprites = sprites; }
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                           double velocityX, double velocityY, double velocityZ) {
                return new ShineTexturedParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites);
            }
        }
    }
}
