package com.bloom.mixin.client;

import com.bloom.client.render.BloomSourceRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the second selective-bloom framebuffer output around vanilla opaque terrain rendering. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererBloomAttachmentMixin {
    @Inject(method = "renderChunkLayer", at = @At("HEAD"))
    private void shine$attachBloomTarget(RenderType renderType, PoseStack poseStack, double cameraX, double cameraY, double cameraZ, Matrix4f projection, CallbackInfo ci) {
        // Disabled: bloom captures the completed main framebuffer safely after world render.
    }

    @Inject(method = "renderChunkLayer", at = @At("RETURN"))
    private void shine$detachBloomTarget(RenderType renderType, PoseStack poseStack, double cameraX, double cameraY, double cameraZ, Matrix4f projection, CallbackInfo ci) {
        // No framebuffer attachment is modified on the legacy 1.20.1 path.
    }
}
