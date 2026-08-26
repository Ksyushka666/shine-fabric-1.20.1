package com.bloom.mixin.client;

import com.bloom.client.compat.IrisCompat;
import com.bloom.client.selection.BloomSelectionState;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceBloomUniformMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void shine$applyParticleStrength(CallbackInfo ci) {
        if (IrisCompat.shouldYieldToShaderPack()) return;
        ShaderInstance shader = (ShaderInstance) (Object) this;
        if (shader.getName().contains("particle")) {
            Uniform uniform = shader.getUniform("ShineParticleStrength");
            if (uniform != null) uniform.set((float) BloomSelectionState.getParticleStrength());
        } else if (shader.getName().contains("entity")) {
            Uniform uniform = shader.getUniform("ShineEntityStrength");
            if (uniform != null) uniform.set((float) BloomSelectionState.getEntityStrength());
        }
    }
}
