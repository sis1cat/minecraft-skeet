package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RedStoneOreBlock extends Block {
    public static final MapCodec<RedStoneOreBlock> CODEC = simpleCodec(RedStoneOreBlock::new);
    public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

    @Override
    public MapCodec<RedStoneOreBlock> codec() {
        return CODEC;
    }

    public RedStoneOreBlock(BlockBehaviour.Properties p_55453_) {
        super(p_55453_);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, Boolean.valueOf(false)));
    }

    @Override
    protected void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        interact(pState, pLevel, pPos);
        super.attack(pState, pLevel, pPos, pPlayer);
    }

    @Override
    public void stepOn(Level p_154299_, BlockPos p_154300_, BlockState p_154301_, Entity p_154302_) {
        if (!p_154302_.isSteppingCarefully()) {
            interact(p_154301_, p_154299_, p_154300_);
        }

        super.stepOn(p_154299_, p_154300_, p_154301_, p_154302_);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_331692_, BlockState p_328847_, Level p_334994_, BlockPos p_336118_, Player p_329891_, InteractionHand p_331867_, BlockHitResult p_329149_
    ) {
        if (p_334994_.isClientSide) {
            spawnParticles(p_334994_, p_336118_);
        } else {
            interact(p_328847_, p_334994_, p_336118_);
        }

        return (InteractionResult)(p_331692_.getItem() instanceof BlockItem && new BlockPlaceContext(p_329891_, p_331867_, p_331692_, p_329149_).canPlace()
            ? InteractionResult.PASS
            : InteractionResult.SUCCESS);
    }

    private static void interact(BlockState pState, Level pLevel, BlockPos pPos) {
        spawnParticles(pLevel, pPos);
        if (!pState.getValue(LIT)) {
            pLevel.setBlock(pPos, pState.setValue(LIT, Boolean.valueOf(true)), 3);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState pState) {
        return pState.getValue(LIT);
    }

    @Override
    protected void randomTick(BlockState p_221918_, ServerLevel p_221919_, BlockPos p_221920_, RandomSource p_221921_) {
        if (p_221918_.getValue(LIT)) {
            p_221919_.setBlock(p_221920_, p_221918_.setValue(LIT, Boolean.valueOf(false)), 3);
        }
    }

    @Override
    protected void spawnAfterBreak(BlockState p_221907_, ServerLevel p_221908_, BlockPos p_221909_, ItemStack p_221910_, boolean p_221911_) {
        super.spawnAfterBreak(p_221907_, p_221908_, p_221909_, p_221910_, p_221911_);
        if (p_221911_) {
            this.tryDropExperience(p_221908_, p_221909_, p_221910_, UniformInt.of(1, 5));
        }
    }

    @Override
    public void animateTick(BlockState p_221913_, Level p_221914_, BlockPos p_221915_, RandomSource p_221916_) {
        if (p_221913_.getValue(LIT)) {
            spawnParticles(p_221914_, p_221915_);
        }
    }

    private static void spawnParticles(Level pLevel, BlockPos pPos) {
        double d0 = 0.5625;
        RandomSource randomsource = pLevel.random;

        for (Direction direction : Direction.values()) {
            BlockPos blockpos = pPos.relative(direction);
            if (!pLevel.getBlockState(blockpos).isSolidRender()) {
                Direction.Axis direction$axis = direction.getAxis();
                double d1 = direction$axis == Direction.Axis.X ? 0.5 + 0.5625 * (double)direction.getStepX() : (double)randomsource.nextFloat();
                double d2 = direction$axis == Direction.Axis.Y ? 0.5 + 0.5625 * (double)direction.getStepY() : (double)randomsource.nextFloat();
                double d3 = direction$axis == Direction.Axis.Z ? 0.5 + 0.5625 * (double)direction.getStepZ() : (double)randomsource.nextFloat();
                pLevel.addParticle(
                    DustParticleOptions.REDSTONE,
                    (double)pPos.getX() + d1,
                    (double)pPos.getY() + d2,
                    (double)pPos.getZ() + d3,
                    0.0,
                    0.0,
                    0.0
                );
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(LIT);
    }
}