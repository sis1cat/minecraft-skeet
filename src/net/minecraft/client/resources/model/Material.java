package net.minecraft.client.resources.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.optifine.render.RenderUtils;
import net.optifine.util.TextureUtils;

public class Material {
    public static final Comparator<Material> COMPARATOR = Comparator.comparing(Material::atlasLocation).thenComparing(Material::texture);
    private final ResourceLocation atlasLocation;
    private final ResourceLocation texture;
    @Nullable
    private RenderType renderType;

    public Material(ResourceLocation pAtlasLocation, ResourceLocation pTexture) {
        this.atlasLocation = pAtlasLocation;
        this.texture = pTexture;
    }

    public ResourceLocation atlasLocation() {
        return this.atlasLocation;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public TextureAtlasSprite sprite() {
        TextureAtlasSprite textureatlassprite = Minecraft.getInstance().getTextureAtlas(this.atlasLocation()).apply(this.texture());
        return TextureUtils.getCustomSprite(textureatlassprite);
    }

    public RenderType renderType(Function<ResourceLocation, RenderType> pRenderTypeGetter) {
        if (this.renderType == null) {
            this.renderType = pRenderTypeGetter.apply(this.atlasLocation);
        }

        return this.renderType;
    }

    public VertexConsumer buffer(MultiBufferSource pBufferSource, Function<ResourceLocation, RenderType> pRenderTypeGetter) {
        TextureAtlasSprite textureatlassprite = this.sprite();
        RenderType rendertype = this.renderType(pRenderTypeGetter);
        if (textureatlassprite.isSpriteEmissive && rendertype.isEntitySolid()) {
            RenderUtils.flushRenderBuffers();
            rendertype = RenderType.entityCutout(this.atlasLocation);
        }

        return textureatlassprite.wrap(pBufferSource.getBuffer(rendertype));
    }

    public VertexConsumer buffer(MultiBufferSource pBufferSource, Function<ResourceLocation, RenderType> pRenderTypeGetter, boolean pNoEntity, boolean pWithGlint) {
        return this.sprite().wrap(ItemRenderer.getFoilBuffer(pBufferSource, this.renderType(pRenderTypeGetter), pNoEntity, pWithGlint));
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else if (pOther != null && this.getClass() == pOther.getClass()) {
            Material material = (Material)pOther;
            return this.atlasLocation.equals(material.atlasLocation) && this.texture.equals(material.texture);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.atlasLocation, this.texture);
    }

    @Override
    public String toString() {
        return "Material{atlasLocation=" + this.atlasLocation + ", texture=" + this.texture + "}";
    }
}