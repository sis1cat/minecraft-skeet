package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;

public class HasSturdyFacePredicate implements BlockPredicate {
    private final Vec3i offset;
    private final Direction direction;
    public static final MapCodec<HasSturdyFacePredicate> CODEC = RecordCodecBuilder.mapCodec(
        p_198327_ -> p_198327_.group(
                    Vec3i.offsetCodec(16).optionalFieldOf("offset", Vec3i.ZERO).forGetter(p_198331_ -> p_198331_.offset),
                    Direction.CODEC.fieldOf("direction").forGetter(p_198329_ -> p_198329_.direction)
                )
                .apply(p_198327_, HasSturdyFacePredicate::new)
    );

    public HasSturdyFacePredicate(Vec3i pOffset, Direction pDirection) {
        this.offset = pOffset;
        this.direction = pDirection;
    }

    public boolean test(WorldGenLevel pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.offset(this.offset);
        return pLevel.getBlockState(blockpos).isFaceSturdy(pLevel, blockpos, this.direction);
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.HAS_STURDY_FACE;
    }
}