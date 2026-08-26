package com.bloom.mixin.client.sodium;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the selective bloom varying only when the complete Sodium 0.5.x template is recognized. */
@Mixin(value = ShaderLoader.class, remap = false)
public abstract class SodiumShaderLoaderMixin {
    private static final String VERTEX_COLOR_DECL = "out vec4 v_Color;";
    private static final String VERTEX_LIGHT_LINE = "    v_Color = _vert_color * _sample_lightmap(u_LightTex, _vert_tex_light_coord);";
    private static final String FRAGMENT_COLOR_DECL = "out vec4 fragColor; // The output fragment for the color framebuffer";
    private static final String FRAGMENT_FOG_LINE = "    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);";

    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true, require = 0)
    private static void shine$injectBloomOutput(ResourceLocation name, CallbackInfoReturnable<String> cir) {
        String shader = cir.getReturnValue();
        if (shader == null || !"sodium".equals(name.getNamespace()) || !name.getPath().contains("block_layer_opaque")) return;

        if (name.getPath().endsWith(".vsh")) {
            String transformed = transformVertex(shader);
            if (!transformed.equals(shader)) cir.setReturnValue(transformed);
            return;
        }
        if (name.getPath().endsWith(".fsh")) {
            String transformed = transformFragment(shader);
            if (!transformed.equals(shader)) cir.setReturnValue(transformed);
        }
    }

    private static String transformVertex(String shader) {
        if (shader.contains("v_ShineSourceStrength") || !shader.contains(VERTEX_COLOR_DECL)
                || !shader.contains("void main() {") || !shader.contains(VERTEX_LIGHT_LINE)) return shader;
        String result = shader.replace(VERTEX_COLOR_DECL, VERTEX_COLOR_DECL + "\nout float v_ShineSourceStrength;");
        result = result.replace("void main() {", """
                float shine_decode_light_strength(ivec2 light) {
                    int blockNibble = light.x & 15;
                    int skyNibble = light.y & 15;
                    int low6 = (blockNibble & 7) | ((skyNibble & 7) << 3);
                    int mode = ((blockNibble >> 3) & 1) | (((skyNibble >> 3) & 1) << 1);
                    if (mode == 0) return float(low6) / 63.0;
                    int code = (mode - 1) * 64 + low6 + 1;
                    return clamp((100.0 + float(code - 1) * (400.0 / 191.0)) / 500.0, 0.0, 1.0);
                }

                void main() {
                """);
        result = result.replace(VERTEX_LIGHT_LINE, VERTEX_LIGHT_LINE + "\n    v_ShineSourceStrength = shine_decode_light_strength(_vert_tex_light_coord);");
        return result;
    }

    private static String transformFragment(String shader) {
        if (shader.contains("bloomColor") || !shader.contains(FRAGMENT_COLOR_DECL)
                || !shader.contains(FRAGMENT_FOG_LINE)) return shader;
        String result = shader.replace(FRAGMENT_COLOR_DECL,
                FRAGMENT_COLOR_DECL + "\nin float v_ShineSourceStrength;\nout vec4 bloomColor;");
        result = result.replace(FRAGMENT_FOG_LINE,
                FRAGMENT_FOG_LINE + "\n    bloomColor = vec4(fragColor.rgb * fragColor.a, v_ShineSourceStrength);");
        return result;
    }
}
