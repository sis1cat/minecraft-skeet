package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.CoreShaders;
import net.optifine.Config;
import net.optifine.shaders.GlState;

public abstract class RenderTarget {
    private static final int RED_CHANNEL = 0;
    private static final int GREEN_CHANNEL = 1;
    private static final int BLUE_CHANNEL = 2;
    private static final int ALPHA_CHANNEL = 3;
    public int width;
    public int height;
    public int viewWidth;
    public int viewHeight;
    public final boolean useDepth;
    public int frameBufferId;
    protected int colorTextureId;
    protected int depthBufferId;
    private final float[] clearChannels = Util.make(() -> new float[]{1.0F, 1.0F, 1.0F, 0.0F});
    public int filterMode;
    private boolean enabled = true;
    private boolean stencilEnabled = false;

    public RenderTarget(boolean pUseDepth) {
        this.useDepth = pUseDepth;
        this.frameBufferId = -1;
        this.colorTextureId = -1;
        this.depthBufferId = -1;
        if (this instanceof MainTarget) {
            this.enabled = Config.isMainFramebufferEnabled();
        }
    }

    public void resize(int pWidth, int pHeight) {
        if (!this.enabled) {
            this.viewWidth = pWidth;
            this.viewHeight = pHeight;
            this.width = pWidth;
            this.height = pHeight;
        } else {
            RenderSystem.assertOnRenderThreadOrInit();
            GlStateManager._enableDepthTest();
            if (this.frameBufferId >= 0) {
                this.destroyBuffers();
            }

            this.createBuffers(pWidth, pHeight);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    public void destroyBuffers() {
        if (this.enabled) {
            RenderSystem.assertOnRenderThreadOrInit();
            this.unbindRead();
            this.unbindWrite();
            if (this.depthBufferId > -1) {
                TextureUtil.releaseTextureId(this.depthBufferId);
                this.depthBufferId = -1;
            }

            if (this.colorTextureId > -1) {
                TextureUtil.releaseTextureId(this.colorTextureId);
                this.colorTextureId = -1;
            }

            if (this.frameBufferId > -1) {
                GlStateManager._glBindFramebuffer(36160, 0);
                GlStateManager._glDeleteFramebuffers(this.frameBufferId);
                this.frameBufferId = -1;
            }
        }
    }

    public void copyDepthFrom(RenderTarget pOtherTarget) {
        if (this.enabled) {
            RenderSystem.assertOnRenderThreadOrInit();
            GlStateManager._glBindFramebuffer(36008, pOtherTarget.frameBufferId);
            GlStateManager._glBindFramebuffer(36009, this.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, pOtherTarget.width, pOtherTarget.height, 0, 0, this.width, this.height, 256, 9728);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    public void createBuffers(int pWidth, int pHeight) {
        RenderSystem.assertOnRenderThreadOrInit();
        int i = RenderSystem.maxSupportedTextureSize();
        if (pWidth > 0 && pWidth <= i && pHeight > 0 && pHeight <= i) {
            this.viewWidth = pWidth;
            this.viewHeight = pHeight;
            this.width = pWidth;
            this.height = pHeight;
            if (!this.enabled) {
                this.clear();
            } else {
                this.frameBufferId = GlStateManager.glGenFramebuffers();
                this.colorTextureId = TextureUtil.generateTextureId();
                if (this.useDepth) {
                    this.depthBufferId = TextureUtil.generateTextureId();
                    GlStateManager._bindTexture(this.depthBufferId);
                    GlStateManager._texParameter(3553, 10241, 9728);
                    GlStateManager._texParameter(3553, 10240, 9728);
                    GlStateManager._texParameter(3553, 34892, 0);
                    GlStateManager._texParameter(3553, 10242, 33071);
                    GlStateManager._texParameter(3553, 10243, 33071);
                    if (this.stencilEnabled) {
                        GlStateManager._texImage2D(3553, 0, 36013, this.width, this.height, 0, 34041, 36269, null);
                    } else {
                        GlStateManager._texImage2D(3553, 0, 6402, this.width, this.height, 0, 6402, 5126, null);
                    }
                }

                this.setFilterMode(9728, true);
                GlStateManager._bindTexture(this.colorTextureId);
                GlStateManager._texParameter(3553, 10242, 33071);
                GlStateManager._texParameter(3553, 10243, 33071);
                GlStateManager._texImage2D(3553, 0, 32856, this.width, this.height, 0, 6408, 5121, null);
                GlStateManager._glBindFramebuffer(36160, this.frameBufferId);
                GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, this.colorTextureId, 0);
                if (this.useDepth) {
                    GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.depthBufferId, 0);
                    if (this.stencilEnabled) {
                        GlStateManager._glFramebufferTexture2D(36160, 36128, 3553, this.depthBufferId, 0);
                    }
                }

                this.checkStatus();
                this.clear();
                this.unbindRead();
            }
        } else {
            throw new IllegalArgumentException("Window " + pWidth + "x" + pHeight + " size out of bounds (max. size: " + i + ")");
        }
    }

    public void setFilterMode(int pFilterMode) {
        this.setFilterMode(pFilterMode, false);
    }

    private void setFilterMode(int pFilterMode, boolean pForce) {
        if (this.enabled) {
            RenderSystem.assertOnRenderThreadOrInit();
            if (pForce || pFilterMode != this.filterMode) {
                this.filterMode = pFilterMode;
                GlStateManager._bindTexture(this.colorTextureId);
                GlStateManager._texParameter(3553, 10241, pFilterMode);
                GlStateManager._texParameter(3553, 10240, pFilterMode);
                GlStateManager._bindTexture(0);
            }
        }
    }

    public void checkStatus() {
        RenderSystem.assertOnRenderThreadOrInit();
        int i = GlStateManager.glCheckFramebufferStatus(36160);
        if (i != 36053) {
            if (i == 36054) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            } else if (i == 36055) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            } else if (i == 36059) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
            } else if (i == 36060) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
            } else if (i == 36061) {
                throw new RuntimeException("GL_FRAMEBUFFER_UNSUPPORTED");
            } else if (i == 1285) {
                throw new RuntimeException("GL_OUT_OF_MEMORY");
            } else {
                throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
            }
        }
    }

    public void bindRead() {
        RenderSystem.assertOnRenderThread();
        GlStateManager._bindTexture(this.colorTextureId);
    }

    public void unbindRead() {
        if (this.enabled) {
            RenderSystem.assertOnRenderThreadOrInit();
            GlStateManager._bindTexture(0);
        }
    }

    public void bindWrite(boolean pSetViewport) {
        if (this.enabled) {
            if (!Config.isShaders() || GlState.getFramebuffer() == null) {
                RenderSystem.assertOnRenderThreadOrInit();
                GlStateManager._glBindFramebuffer(36160, this.frameBufferId);
                if (pSetViewport) {
                    GlStateManager._viewport(0, 0, this.viewWidth, this.viewHeight);
                }
            }
        }
    }

    public void unbindWrite() {
        if (this.enabled) {
            RenderSystem.assertOnRenderThreadOrInit();
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    public void setClearColor(float pRed, float pGreen, float pBlue, float pAlpha) {
        this.clearChannels[0] = pRed;
        this.clearChannels[1] = pGreen;
        this.clearChannels[2] = pBlue;
        this.clearChannels[3] = pAlpha;
    }

    public void blitToScreen(int pWidth, int pHeight) {
        if (this.enabled) {
            GlStateManager._glBindFramebuffer(36008, this.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, this.width, this.height, 0, 0, pWidth, pHeight, 16384, 9728);
            GlStateManager._glBindFramebuffer(36008, 0);
        }
    }

    public void blitAndBlendToScreen(int pWidth, int pHeight) {
        if (this.enabled) {
            RenderSystem.assertOnRenderThread();
            GlStateManager._colorMask(true, true, true, false);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask(false);
            GlStateManager._viewport(0, 0, pWidth, pHeight);
            CompiledShaderProgram compiledshaderprogram = Objects.requireNonNull(RenderSystem.setShader(CoreShaders.BLIT_SCREEN), "Blit shader not loaded");
            compiledshaderprogram.bindSampler("InSampler", this.colorTextureId);
            BufferBuilder bufferbuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);
            bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
        }
    }

    public void clear() {
        RenderSystem.assertOnRenderThreadOrInit();
        this.bindWrite(true);
        GlStateManager._clearColor(this.clearChannels[0], this.clearChannels[1], this.clearChannels[2], this.clearChannels[3]);
        int i = 16384;
        if (this.useDepth) {
            GlStateManager._clearDepth(1.0);
            i |= 256;
        }

        GlStateManager._clear(i);
        this.unbindWrite();
    }

    public int getColorTextureId() {
        return this.colorTextureId;
    }

    public int getDepthTextureId() {
        return this.depthBufferId;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void enableStencil() {
        if (!this.stencilEnabled) {
            this.stencilEnabled = true;
            this.resize(this.viewWidth, this.viewHeight);
        }
    }

    public boolean isStencilEnabled() {
        return this.stencilEnabled;
    }
}