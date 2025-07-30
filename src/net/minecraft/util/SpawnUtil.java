package net.minecraft.util;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnUtil {
    public static <T extends Mob> Optional<T> trySpawnMob(
        EntityType<T> pEntityType,
        EntitySpawnReason pSpawnReason,
        ServerLevel pLevel,
        BlockPos pPos,
        int pAttempts,
        int pRange,
        int pYOffset,
        SpawnUtil.Strategy pStrategy,
        boolean pCheckCollision
    ) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();

        for (int i = 0; i < pAttempts; i++) {
            int j = Mth.randomBetweenInclusive(pLevel.random, -pRange, pRange);
            int k = Mth.randomBetweenInclusive(pLevel.random, -pRange, pRange);
            blockpos$mutableblockpos.setWithOffset(pPos, j, pYOffset, k);
            if (pLevel.getWorldBorder().isWithinBounds(blockpos$mutableblockpos)
                && moveToPossibleSpawnPosition(pLevel, pYOffset, blockpos$mutableblockpos, pStrategy)
                && (
                    !pCheckCollision
                        || pLevel.noCollision(
                            pEntityType.getSpawnAABB(
                                (double)blockpos$mutableblockpos.getX() + 0.5,
                                (double)blockpos$mutableblockpos.getY(),
                                (double)blockpos$mutableblockpos.getZ() + 0.5
                            )
                        )
                )) {
                T t = (T)pEntityType.create(pLevel, null, blockpos$mutableblockpos, pSpawnReason, false, false);
                if (t != null) {
                    if (t.checkSpawnRules(pLevel, pSpawnReason) && t.checkSpawnObstruction(pLevel)) {
                        pLevel.addFreshEntityWithPassengers(t);
                        t.playAmbientSound();
                        return Optional.of(t);
                    }

                    t.discard();
                }
            }
        }

        return Optional.empty();
    }

    private static boolean moveToPossibleSpawnPosition(ServerLevel pLevel, int pYOffset, BlockPos.MutableBlockPos pPos, SpawnUtil.Strategy pStrategy) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos().set(pPos);
        BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);

        for (int i = pYOffset; i >= -pYOffset; i--) {
            pPos.move(Direction.DOWN);
            blockpos$mutableblockpos.setWithOffset(pPos, Direction.UP);
            BlockState blockstate1 = pLevel.getBlockState(pPos);
            if (pStrategy.canSpawnOn(pLevel, pPos, blockstate1, blockpos$mutableblockpos, blockstate)) {
                pPos.move(Direction.UP);
                return true;
            }

            blockstate = blockstate1;
        }

        return false;
    }

    public interface Strategy {
        @Deprecated
        SpawnUtil.Strategy LEGACY_IRON_GOLEM = (p_289751_, p_289752_, p_289753_, p_289754_, p_289755_) -> !p_289753_.is(Blocks.COBWEB)
                    && !p_289753_.is(Blocks.CACTUS)
                    && !p_289753_.is(Blocks.GLASS_PANE)
                    && !(p_289753_.getBlock() instanceof StainedGlassPaneBlock)
                    && !(p_289753_.getBlock() instanceof StainedGlassBlock)
                    && !(p_289753_.getBlock() instanceof LeavesBlock)
                    && !p_289753_.is(Blocks.CONDUIT)
                    && !p_289753_.is(Blocks.ICE)
                    && !p_289753_.is(Blocks.TNT)
                    && !p_289753_.is(Blocks.GLOWSTONE)
                    && !p_289753_.is(Blocks.BEACON)
                    && !p_289753_.is(Blocks.SEA_LANTERN)
                    && !p_289753_.is(Blocks.FROSTED_ICE)
                    && !p_289753_.is(Blocks.TINTED_GLASS)
                    && !p_289753_.is(Blocks.GLASS)
                ? (p_289755_.isAir() || p_289755_.liquid()) && (p_289753_.isSolid() || p_289753_.is(Blocks.POWDER_SNOW))
                : false;
        SpawnUtil.Strategy ON_TOP_OF_COLLIDER = (p_358812_, p_358813_, p_358814_, p_358815_, p_358816_) -> p_358816_.getCollisionShape(p_358812_, p_358815_).isEmpty()
                && Block.isFaceFull(p_358814_.getCollisionShape(p_358812_, p_358813_), Direction.UP);
        SpawnUtil.Strategy ON_TOP_OF_COLLIDER_NO_LEAVES = (p_358807_, p_358808_, p_358809_, p_358810_, p_358811_) -> p_358811_.getCollisionShape(p_358807_, p_358810_).isEmpty()
                && !p_358809_.is(BlockTags.LEAVES)
                && Block.isFaceFull(p_358809_.getCollisionShape(p_358807_, p_358808_), Direction.UP);

        boolean canSpawnOn(ServerLevel pLevel, BlockPos pTargetPos, BlockState pTargetState, BlockPos pAttemptedPos, BlockState pAttemptedState);
    }
}