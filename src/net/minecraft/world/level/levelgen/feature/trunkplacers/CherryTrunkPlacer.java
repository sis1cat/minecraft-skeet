package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class CherryTrunkPlacer extends TrunkPlacer {
    private static final Codec<UniformInt> BRANCH_START_CODEC = UniformInt.CODEC
        .codec()
        .validate(
            p_275181_ -> p_275181_.getMaxValue() - p_275181_.getMinValue() < 1
                    ? DataResult.error(() -> "Need at least 2 blocks variation for the branch starts to fit both branches")
                    : DataResult.success(p_275181_)
        );
    public static final MapCodec<CherryTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
        p_327472_ -> trunkPlacerParts(p_327472_)
                .and(
                    p_327472_.group(
                        IntProvider.codec(1, 3).fieldOf("branch_count").forGetter(p_272644_ -> p_272644_.branchCount),
                        IntProvider.codec(2, 16).fieldOf("branch_horizontal_length").forGetter(p_273612_ -> p_273612_.branchHorizontalLength),
                        IntProvider.validateCodec(-16, 0, BRANCH_START_CODEC).fieldOf("branch_start_offset_from_top").forGetter(p_272705_ -> p_272705_.branchStartOffsetFromTop),
                        IntProvider.codec(-16, 16).fieldOf("branch_end_offset_from_top").forGetter(p_273633_ -> p_273633_.branchEndOffsetFromTop)
                    )
                )
                .apply(p_327472_, CherryTrunkPlacer::new)
    );
    private final IntProvider branchCount;
    private final IntProvider branchHorizontalLength;
    private final UniformInt branchStartOffsetFromTop;
    private final UniformInt secondBranchStartOffsetFromTop;
    private final IntProvider branchEndOffsetFromTop;

    public CherryTrunkPlacer(
        int pBaseHeight, int pHeightRandA, int pHeightRandB, IntProvider pBranchCount, IntProvider pBranchHorizontalLength, UniformInt pBranchStartOffsetFromTop, IntProvider pBranchEndOffsetFromTop
    ) {
        super(pBaseHeight, pHeightRandA, pHeightRandB);
        this.branchCount = pBranchCount;
        this.branchHorizontalLength = pBranchHorizontalLength;
        this.branchStartOffsetFromTop = pBranchStartOffsetFromTop;
        this.secondBranchStartOffsetFromTop = UniformInt.of(pBranchStartOffsetFromTop.getMinValue(), pBranchStartOffsetFromTop.getMaxValue() - 1);
        this.branchEndOffsetFromTop = pBranchEndOffsetFromTop;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.CHERRY_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(
        LevelSimulatedReader p_272827_,
        BiConsumer<BlockPos, BlockState> p_272650_,
        RandomSource p_272993_,
        int p_272990_,
        BlockPos p_273471_,
        TreeConfiguration p_273355_
    ) {
        setDirtAt(p_272827_, p_272650_, p_272993_, p_273471_.below(), p_273355_);
        int i = Math.max(0, p_272990_ - 1 + this.branchStartOffsetFromTop.sample(p_272993_));
        int j = Math.max(0, p_272990_ - 1 + this.secondBranchStartOffsetFromTop.sample(p_272993_));
        if (j >= i) {
            j++;
        }

        int k = this.branchCount.sample(p_272993_);
        boolean flag = k == 3;
        boolean flag1 = k >= 2;
        int l;
        if (flag) {
            l = p_272990_;
        } else if (flag1) {
            l = Math.max(i, j) + 1;
        } else {
            l = i + 1;
        }

        for (int i1 = 0; i1 < l; i1++) {
            this.placeLog(p_272827_, p_272650_, p_272993_, p_273471_.above(i1), p_273355_);
        }

        List<FoliagePlacer.FoliageAttachment> list = new ArrayList<>();
        if (flag) {
            list.add(new FoliagePlacer.FoliageAttachment(p_273471_.above(l), 0, false));
        }

        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_272993_);
        Function<BlockState, BlockState> function = p_360615_ -> p_360615_.trySetValue(RotatedPillarBlock.AXIS, direction.getAxis());
        list.add(this.generateBranch(p_272827_, p_272650_, p_272993_, p_272990_, p_273471_, p_273355_, function, direction, i, i < l - 1, blockpos$mutableblockpos));
        if (flag1) {
            list.add(
                this.generateBranch(
                    p_272827_, p_272650_, p_272993_, p_272990_, p_273471_, p_273355_, function, direction.getOpposite(), j, j < l - 1, blockpos$mutableblockpos
                )
            );
        }

        return list;
    }

    private FoliagePlacer.FoliageAttachment generateBranch(
        LevelSimulatedReader pLevel,
        BiConsumer<BlockPos, BlockState> pBlockSetter,
        RandomSource pRandom,
        int pFreeTreeHeight,
        BlockPos pPos,
        TreeConfiguration pConfig,
        Function<BlockState, BlockState> pPropertySetter,
        Direction pDirection,
        int pSecondBranchStartOffsetFromTop,
        boolean pDoubleBranch,
        BlockPos.MutableBlockPos pCurrentPos
    ) {
        pCurrentPos.set(pPos).move(Direction.UP, pSecondBranchStartOffsetFromTop);
        int i = pFreeTreeHeight - 1 + this.branchEndOffsetFromTop.sample(pRandom);
        boolean flag = pDoubleBranch || i < pSecondBranchStartOffsetFromTop;
        int j = this.branchHorizontalLength.sample(pRandom) + (flag ? 1 : 0);
        BlockPos blockpos = pPos.relative(pDirection, j).above(i);
        int k = flag ? 2 : 1;

        for (int l = 0; l < k; l++) {
            this.placeLog(pLevel, pBlockSetter, pRandom, pCurrentPos.move(pDirection), pConfig, pPropertySetter);
        }

        Direction direction = blockpos.getY() > pCurrentPos.getY() ? Direction.UP : Direction.DOWN;

        while (true) {
            int i1 = pCurrentPos.distManhattan(blockpos);
            if (i1 == 0) {
                return new FoliagePlacer.FoliageAttachment(blockpos.above(), 0, false);
            }

            float f = (float)Math.abs(blockpos.getY() - pCurrentPos.getY()) / (float)i1;
            boolean flag1 = pRandom.nextFloat() < f;
            pCurrentPos.move(flag1 ? direction : pDirection);
            this.placeLog(pLevel, pBlockSetter, pRandom, pCurrentPos, pConfig, flag1 ? Function.identity() : pPropertySetter);
        }
    }
}