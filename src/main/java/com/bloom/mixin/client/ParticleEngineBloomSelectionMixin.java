package com.bloom.mixin.client;

import com.bloom.client.config.BloomConfig;
import com.bloom.client.selection.BloomSelectionState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineBloomSelectionMixin {
    @Unique private double shine$previousParticleStrength;

    @Inject(method = "render", at = @At("HEAD"))
    private void shine$pushParticleStrength(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        shine$previousParticleStrength = BloomSelectionState.pushParticleStrength(BloomConfig.get().defaultParticleStrength);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void shine$popParticleStrength(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        BloomSelectionState.popParticleStrength(shine$previousParticleStrength);
    }
}
