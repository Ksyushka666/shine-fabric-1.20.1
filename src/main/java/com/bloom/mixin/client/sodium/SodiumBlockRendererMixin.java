package com.bloom.mixin.client.sodium;

import com.bloom.client.selection.BloomSelection;
import com.bloom.client.selection.BloomSelectionState;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium 0.5.x hook.  In 0.5.x BlockRenderer.renderModel receives the
 * mutable BlockRenderContext and ChunkBuildBuffers; the older four-argument
 * renderModel signature belonged to a different Sodium generation and caused
 * a fatal Mixin callback descriptor failure on 0.5.13.
 */
@Mixin(value = BlockRenderer.class, remap = false)
public abstract class SodiumBlockRendererMixin {
    @Unique
    private double shine$previousBlockStrength;

    @Inject(method = "renderModel(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;)V", at = @At("HEAD"), require = 0)
    private void shine$pushBlockStrength(BlockRenderContext context, ChunkBuildBuffers buffers, CallbackInfo ci) {
        if (context == null || context.state() == null) {
            this.shine$previousBlockStrength = BloomSelectionState.pushBlockStrength(0.0D);
            return;
        }
        this.shine$previousBlockStrength = BloomSelectionState.pushBlockStrength(
            BloomSelection.getBlockSourceStrength(context.state())
        );
    }

    @Inject(method = "renderModel(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;)V", at = @At("RETURN"), require = 0)
    private void shine$popBlockStrength(BlockRenderContext context, ChunkBuildBuffers buffers, CallbackInfo ci) {
        BloomSelectionState.popBlockStrength(this.shine$previousBlockStrength);
    }
}
