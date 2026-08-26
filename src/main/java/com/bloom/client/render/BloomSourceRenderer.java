package com.bloom.client.render;

import com.bloom.BloomMod;
import com.bloom.client.selection.BloomSelectionState;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Minecraft 1.20.1 selective bloom source framebuffer and attachment bridge. */
public final class BloomSourceRenderer {
    public static final ResourceLocation SOURCE_TARGET_ID = new ResourceLocation("shine", "source");
    private static TextureTarget sourceTarget;
    private static int sourceWidth = -1;
    private static int sourceHeight = -1;
    private static boolean preparedThisFrame;
    private static boolean attached;
    private static int attachedFramebuffer = 0;
    private static int previousDrawBuffer0 = GL30.GL_COLOR_ATTACHMENT0;
    private static int previousDrawBuffer1 = GL30.GL_NONE;
    private static int previousColorAttachment1 = 0;
    private static boolean attachmentWarningLogged;

    private BloomSourceRenderer() {}

    public static void reset() {
        BloomSelectionState.reset();
        if (attached) detachSourceFromCurrentFramebuffer();
        if (sourceTarget != null) sourceTarget.destroyBuffers();
        sourceTarget = null;
        sourceWidth = -1;
        sourceHeight = -1;
        preparedThisFrame = false;
    }

    public static void prepareSource(WorldRenderContext context) {
        BloomSelectionState.reset();
        if (attached) detachSourceFromCurrentFramebuffer();
        preparedThisFrame = false;
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) return;
        try {
            if (sourceTarget == null) {
                sourceTarget = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
                sourceWidth = main.width;
                sourceHeight = main.height;
            } else if (sourceWidth != main.width || sourceHeight != main.height) {
                sourceTarget.resize(main.width, main.height, Minecraft.ON_OSX);
                sourceWidth = main.width;
                sourceHeight = main.height;
            }
            sourceTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            sourceTarget.clear(false);
            preparedThisFrame = true;
        } catch (RuntimeException exception) {
            BloomMod.LOGGER.warn("Unable to prepare Shine selective bloom source target", exception);
            if (sourceTarget != null) sourceTarget.destroyBuffers();
            sourceTarget = null;
            sourceWidth = -1;
            sourceHeight = -1;
            preparedThisFrame = false;
        }
    }

    /** Attach source texture to COLOR_ATTACHMENT1 of the currently bound terrain framebuffer. */
    public static void attachSourceToCurrentFramebuffer() {
        if (!preparedThisFrame || sourceTarget == null || attached) return;
        attachedFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        previousDrawBuffer0 = GL11.glGetInteger(GL30.GL_DRAW_BUFFER0);
        previousDrawBuffer1 = GL11.glGetInteger(GL30.GL_DRAW_BUFFER1);
        previousColorAttachment1 = GL30.glGetFramebufferAttachmentParameteri(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_COLOR_ATTACHMENT1,
            GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
        );
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_TEXTURE_2D, sourceTarget.getColorTextureId(), 0);
        GL20.glDrawBuffers(new int[] { GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1 });
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_TEXTURE_2D, previousColorAttachment1, 0);
            previousColorAttachment1 = 0;
            if (!attachmentWarningLogged) {
                BloomMod.LOGGER.warn("Shine could not attach selective bloom framebuffer (status 0x{})", Integer.toHexString(status));
                attachmentWarningLogged = true;
            }
            return;
        }
        attached = true;
    }

    /** Stop dual output and detach source texture while preserving its rendered contents. */
    public static void detachSourceFromCurrentFramebuffer() {
        if (!attached) return;
        int currentFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        try {
            if (currentFramebuffer != attachedFramebuffer) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, attachedFramebuffer);
            }
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_TEXTURE_2D, previousColorAttachment1, 0);
            if (previousDrawBuffer1 == GL30.GL_NONE) {
                GL20.glDrawBuffers(previousDrawBuffer0);
            } else {
                GL20.glDrawBuffers(new int[] { previousDrawBuffer0, previousDrawBuffer1 });
            }
        } finally {
            if (currentFramebuffer != attachedFramebuffer) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFramebuffer);
            }
            attachedFramebuffer = 0;
            previousColorAttachment1 = 0;
            attached = false;
        }
    }

    public static RenderTarget getSourceTarget() {
        return preparedThisFrame ? sourceTarget : null;
    }

    public static boolean hasPreparedSourceThisFrame() {
        return preparedThisFrame && sourceTarget != null;
    }

    /** Compatibility hook retained for callers that already have a RenderTarget. */
    public static void enableBloomDrawBuffers(RenderTarget target) {
        attachSourceToCurrentFramebuffer();
    }

    public static void disableBloomDrawBuffers(RenderTarget target) {
        detachSourceFromCurrentFramebuffer();
    }

    public static void enableBloomDrawBuffers() {
        attachSourceToCurrentFramebuffer();
    }

    public static void disableBloomDrawBuffers() {
        detachSourceFromCurrentFramebuffer();
    }

    public static int getInternalRenderWidth(int mainWidth) { return Math.max(1, mainWidth / 2); }
    public static int getInternalRenderHeight(int mainHeight) { return Math.max(1, mainHeight / 2); }
}
