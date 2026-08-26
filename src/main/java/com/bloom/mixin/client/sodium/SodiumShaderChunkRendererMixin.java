package com.bloom.mixin.client.sodium;

import com.bloom.client.render.BloomSourceRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShaderChunkRenderer.class, remap = false)
public abstract class SodiumShaderChunkRendererMixin {
    @Inject(method = "begin", at = @At("HEAD"), require = 0)
    private void shine$enableBloomAttachment(TerrainRenderPass pass, CallbackInfo ci) {
        // Disabled on 1.20.1: the secondary draw buffer corrupts Intel/Sodium framebuffers.
    }

    @Inject(method = "end", at = @At("RETURN"), require = 0)
    private void shine$disableBloomAttachment(TerrainRenderPass pass, CallbackInfo ci) {
        // Source is captured from the completed main target by BloomPostProcessor.
    }

    @Redirect(
        method = "compileProgram",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/gl/shader/GlProgram$Builder;bindFragmentData(Ljava/lang/String;I)Lme/jellysquid/mods/sodium/client/gl/shader/GlProgram$Builder;"
        ),
        require = 0
    )
    private me.jellysquid.mods.sodium.client.gl.shader.GlProgram.Builder shine$bindBloomFragmentOutput(
        me.jellysquid.mods.sodium.client.gl.shader.GlProgram.Builder builder,
        String name,
        int index
    ) {
        return builder.bindFragmentData(name, index);
    }
}
