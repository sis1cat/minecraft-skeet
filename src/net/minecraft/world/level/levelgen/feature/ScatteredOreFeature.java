package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class ScatteredOreFeature extends Feature<OreConfiguration> {
    private static final int MAX_DIST_FROM_ORIGIN = 7;

    ScatteredOreFeature(Codec<OreConfiguration> p_160304_) {
        super(p_160304_);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> p_160306_) {
        WorldGenLevel worldgenlevel = p_160306_.level();
        RandomSource randomsource = p_160306_.random();
        OreConfiguration oreconfiguration = p_160306_.config();
        BlockPos blockpos = p_160306_.origin();
        int i = randomsource.nextInt(oreconfiguration.size + 1);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int j = 0; j < i; j++) {
            this.offsetTargetPos(blockpos$mutableblockpos, randomsource, blockpos, Math.min(j, 7));
            BlockState blockstate = worldgenlevel.getBlockState(blockpos$mutableblockpos);

            for (OreConfiguration.TargetBlockState oreconfiguration$targetblockstate : oreconfiguration.targetStates) {
                if (OreFeature.canPlaceOre(
                    blockstate, worldgenlevel::getBlockState, randomsource, oreconfiguration, oreconfiguration$targetblockstate, blockpos$mutableblockpos
                )) {
                    worldgenlevel.setBlock(blockpos$mutableblockpos, oreconfiguration$targetblockstate.state, 2);
                    break;
                }
            }
        }

        return true;
    }

    private void offsetTargetPos(BlockPos.MutableBlockPos pMutablePos, RandomSource pRandom, BlockPos pPos, int pMagnitude) {
        int i = this.getRandomPlacementInOneAxisRelativeToOrigin(pRandom, pMagnitude);
        int j = this.getRandomPlacementInOneAxisRelativeToOrigin(pRandom, pMagnitude);
        int k = this.getRandomPlacementInOneAxisRelativeToOrigin(pRandom, pMagnitude);
        pMutablePos.setWithOffset(pPos, i, j, k);
    }

    private int getRandomPlacementInOneAxisRelativeToOrigin(RandomSource pRandom, int pMagnitude) {
        return Math.round((pRandom.nextFloat() - pRandom.nextFloat()) * (float)pMagnitude);
    }
}