package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public abstract class CoralFeature extends Feature<NoneFeatureConfiguration> {
    public CoralFeature(Codec<NoneFeatureConfiguration> p_65429_) {
        super(p_65429_);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_159536_) {
        RandomSource randomsource = p_159536_.random();
        WorldGenLevel worldgenlevel = p_159536_.level();
        BlockPos blockpos = p_159536_.origin();
        Optional<Block> optional = BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.CORAL_BLOCKS, randomsource).map(Holder::value);
        return optional.isEmpty() ? false : this.placeFeature(worldgenlevel, randomsource, blockpos, optional.get().defaultBlockState());
    }

    protected abstract boolean placeFeature(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState);

    protected boolean placeCoralBlock(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        BlockPos blockpos = pPos.above();
        BlockState blockstate = pLevel.getBlockState(pPos);
        if ((blockstate.is(Blocks.WATER) || blockstate.is(BlockTags.CORALS)) && pLevel.getBlockState(blockpos).is(Blocks.WATER)) {
            pLevel.setBlock(pPos, pState, 3);
            if (pRandom.nextFloat() < 0.25F) {
                BuiltInRegistries.BLOCK
                    .getRandomElementOf(BlockTags.CORALS, pRandom)
                    .map(Holder::value)
                    .ifPresent(p_204720_ -> pLevel.setBlock(blockpos, p_204720_.defaultBlockState(), 2));
            } else if (pRandom.nextFloat() < 0.05F) {
                pLevel.setBlock(blockpos, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, Integer.valueOf(pRandom.nextInt(4) + 1)), 2);
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (pRandom.nextFloat() < 0.2F) {
                    BlockPos blockpos1 = pPos.relative(direction);
                    if (pLevel.getBlockState(blockpos1).is(Blocks.WATER)) {
                        BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.WALL_CORALS, pRandom).map(Holder::value).ifPresent(p_360600_ -> {
                            BlockState blockstate1 = p_360600_.defaultBlockState();
                            if (blockstate1.hasProperty(BaseCoralWallFanBlock.FACING)) {
                                blockstate1 = blockstate1.setValue(BaseCoralWallFanBlock.FACING, direction);
                            }

                            pLevel.setBlock(blockpos1, blockstate1, 2);
                        });
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }
}