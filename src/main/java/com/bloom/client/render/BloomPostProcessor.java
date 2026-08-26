package com.bloom.client.render;

import com.bloom.BloomMod;
import com.bloom.client.compat.IrisCompat;
import com.bloom.client.config.BloomConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import com.mojang.blaze3d.shaders.Uniform;
import com.bloom.mixin.client.accessor.PostChainAccessor;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Minecraft 1.20.1 implementation backed by the legacy RenderTarget/PostChain pipeline. */
public final class BloomPostProcessor {
    private static final ResourceLocation CHAIN_ID = new ResourceLocation("shine", "shaders/post/bloom_poc.json");
    private static PostChain runtimeChain;
    private static int chainWidth = -1;
    private static int chainHeight = -1;
    private static boolean irisDisabled;
    private static boolean uniformBindingWarningLogged;
    private static boolean chainFailureLogged;

    private BloomPostProcessor() {}

    public static boolean toggleFromKeybind() {
        BloomConfig.Data config = BloomConfig.get();
        config.enabled = !config.enabled;
        BloomConfig.save();
        return config.enabled;
    }

    public static void onConfigSaved() {
        closeRuntimeChain();
        BloomSourceRenderer.reset();
        MaskTargetManager.close();
        chainFailureLogged = false;
        uniformBindingWarningLogged = false;
    }

    /** Releases all GPU resources when the client is stopping. */
    public static void shutdown() {
        closeRuntimeChain();
        BloomSourceRenderer.reset();
        MaskTargetManager.close();
        irisDisabled = false;
        uniformBindingWarningLogged = false;
        chainFailureLogged = false;
    }

    public static void prepareSourceIfEnabled(WorldRenderContext context) {
        BloomConfig.Data config = BloomConfig.get();
        if (!config.enabled || shouldSkipForIris()) return;
        if (Minecraft.getInstance().level == null) return;
        BloomSourceRenderer.prepareSource(context);
    }

    public static void renderIfEnabled(WorldRenderContext context) {
        BloomConfig.Data config = BloomConfig.get();
        if (!config.enabled || shouldSkipForIris() || config.strength <= 0.0001) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        RenderTarget main = minecraft.getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) return;
        PostChain chain = ensureChain(main.width, main.height);
        if (chain == null) return;

        RenderTarget chainSource = chain.getTempTarget("source");
        RenderTarget chainScene = chain.getTempTarget("scene");
        RenderTarget chainMask = chain.getTempTarget("mask");
        int[] previousViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, previousViewport);
        boolean previousBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean previousDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        try {
            if (chainScene != null) copyTarget(main, chainScene);
            if (chainSource != null) copyTarget(main, chainSource);
            if (config.selectiveMaskEnabled) {
                MaskTargetManager.prepare(main);
                if (minecraft.getCameraEntity() != null) {
                    var camera = minecraft.getCameraEntity();
                    MaskTargetManager.renderBlocks(minecraft.level, camera.blockPosition(), camera.getX(), camera.getY(), camera.getZ());
                }
                if (chainMask != null && MaskTargetManager.getTarget() != null) copyTarget(MaskTargetManager.getTarget(), chainMask);
            }
            applyConfigUniforms(chain, config);
            chain.process(0.0F);
        } catch (RuntimeException exception) {
            if (!chainFailureLogged) {
                BloomMod.LOGGER.warn("Shine bloom chain failed during processing; disabling it until the next reload", exception);
                chainFailureLogged = true;
            }
            closeRuntimeChain();
            BloomSourceRenderer.reset();
        } finally {
            GL11.glViewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);
            if (previousBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (previousDepth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        }
    }

    private static boolean shouldSkipForIris() {
        boolean skip = IrisCompat.shouldDisableBloom();
        if (skip && !irisDisabled) {
            BloomMod.LOGGER.info(IrisCompat.disableMessage());
            irisDisabled = true;
            closeRuntimeChain();
            BloomSourceRenderer.reset();
        } else if (!skip) {
            irisDisabled = false;
        }
        return skip;
    }

    private static PostChain ensureChain(int width, int height) {
        if (width <= 0 || height <= 0) return null;
        if (runtimeChain != null && chainWidth == width && chainHeight == height) return runtimeChain;
        closeRuntimeChain();
        Minecraft minecraft = Minecraft.getInstance();
        try {
            runtimeChain = new PostChain(
                minecraft.getTextureManager(),
                minecraft.getResourceManager(),
                minecraft.getMainRenderTarget(),
                CHAIN_ID
            );
            runtimeChain.resize(width, height);
            chainWidth = width;
            chainHeight = height;
            return runtimeChain;
        } catch (Exception exception) {
            if (!chainFailureLogged) {
                BloomMod.LOGGER.warn("Unable to create Shine 1.20.1 bloom chain; bloom remains disabled until the next reload", exception);
                chainFailureLogged = true;
            }
            runtimeChain = null;
            return null;
        }
    }

    private static void applyConfigUniforms(PostChain chain, BloomConfig.Data config) {
        if (!(chain instanceof PostChainAccessor accessor)) {
            if (!uniformBindingWarningLogged) {
                BloomMod.LOGGER.warn("Shine PostChain accessor is unavailable; using JSON uniform defaults");
                uniformBindingWarningLogged = true;
            }
            return;
        }
        int blurIndex = 0;
        int activeBlurPasses = Math.max(1, Math.min(BloomConfig.MAX_BLUR_PASSES, config.blurPassCount)) * 2;
        for (PostPass pass : accessor.shine$getPasses()) {
            EffectInstance effect = pass.getEffect();
            if (effect == null) continue;
            String effectName = effect.getName() == null ? "" : effect.getName();
            setUniform(effect, "Threshold", (float) config.threshold);
            setUniform(effect, "Strength", (float) Math.max(0.0, Math.min(BloomConfig.MAX_STRENGTH, config.strength * 0.10625)));
            setUniform(effect, "SourceStrengthScale", (float) Math.max(0.0, config.defaultLightSourceStrength / BloomConfig.MAX_SOURCE_STRENGTH));
            setUniform(effect, "HighlightClamp", (float) config.highlightClamp);
            setUniform(effect, "SoftKnee", (float) config.softKnee);
            setUniform(effect, "SelectiveMask", config.selectiveMaskEnabled ? 1.0F : 0.0F);
            setUniform(effect, "MaxDistance", (float) config.bloomDistance);
            setUniform(effect, "DistanceFadeRange", (float) Math.max(1.0, config.bloomDistance * 0.25));
            if (effectName.endsWith("bloom_blur_horizontal") || effectName.endsWith("bloom_blur_vertical")) {
                double radius = blurIndex < activeBlurPasses
                    ? (blurIndex < 2 ? config.tinyRadius : blurIndex < 4 ? config.radius : config.broadRadius)
                    : 0.0;
                setUniform(effect, "Radius", (float) Math.max(0.0, Math.min(BloomConfig.MAX_RADIUS, radius)));
                blurIndex++;
            }
        }
    }

    private static void setUniform(EffectInstance effect, String name, float value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void copyTarget(RenderTarget source, RenderTarget destination) {
        RenderSystem.assertOnRenderThreadOrInit();
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
            GL30.glBlitFramebuffer(
                0, 0, source.width, source.height,
                0, 0, destination.width, destination.height,
                GL30.GL_COLOR_BUFFER_BIT,
                GL30.GL_LINEAR
            );
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
        }
    }

    private static void closeRuntimeChain() {
        if (runtimeChain != null) {
            runtimeChain.close();
            runtimeChain = null;
        }
        chainWidth = -1;
        chainHeight = -1;
    }
}
