package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.optifine.entity.model.CustomEntityModels;

public class SkullSpecialRenderer implements SpecialModelRenderer<ResolvableProfile> {
    private final SkullBlock.Type skullType;
    private SkullModelBase model;
    @Nullable
    private final ResourceLocation textureOverride;
    private final float animation;
    private boolean modelUpdated;

    public SkullSpecialRenderer(SkullBlock.Type pSkullType, SkullModelBase pModel, @Nullable ResourceLocation pTextureOverride, float pAnimation) {
        this.skullType = pSkullType;
        this.model = pModel;
        this.textureOverride = pTextureOverride;
        this.animation = pAnimation;
    }

    @Nullable
    public ResolvableProfile extractArgument(ItemStack p_376567_) {
        return p_376567_.get(DataComponents.PROFILE);
    }

    public void render(
        @Nullable ResolvableProfile p_377678_,
        ItemDisplayContext p_378440_,
        PoseStack p_377644_,
        MultiBufferSource p_375574_,
        int p_376639_,
        int p_376976_,
        boolean p_378372_
    ) {
        if (CustomEntityModels.isActive() && !this.modelUpdated) {
            this.model = SkullBlockRenderer.getGlobalModels().getOrDefault(this.skullType, this.model);
            this.modelUpdated = true;
        }

        RenderType rendertype = SkullBlockRenderer.getRenderType(this.skullType, p_377678_, this.textureOverride);
        SkullBlockRenderer.renderSkull(null, 180.0F, this.animation, p_377644_, p_375574_, p_376639_, this.model, rendertype);
    }

    public static record Unbaked(SkullBlock.Type kind, Optional<ResourceLocation> textureOverride, float animation) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<SkullSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_373895_0_ -> p_373895_0_.group(
                        SkullBlock.Type.CODEC.fieldOf("kind").forGetter(SkullSpecialRenderer.Unbaked::kind),
                        ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(SkullSpecialRenderer.Unbaked::textureOverride),
                        Codec.FLOAT.optionalFieldOf("animation", Float.valueOf(0.0F)).forGetter(SkullSpecialRenderer.Unbaked::animation)
                    )
                    .apply(p_373895_0_, SkullSpecialRenderer.Unbaked::new)
        );

        public Unbaked(SkullBlock.Type pType) {
            this(pType, Optional.empty(), 0.0F);
        }

        @Override
        public MapCodec<SkullSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Nullable
        @Override
        public SpecialModelRenderer<?> bake(EntityModelSet p_376016_) {
            SkullModelBase skullmodelbase = SkullBlockRenderer.createModel(p_376016_, this.kind);
            ResourceLocation resourcelocation = this.textureOverride
                .<ResourceLocation>map(p_373233_0_ -> p_373233_0_.withPath(p_373501_0_ -> "textures/entity/" + p_373501_0_ + ".png"))
                .orElse(null);
            return skullmodelbase != null ? new SkullSpecialRenderer(this.kind, skullmodelbase, resourcelocation, this.animation) : null;
        }
    }
}