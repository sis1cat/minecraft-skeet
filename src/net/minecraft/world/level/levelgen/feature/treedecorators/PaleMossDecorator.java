package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingMossBlock;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

public class PaleMossDecorator extends TreeDecorator {
    public static final MapCodec<PaleMossDecorator> CODEC = RecordCodecBuilder.mapCodec(
        p_367624_ -> p_367624_.group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("leaves_probability").forGetter(p_368139_ -> p_368139_.leavesProbability),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("trunk_probability").forGetter(p_368321_ -> p_368321_.trunkProbability),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("ground_probability").forGetter(p_364066_ -> p_364066_.groundProbability)
                )
                .apply(p_367624_, PaleMossDecorator::new)
    );
    private final float leavesProbability;
    private final float trunkProbability;
    private final float groundProbability;

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.PALE_MOSS;
    }

    public PaleMossDecorator(float pLeavesProbability, float pTrunkProbability, float pGroundProbability) {
        this.leavesProbability = pLeavesProbability;
        this.trunkProbability = pTrunkProbability;
        this.groundProbability = pGroundProbability;
    }

    @Override
    public void place(TreeDecorator.Context p_366717_) {
        RandomSource randomsource = p_366717_.random();
        WorldGenLevel worldgenlevel = (WorldGenLevel)p_366717_.level();
        List<BlockPos> list = Util.shuffledCopy(p_366717_.logs(), randomsource);
        if (!list.isEmpty()) {
            Mutable<BlockPos> mutable = new MutableObject<>(list.getFirst());
            list.forEach(p_360964_ -> {
                if (p_360964_.getY() < mutable.getValue().getY()) {
                    mutable.setValue(p_360964_);
                }
            });
            BlockPos blockpos = mutable.getValue();
            if (randomsource.nextFloat() < this.groundProbability) {
                worldgenlevel.registryAccess()
                    .lookup(Registries.CONFIGURED_FEATURE)
                    .flatMap(p_375357_ -> p_375357_.get(VegetationFeatures.PALE_MOSS_PATCH))
                    .ifPresent(
                        p_370079_ -> p_370079_.value()
                                .place(worldgenlevel, worldgenlevel.getLevel().getChunkSource().getGenerator(), randomsource, blockpos.above())
                    );
            }

            p_366717_.logs().forEach(p_375360_ -> {
                if (randomsource.nextFloat() < this.trunkProbability) {
                    BlockPos blockpos1 = p_375360_.below();
                    if (p_366717_.isAir(blockpos1)) {
                        addMossHanger(blockpos1, p_366717_);
                    }
                }
            });
            p_366717_.leaves().forEach(p_364665_ -> {
                if (randomsource.nextFloat() < this.leavesProbability) {
                    BlockPos blockpos1 = p_364665_.below();
                    if (p_366717_.isAir(blockpos1)) {
                        addMossHanger(blockpos1, p_366717_);
                    }
                }
            });
        }
    }

    private static void addMossHanger(BlockPos pPos, TreeDecorator.Context pContext) {
        while (pContext.isAir(pPos.below()) && !((double)pContext.random().nextFloat() < 0.5)) {
            pContext.setBlock(pPos, Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, Boolean.valueOf(false)));
            pPos = pPos.below();
        }

        pContext.setBlock(pPos, Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, Boolean.valueOf(true)));
    }
}