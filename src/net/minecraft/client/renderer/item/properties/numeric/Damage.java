package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Damage(boolean normalize) implements RangeSelectItemModelProperty {
    public static final MapCodec<Damage> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_376348_ -> p_376348_.group(Codec.BOOL.optionalFieldOf("normalize", Boolean.valueOf(true)).forGetter(Damage::normalize)).apply(p_376348_, Damage::new)
    );

    @Override
    public float get(ItemStack p_376952_, @Nullable ClientLevel p_376292_, @Nullable LivingEntity p_376246_, int p_375934_) {
        float f = (float)p_376952_.getDamageValue();
        float f1 = (float)p_376952_.getMaxDamage();
        return this.normalize ? Mth.clamp(f / f1, 0.0F, 1.0F) : Mth.clamp(f, 0.0F, f1);
    }

    @Override
    public MapCodec<Damage> type() {
        return MAP_CODEC;
    }
}