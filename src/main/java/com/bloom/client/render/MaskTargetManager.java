package com.bloom.client.render;

import com.bloom.BloomMod;
import com.bloom.client.config.BloomConfig;
import com.bloom.client.selection.BloomSelection;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Private selective mask pass. It never attaches to Sodium/Iris framebuffers and
 * never changes their draw buffers. The selected blocks are rendered into one
 * bounded half-resolution target after the world has finished rendering.
 */
public final class MaskTargetManager {
    private static TextureTarget target;
    private static int width = -1;
    private static int height = -1;
    private static boolean failureLogged;

    private MaskTargetManager() {}

    public static void prepare(RenderTarget main) {
        if (main == null || main.width <= 0 || main.height <= 0) return;
        RenderSystem.assertOnRenderThreadOrInit();
        int desiredWidth = Math.max(1, Math.min(1024, main.width / 2));
        int desiredHeight = Math.max(1, Math.min(1024, main.height / 2));
        try {
            if (target == null) {
                target = new TextureTarget(desiredWidth, desiredHeight, true, Minecraft.ON_OSX);
            } else if (width != desiredWidth || height != desiredHeight) {
                target.resize(desiredWidth, desiredHeight, Minecraft.ON_OSX);
            }
            width = desiredWidth;
            height = desiredHeight;
            target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            target.clear(false);
            target.copyDepthFrom(main);
            failureLogged = false;
        } catch (RuntimeException exception) {
            disableAfterFailure("Unable to prepare Shine selective mask target", exception);
        }
    }

    /** Render selected solid blocks into the private target. */
    public static void renderBlocks(Level level, BlockPos cameraPos, double cameraX, double cameraY, double cameraZ) {
        if (target == null || level == null || cameraPos == null) return;
        RenderSystem.assertOnRenderThread();
        BloomConfig.Data config = BloomConfig.get();
        int radius = (int) Math.max(4.0, Math.min(64.0, config.bloomDistance));
        int maxBlocks = config.maskBlockBudget;
        int rendered = 0;
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        PoseStack modelView = RenderSystem.getModelViewStack();
        boolean posePushed = false;
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(1024));
        try {
            target.bindWrite(true);
            GL11.glViewport(0, 0, target.width, target.height);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            modelView.pushPose();
            posePushed = true;
            modelView.translate(-cameraX, -cameraY, -cameraZ);
            RenderSystem.applyModelViewMatrix();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = -radius; x <= radius && rendered < maxBlocks; x++) {
                for (int y = -radius; y <= radius && rendered < maxBlocks; y++) {
                    for (int z = -radius; z <= radius && rendered < maxBlocks; z++) {
                        if (x * x + y * y + z * z > radius * radius) continue;
                        pos.set(cameraPos.getX() + x, cameraPos.getY() + y, cameraPos.getZ() + z);
                        if (!level.isLoaded(pos)) continue;
                        double strength = BloomSelection.getBlockSourceStrength(level.getBlockState(pos));
                        if (strength <= 0.0001) continue;
                        modelView.pushPose();
                        modelView.translate(pos.getX(), pos.getY(), pos.getZ());
                        RenderSystem.applyModelViewMatrix();
                        dispatcher.renderBatched(level.getBlockState(pos), pos, level, modelView, buffers.getBuffer(RenderType.solid()), true, RandomSource.create());
                        modelView.popPose();
                        RenderSystem.applyModelViewMatrix();
                        rendered++;
                    }
                }
            }
            buffers.endBatch(RenderType.solid());
        } catch (RuntimeException exception) {
            disableAfterFailure("Selective block mask pass failed; using empty mask", exception);
        } finally {
            if (posePushed) modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            target.unbindWrite();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        }
    }

    public static TextureTarget getTarget() { return target; }

    public static void close() {
        RenderSystem.assertOnRenderThreadOrInit();
        if (target != null) target.destroyBuffers();
        target = null;
        width = height = -1;
    }

    private static void disableAfterFailure(String message, RuntimeException exception) {
        if (!failureLogged) {
            BloomMod.LOGGER.warn(message, exception);
            failureLogged = true;
        }
        if (target != null) target.destroyBuffers();
        target = null;
        width = height = -1;
    }
}
