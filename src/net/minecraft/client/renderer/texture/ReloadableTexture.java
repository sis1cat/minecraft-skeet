package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.Config;
import net.optifine.EmissiveTextures;
import net.optifine.shaders.ShadersTex;

public abstract class ReloadableTexture extends AbstractTexture {
    private final ResourceLocation resourceId;
    private ResourceManager resourceManager = Config.getResourceManager();
    public ResourceLocation locationEmissive;
    public boolean isEmissive;
    public long size;

    public ReloadableTexture(ResourceLocation pResourceId) {
        this.resourceId = pResourceId;
    }

    public ResourceLocation resourceId() {
        return this.resourceId;
    }

    public void apply(TextureContents pTextureContents) {
        boolean flag = pTextureContents.clamp();
        boolean flag1 = pTextureContents.blur();
        this.defaultBlur = flag1;
        NativeImage nativeimage = pTextureContents.image();
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> this.doLoad(nativeimage, flag1, flag));
        } else {
            this.doLoad(nativeimage, flag1, flag);
        }
    }

    private void doLoad(NativeImage pImage, boolean pBlur, boolean pClamp) {
        TextureUtil.prepareImage(this.getId(), 0, pImage.getWidth(), pImage.getHeight());
        this.setFilter(pBlur, false);
        this.setClamp(pClamp);
        pImage.upload(0, 0, 0, 0, 0, pImage.getWidth(), pImage.getHeight(), true);
        if (Config.isShaders()) {
            ShadersTex.loadSimpleTextureNS(this.getId(), pImage, pBlur, pClamp, this.resourceManager, this.resourceId, this.getMultiTexID());
        }

        if (EmissiveTextures.isActive()) {
            EmissiveTextures.loadTexture(this.resourceId, this);
        }

        this.size = pImage.getSize();
    }

    public abstract TextureContents loadContents(ResourceManager pResourceManager) throws IOException;
}