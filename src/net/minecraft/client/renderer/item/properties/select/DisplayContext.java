package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DisplayContext() implements SelectItemModelProperty<ItemDisplayContext> {
    public static final SelectItemModelProperty.Type<DisplayContext, ItemDisplayContext> TYPE = SelectItemModelProperty.Type.create(
        MapCodec.unit(new DisplayContext()), ItemDisplayContext.CODEC
    );

    public ItemDisplayContext get(
        ItemStack p_377542_, @Nullable ClientLevel p_376709_, @Nullable LivingEntity p_376980_, int p_377595_, ItemDisplayContext p_376417_
    ) {
        return p_376417_;
    }

    @Override
    public SelectItemModelProperty.Type<DisplayContext, ItemDisplayContext> type() {
        return TYPE;
    }
}