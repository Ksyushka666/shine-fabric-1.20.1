package com.bloom.mixin.client.sodium;

import com.bloom.client.selection.BloomSelection;
import com.bloom.client.selection.BloomSelectionState;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidRenderer.class, remap = false)
public abstract class SodiumDefaultFluidRendererMixin {
    @Unique
    private double bloom$previousFluidStrength;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void bloom$pushFluidStrength(
        WorldSlice level,
        FluidState fluidState,
        BlockPos blockPos,
        BlockPos modelOffset,
        ChunkBuildBuffers buffers,
        CallbackInfo ci
    ) {
        this.bloom$previousFluidStrength = BloomSelectionState.pushFluidStrength(
            BloomSelection.getFluidSourceStrength(fluidState)
        );
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void bloom$popFluidStrength(
        WorldSlice level,
        FluidState fluidState,
        BlockPos blockPos,
        BlockPos modelOffset,
        ChunkBuildBuffers buffers,
        CallbackInfo ci
    ) {
        BloomSelectionState.popFluidStrength(this.bloom$previousFluidStrength);
    }
}
