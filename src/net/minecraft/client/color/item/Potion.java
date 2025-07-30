package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.optifine.Config;
import net.optifine.CustomColors;

public record Potion(int defaultColor) implements ItemTintSource {
    public static final MapCodec<Potion> MAP_CODEC = RecordCodecBuilder.mapCodec(
        potionIn -> potionIn.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(Potion::defaultColor)).apply(potionIn, Potion::new)
    );

    public Potion() {
        this(-13083194);
    }

    @Override
    public int calculate(ItemStack p_377068_, @Nullable ClientLevel p_375615_, @Nullable LivingEntity p_376005_) {
        PotionContents potioncontents = p_377068_.get(DataComponents.POTION_CONTENTS);
        int i = Config.isCustomColors() ? CustomColors.getPotionColor(0, this.defaultColor) : this.defaultColor;
        return potioncontents != null ? ARGB.opaque(potioncontents.getColorOr(i)) : ARGB.opaque(i);
    }

    @Override
    public MapCodec<Potion> type() {
        return MAP_CODEC;
    }
}