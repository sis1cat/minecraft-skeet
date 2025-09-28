package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SpecialModelWrapper<T> implements ItemModel {
    private final SpecialModelRenderer<T> specialRenderer;
    private final BakedModel baseModel;

    public SpecialModelWrapper(SpecialModelRenderer<T> pSpecialRenderer, BakedModel pBaseModel) {
        this.specialRenderer = pSpecialRenderer;
        this.baseModel = pBaseModel;
    }

    @Override
    public void update(
        ItemStackRenderState p_376096_,
        ItemStack p_376294_,
        ItemModelResolver p_377226_,
        ItemDisplayContext p_377206_,
        @Nullable ClientLevel p_375445_,
        @Nullable LivingEntity p_375829_,
        int p_375847_
    ) {
        ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = p_376096_.newLayer();
        itemstackrenderstate$layerrenderstate.setItemStack(p_376294_);
        if (p_376294_.hasFoil()) {
            itemstackrenderstate$layerrenderstate.setFoilType(ItemStackRenderState.FoilType.STANDARD);
        }

        itemstackrenderstate$layerrenderstate.setupSpecialModel(this.specialRenderer, this.specialRenderer.extractArgument(p_376294_), this.baseModel);
    }

    public static record Unbaked(ResourceLocation base, SpecialModelRenderer.Unbaked specialModel) implements ItemModel.Unbaked {
        public static final MapCodec<SpecialModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instanceIn -> instanceIn.group(
                        ResourceLocation.CODEC.fieldOf("base").forGetter(SpecialModelWrapper.Unbaked::base),
                        SpecialModelRenderers.CODEC.fieldOf("model").forGetter(SpecialModelWrapper.Unbaked::specialModel)
                    )
                    .apply(instanceIn, SpecialModelWrapper.Unbaked::new)
        );

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_377714_) {
            p_377714_.resolve(this.base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_378066_) {
            BakedModel bakedmodel = p_378066_.bake(this.base);
            SpecialModelRenderer<?> specialmodelrenderer = this.specialModel.bake(p_378066_.entityModelSet());
            return (ItemModel)(specialmodelrenderer == null ? p_378066_.missingItemModel() : new SpecialModelWrapper<>(specialmodelrenderer, bakedmodel));
        }

        @Override
        public MapCodec<SpecialModelWrapper.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}