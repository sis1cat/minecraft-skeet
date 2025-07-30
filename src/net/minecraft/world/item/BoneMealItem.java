package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class BoneMealItem extends Item {
    public static final int GRASS_SPREAD_WIDTH = 3;
    public static final int GRASS_SPREAD_HEIGHT = 1;
    public static final int GRASS_COUNT_MULTIPLIER = 3;

    public BoneMealItem(Item.Properties p_40626_) {
        super(p_40626_);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockPos blockpos1 = blockpos.relative(pContext.getClickedFace());
        if (growCrop(pContext.getItemInHand(), level, blockpos)) {
            if (!level.isClientSide) {
                pContext.getPlayer().gameEvent(GameEvent.ITEM_INTERACT_FINISH);
                level.levelEvent(1505, blockpos, 15);
            }

            return InteractionResult.SUCCESS;
        } else {
            BlockState blockstate = level.getBlockState(blockpos);
            boolean flag = blockstate.isFaceSturdy(level, blockpos, pContext.getClickedFace());
            if (flag && growWaterPlant(pContext.getItemInHand(), level, blockpos1, pContext.getClickedFace())) {
                if (!level.isClientSide) {
                    pContext.getPlayer().gameEvent(GameEvent.ITEM_INTERACT_FINISH);
                    level.levelEvent(1505, blockpos1, 15);
                }

                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    public static boolean growCrop(ItemStack pStack, Level pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        if (blockstate.getBlock() instanceof BonemealableBlock bonemealableblock && bonemealableblock.isValidBonemealTarget(pLevel, pPos, blockstate)) {
            if (pLevel instanceof ServerLevel) {
                if (bonemealableblock.isBonemealSuccess(pLevel, pLevel.random, pPos, blockstate)) {
                    bonemealableblock.performBonemeal((ServerLevel)pLevel, pLevel.random, pPos, blockstate);
                }

                pStack.shrink(1);
            }

            return true;
        }

        return false;
    }

    public static boolean growWaterPlant(ItemStack pStack, Level pLevel, BlockPos pPos, @Nullable Direction pClickedSide) {
        if (pLevel.getBlockState(pPos).is(Blocks.WATER) && pLevel.getFluidState(pPos).getAmount() == 8) {
            if (!(pLevel instanceof ServerLevel)) {
                return true;
            } else {
                RandomSource randomsource = pLevel.getRandom();

                label80:
                for (int i = 0; i < 128; i++) {
                    BlockPos blockpos = pPos;
                    BlockState blockstate = Blocks.SEAGRASS.defaultBlockState();

                    for (int j = 0; j < i / 16; j++) {
                        blockpos = blockpos.offset(
                            randomsource.nextInt(3) - 1, (randomsource.nextInt(3) - 1) * randomsource.nextInt(3) / 2, randomsource.nextInt(3) - 1
                        );
                        if (pLevel.getBlockState(blockpos).isCollisionShapeFullBlock(pLevel, blockpos)) {
                            continue label80;
                        }
                    }

                    Holder<Biome> holder = pLevel.getBiome(blockpos);
                    if (holder.is(BiomeTags.PRODUCES_CORALS_FROM_BONEMEAL)) {
                        if (i == 0 && pClickedSide != null && pClickedSide.getAxis().isHorizontal()) {
                            blockstate = BuiltInRegistries.BLOCK
                                .getRandomElementOf(BlockTags.WALL_CORALS, pLevel.random)
                                .map(p_204100_ -> p_204100_.value().defaultBlockState())
                                .orElse(blockstate);
                            if (blockstate.hasProperty(BaseCoralWallFanBlock.FACING)) {
                                blockstate = blockstate.setValue(BaseCoralWallFanBlock.FACING, pClickedSide);
                            }
                        } else if (randomsource.nextInt(4) == 0) {
                            blockstate = BuiltInRegistries.BLOCK
                                .getRandomElementOf(BlockTags.UNDERWATER_BONEMEALS, pLevel.random)
                                .map(p_204095_ -> p_204095_.value().defaultBlockState())
                                .orElse(blockstate);
                        }
                    }

                    if (blockstate.is(BlockTags.WALL_CORALS, p_359379_ -> p_359379_.hasProperty(BaseCoralWallFanBlock.FACING))) {
                        for (int k = 0; !blockstate.canSurvive(pLevel, blockpos) && k < 4; k++) {
                            blockstate = blockstate.setValue(BaseCoralWallFanBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(randomsource));
                        }
                    }

                    if (blockstate.canSurvive(pLevel, blockpos)) {
                        BlockState blockstate1 = pLevel.getBlockState(blockpos);
                        if (blockstate1.is(Blocks.WATER) && pLevel.getFluidState(blockpos).getAmount() == 8) {
                            pLevel.setBlock(blockpos, blockstate, 3);
                        } else if (blockstate1.is(Blocks.SEAGRASS)
                            && ((BonemealableBlock)Blocks.SEAGRASS).isValidBonemealTarget(pLevel, blockpos, blockstate1)
                            && randomsource.nextInt(10) == 0) {
                            ((BonemealableBlock)Blocks.SEAGRASS).performBonemeal((ServerLevel)pLevel, randomsource, blockpos, blockstate1);
                        }
                    }
                }

                pStack.shrink(1);
                return true;
            }
        } else {
            return false;
        }
    }

    public static void addGrowthParticles(LevelAccessor pLevel, BlockPos pPos, int pData) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        if (blockstate.getBlock() instanceof BonemealableBlock bonemealableblock) {
            BlockPos blockpos = bonemealableblock.getParticlePos(pPos);
            switch (bonemealableblock.getType()) {
                case NEIGHBOR_SPREADER:
                    ParticleUtils.spawnParticles(pLevel, blockpos, pData * 3, 3.0, 1.0, false, ParticleTypes.HAPPY_VILLAGER);
                    break;
                case GROWER:
                    ParticleUtils.spawnParticleInBlock(pLevel, blockpos, pData, ParticleTypes.HAPPY_VILLAGER);
            }
        } else if (blockstate.is(Blocks.WATER)) {
            ParticleUtils.spawnParticles(pLevel, pPos, pData * 3, 3.0, 1.0, false, ParticleTypes.HAPPY_VILLAGER);
        }
    }
}