package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Cooldown() implements RangeSelectItemModelProperty {
    public static final MapCodec<Cooldown> MAP_CODEC = MapCodec.unit(new Cooldown());

    @Override
    public float get(ItemStack p_376027_, @Nullable ClientLevel p_377074_, @Nullable LivingEntity p_376339_, int p_376029_) {
        return p_376339_ instanceof Player player ? player.getCooldowns().getCooldownPercent(p_376027_, 0.0F) : 0.0F;
    }

    @Override
    public MapCodec<Cooldown> type() {
        return MAP_CODEC;
    }
}