package com.bloom.mixin.client;

import com.bloom.client.selection.BloomSelection;
import com.bloom.client.selection.BloomSelectionState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererBloomSelectionMixin<T extends Entity> {
    @Unique private double shine$previousEntityStrength;

    @Inject(method = "render", at = @At("HEAD"))
    private void shine$pushEntityStrength(T entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        ResourceLocation texture = ((EntityRenderer<T>) (Object) this).getTextureLocation(entity);
        shine$previousEntityStrength = BloomSelectionState.pushEntityStrength(BloomSelection.getEntitySourceStrength(texture));
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void shine$popEntityStrength(T entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        BloomSelectionState.popEntityStrength(shine$previousEntityStrength);
    }
}
