package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EmptyModel implements ItemModel {
    public static final ItemModel INSTANCE = new EmptyModel();

    @Override
    public void update(
        ItemStackRenderState p_376944_,
        ItemStack p_378117_,
        ItemModelResolver p_377464_,
        ItemDisplayContext p_378694_,
        @Nullable ClientLevel p_376347_,
        @Nullable LivingEntity p_377973_,
        int p_375552_
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    public static record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<EmptyModel.Unbaked> MAP_CODEC = MapCodec.unit(EmptyModel.Unbaked::new);

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_376461_) {
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_377224_) {
            return EmptyModel.INSTANCE;
        }

        @Override
        public MapCodec<EmptyModel.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}