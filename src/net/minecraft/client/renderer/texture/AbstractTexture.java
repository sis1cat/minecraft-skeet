package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.TriState;
import net.optifine.Config;
import net.optifine.shaders.MultiTexID;
import net.optifine.shaders.ShadersTex;

public abstract class AbstractTexture implements AutoCloseable {
    public static final int NOT_ASSIGNED = -1;
    protected int id = -1;
    protected boolean defaultBlur;
    private int wrapS = 10497;
    private int wrapT = 10497;
    private int minFilter = 9986;
    private int magFilter = 9729;
    public MultiTexID multiTex;
    private boolean blurMipmapSet;
    private boolean blur;
    private boolean mipmap;
    private boolean updateBlurMipmap = true;
    private boolean lastBlur;
    private boolean lastMipmap;

    public void setClamp(boolean pClamp) {
        RenderSystem.assertOnRenderThreadOrInit();
        int i;
        int j;
        if (pClamp) {
            i = 33071;
            j = 33071;
        } else {
            i = 10497;
            j = 10497;
        }

        boolean flag = this.wrapS != i;
        boolean flag1 = this.wrapT != j;
        if (flag || flag1) {
            this.bind();
            if (flag) {
                GlStateManager._texParameter(3553, 10242, i);
                this.wrapS = i;
            }

            if (flag1) {
                GlStateManager._texParameter(3553, 10243, j);
                this.wrapT = j;
            }
        }
    }

    public void setFilter(TriState pBlur, boolean pMipmap) {
        this.setFilter(pBlur.toBoolean(this.defaultBlur), pMipmap);
    }

    public void setFilter(boolean pBlur, boolean pMipmap) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (!this.blurMipmapSet || this.blur != pBlur || this.mipmap != pMipmap) {
            this.blur = pBlur;
            this.mipmap = pMipmap;
            this.blurMipmapSet = true;
            int i;
            int j;
            if (pBlur) {
                i = pMipmap ? 9987 : 9729;
                j = 9729;
            } else {
                int k = Config.getMipmapType();
                i = pMipmap ? k : 9728;
                j = 9728;
            }

            boolean flag1 = this.minFilter != i;
            boolean flag = this.magFilter != j;
            if (flag || flag1) {
                this.bind();
                if (flag1) {
                    GlStateManager._texParameter(3553, 10241, i);
                    this.minFilter = i;
                }

                if (flag) {
                    GlStateManager._texParameter(3553, 10240, j);
                    this.magFilter = j;
                }
            }
        }
    }

    public int getId() {
        RenderSystem.assertOnRenderThreadOrInit();
        if (this.id == -1) {
            this.id = TextureUtil.generateTextureId();
        }

        return this.id;
    }

    public void releaseId() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                ShadersTex.deleteTextures(this, this.id);
                this.blurMipmapSet = false;
                if (this.id != -1) {
                    TextureUtil.releaseTextureId(this.id);
                    this.id = -1;
                }
            });
        } else if (this.id != -1) {
            ShadersTex.deleteTextures(this, this.id);
            this.blurMipmapSet = false;
            TextureUtil.releaseTextureId(this.id);
            this.id = -1;
        }
    }

    public void bind() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> {
                if (Config.isShaders()) {
                    ShadersTex.bindTexture(this);
                } else {
                    GlStateManager._bindTexture(this.getId());
                }
            });
        } else if (Config.isShaders()) {
            ShadersTex.bindTexture(this);
        } else {
            GlStateManager._bindTexture(this.getId());
        }
    }

    @Override
    public void close() {
    }

    public MultiTexID getMultiTexID() {
        return ShadersTex.getMultiTexID(this);
    }

    public void setBlurMipmap(boolean blur, boolean mipmap) {
        this.lastBlur = blur;
        this.lastMipmap = mipmap;
        this.setFilter(blur, mipmap);
    }

    public void restoreLastBlurMipmap() {
        this.setFilter(this.lastBlur, this.lastMipmap);
    }

    public void resetBlurMipmap() {
        this.blurMipmapSet = false;
    }

    public void setUpdateBlurMipmap(boolean updateBlurMipmap) {
        this.updateBlurMipmap = updateBlurMipmap;
    }

    public boolean isUpdateBlurMipmap() {
        return this.updateBlurMipmap;
    }

    public int getGlTextureIdSafe() {
        return RenderSystem.isOnRenderThreadOrInit() ? this.getId() : this.id;
    }
}