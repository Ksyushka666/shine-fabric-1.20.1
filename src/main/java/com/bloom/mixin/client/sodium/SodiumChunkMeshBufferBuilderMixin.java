package com.bloom.mixin.client.sodium;

import com.bloom.client.selection.BloomSelectionState;
import com.bloom.client.selection.BloomSourceEncoding;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ChunkMeshBufferBuilder.class, remap = false)
public abstract class SodiumChunkMeshBufferBuilderMixin {
    @ModifyArg(
        method = "push",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder;write(JLme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)J"
        ),
        index = 2,
        require = 0
    )
    private ChunkVertexEncoder.Vertex bloom$encodeSourceStrengthInLight(ChunkVertexEncoder.Vertex vertex) {
        double sourceStrength = Math.max(
            BloomSelectionState.getBlockStrength(),
            BloomSelectionState.getFluidStrength()
        );
        if (sourceStrength <= 0.0) {
            return vertex;
        }
        vertex.light = BloomSourceEncoding.encodePackedLight(vertex.light, sourceStrength);
        return vertex;
    }
}
