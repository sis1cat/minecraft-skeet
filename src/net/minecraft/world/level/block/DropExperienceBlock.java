package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class DropExperienceBlock extends Block {
    public static final MapCodec<DropExperienceBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360430_ -> p_360430_.group(IntProvider.codec(0, 10).fieldOf("experience").forGetter(p_311138_ -> p_311138_.xpRange), propertiesCodec())
                .apply(p_360430_, DropExperienceBlock::new)
    );
    private final IntProvider xpRange;

    @Override
    public MapCodec<? extends DropExperienceBlock> codec() {
        return CODEC;
    }

    public DropExperienceBlock(IntProvider pXpRange, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.xpRange = pXpRange;
    }

    @Override
    protected void spawnAfterBreak(BlockState p_221086_, ServerLevel p_221087_, BlockPos p_221088_, ItemStack p_221089_, boolean p_221090_) {
        super.spawnAfterBreak(p_221086_, p_221087_, p_221088_, p_221089_, p_221090_);
        if (p_221090_) {
            this.tryDropExperience(p_221087_, p_221088_, p_221089_, this.xpRange);
        }
    }
}