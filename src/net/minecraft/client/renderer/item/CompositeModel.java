package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompositeModel implements ItemModel {
    private final List<ItemModel> models;

    public CompositeModel(List<ItemModel> pModels) {
        this.models = pModels;
    }

    @Override
    public void update(
        ItemStackRenderState p_375844_,
        ItemStack p_378218_,
        ItemModelResolver p_376601_,
        ItemDisplayContext p_376240_,
        @Nullable ClientLevel p_376097_,
        @Nullable LivingEntity p_378210_,
        int p_377487_
    ) {
        p_375844_.ensureCapacity(this.models.size());

        for (ItemModel itemmodel : this.models) {
            itemmodel.update(p_375844_, p_378218_, p_376601_, p_376240_, p_376097_, p_378210_, p_377487_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record Unbaked(List<ItemModel.Unbaked> models) implements ItemModel.Unbaked {
        public static final MapCodec<CompositeModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_375773_ -> p_375773_.group(ItemModels.CODEC.listOf().fieldOf("models").forGetter(CompositeModel.Unbaked::models))
                    .apply(p_375773_, CompositeModel.Unbaked::new)
        );

        @Override
        public MapCodec<CompositeModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_375395_) {
            for (ItemModel.Unbaked itemmodel$unbaked : this.models) {
                itemmodel$unbaked.resolveDependencies(p_375395_);
            }
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_377746_) {
            return new CompositeModel(this.models.stream().map(p_377328_ -> p_377328_.bake(p_377746_)).toList());
        }
    }
}