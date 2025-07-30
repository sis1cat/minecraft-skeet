package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.optifine.Config;
import net.optifine.shaders.ShadersTex;
import org.slf4j.Logger;

public class DynamicTexture extends AbstractTexture implements Dumpable {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private NativeImage pixels;
    private boolean capeTexture;
    private boolean elytraCapeTexture;

    public DynamicTexture(NativeImage pPixels) {
        this.pixels = pPixels;
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                TextureUtil.prepareImage(this.getId(), this.pixels.getWidth(), this.pixels.getHeight());
                this.upload();
                if (Config.isShaders()) {
                    ShadersTex.initDynamicTextureNS(this);
                }
            });
        } else {
            TextureUtil.prepareImage(this.getId(), this.pixels.getWidth(), this.pixels.getHeight());
            this.upload();
            if (Config.isShaders()) {
                ShadersTex.initDynamicTextureNS(this);
            }
        }
    }

    public DynamicTexture(int pWidth, int pHeight, boolean pUseCalloc) {
        this.pixels = new NativeImage(pWidth, pHeight, pUseCalloc);
        TextureUtil.prepareImage(this.getId(), this.pixels.getWidth(), this.pixels.getHeight());
        if (Config.isShaders()) {
            ShadersTex.initDynamicTextureNS(this);
        }
    }

    public void upload() {
        if (this.pixels != null) {
            this.bind();
            this.pixels.upload(0, 0, 0, false);
        } else {
            LOGGER.warn("Trying to upload disposed texture {}", this.getId());
        }
    }

    @Nullable
    public NativeImage getPixels() {
        return this.pixels;
    }

    public void setPixels(NativeImage pPixels) {
        if (this.pixels != null) {
            this.pixels.close();
        }

        this.pixels = pPixels;
    }

    @Override
    public void close() {
        if (this.pixels != null) {
            this.pixels.close();
            this.releaseId();
            this.pixels = null;
        }
    }

    @Override
    public void dumpContents(ResourceLocation p_276119_, Path p_276105_) throws IOException {
        if (this.pixels != null) {
            String s = p_276119_.toDebugFileName() + ".png";
            Path path = p_276105_.resolve(s);
            this.pixels.writeToFile(path);
        }
    }

    public boolean isCapeTexture() {
        return this.capeTexture;
    }

    public void setCapeTexture(boolean capeTexture) {
        this.capeTexture = capeTexture;
    }

    public boolean isElytraCapeTexture() {
        return this.elytraCapeTexture;
    }

    public void setElytraCapeTexture(boolean elytraCapeTexture) {
        this.elytraCapeTexture = elytraCapeTexture;
    }
}