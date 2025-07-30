package net.minecraft.client.renderer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SelectItemModel<T> implements ItemModel {
    private final SelectItemModelProperty<T> property;
    private final Object2ObjectMap<T, ItemModel> models;

    public SelectItemModel(SelectItemModelProperty<T> pProperty, Object2ObjectMap<T, ItemModel> pModels) {
        this.property = pProperty;
        this.models = pModels;
    }

    @Override
    public void update(
        ItemStackRenderState p_377497_,
        ItemStack p_377617_,
        ItemModelResolver p_377247_,
        ItemDisplayContext p_376949_,
        @Nullable ClientLevel p_375690_,
        @Nullable LivingEntity p_377359_,
        int p_377086_
    ) {
        T t = this.property.get(p_377617_, p_375690_, p_377359_, p_377086_, p_376949_);
        ItemModel itemmodel = this.models.get(t);
        if (itemmodel != null) {
            itemmodel.update(p_377497_, p_377617_, p_377247_, p_376949_, p_375690_, p_377359_, p_377086_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record SwitchCase<T>(List<T> values, ItemModel.Unbaked model) {
        public static <T> Codec<SelectItemModel.SwitchCase<T>> codec(Codec<T> pCodec) {
            return RecordCodecBuilder.create(
                p_377942_ -> p_377942_.group(
                            ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(pCodec)).fieldOf("when").forGetter(SelectItemModel.SwitchCase::values),
                            ItemModels.CODEC.fieldOf("model").forGetter(SelectItemModel.SwitchCase::model)
                        )
                        .apply(p_377942_, SelectItemModel.SwitchCase::new)
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record Unbaked(SelectItemModel.UnbakedSwitch<?, ?> unbakedSwitch, Optional<ItemModel.Unbaked> fallback) implements ItemModel.Unbaked {
        public static final MapCodec<SelectItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_375671_ -> p_375671_.group(
                        SelectItemModel.UnbakedSwitch.MAP_CODEC.forGetter(SelectItemModel.Unbaked::unbakedSwitch),
                        ItemModels.CODEC.optionalFieldOf("fallback").forGetter(SelectItemModel.Unbaked::fallback)
                    )
                    .apply(p_375671_, SelectItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<SelectItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_377555_) {
            ItemModel itemmodel = this.fallback.<ItemModel>map(p_378040_ -> p_378040_.bake(p_377555_)).orElse(p_377555_.missingItemModel());
            return this.unbakedSwitch.bake(p_377555_, itemmodel);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_377738_) {
            this.unbakedSwitch.resolveDependencies(p_377738_);
            this.fallback.ifPresent(p_376269_ -> p_376269_.resolveDependencies(p_377738_));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record UnbakedSwitch<P extends SelectItemModelProperty<T>, T>(P property, List<SelectItemModel.SwitchCase<T>> cases) {
        public static final MapCodec<SelectItemModel.UnbakedSwitch<?, ?>> MAP_CODEC = SelectItemModelProperties.CODEC
            .dispatchMap("property", p_377103_ -> p_377103_.property().type(), SelectItemModelProperty.Type::switchCodec);

        public ItemModel bake(ItemModel.BakingContext pBakingContext, ItemModel pModel) {
            Object2ObjectMap<T, ItemModel> object2objectmap = new Object2ObjectOpenHashMap<>();

            for (SelectItemModel.SwitchCase<T> switchcase : this.cases) {
                ItemModel.Unbaked itemmodel$unbaked = switchcase.model;
                ItemModel itemmodel = itemmodel$unbaked.bake(pBakingContext);

                for (T t : switchcase.values) {
                    object2objectmap.put(t, itemmodel);
                }
            }

            object2objectmap.defaultReturnValue(pModel);
            return new SelectItemModel<>(this.property, object2objectmap);
        }

        public void resolveDependencies(ResolvableModel.Resolver pResolver) {
            for (SelectItemModel.SwitchCase<?> switchcase : this.cases) {
                switchcase.model.resolveDependencies(pResolver);
            }
        }
    }
}