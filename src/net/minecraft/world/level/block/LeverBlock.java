package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LeverBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final MapCodec<LeverBlock> CODEC = simpleCodec(LeverBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    protected static final int DEPTH = 6;
    protected static final int WIDTH = 6;
    protected static final int HEIGHT = 8;
    protected static final VoxelShape NORTH_AABB = Block.box(5.0, 4.0, 10.0, 11.0, 12.0, 16.0);
    protected static final VoxelShape SOUTH_AABB = Block.box(5.0, 4.0, 0.0, 11.0, 12.0, 6.0);
    protected static final VoxelShape WEST_AABB = Block.box(10.0, 4.0, 5.0, 16.0, 12.0, 11.0);
    protected static final VoxelShape EAST_AABB = Block.box(0.0, 4.0, 5.0, 6.0, 12.0, 11.0);
    protected static final VoxelShape UP_AABB_Z = Block.box(5.0, 0.0, 4.0, 11.0, 6.0, 12.0);
    protected static final VoxelShape UP_AABB_X = Block.box(4.0, 0.0, 5.0, 12.0, 6.0, 11.0);
    protected static final VoxelShape DOWN_AABB_Z = Block.box(5.0, 10.0, 4.0, 11.0, 16.0, 12.0);
    protected static final VoxelShape DOWN_AABB_X = Block.box(4.0, 10.0, 5.0, 12.0, 16.0, 11.0);

    @Override
    public MapCodec<LeverBlock> codec() {
        return CODEC;
    }

    protected LeverBlock(BlockBehaviour.Properties p_54633_) {
        super(p_54633_);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(FACE, AttachFace.WALL)
        );
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        switch ((AttachFace)pState.getValue(FACE)) {
            case FLOOR:
                switch (pState.getValue(FACING).getAxis()) {
                    case X:
                        return UP_AABB_X;
                    case Z:
                    default:
                        return UP_AABB_Z;
                }
            case WALL:
                switch ((Direction)pState.getValue(FACING)) {
                    case EAST:
                        return EAST_AABB;
                    case WEST:
                        return WEST_AABB;
                    case SOUTH:
                        return SOUTH_AABB;
                    case NORTH:
                    default:
                        return NORTH_AABB;
                }
            case CEILING:
            default:
                switch (pState.getValue(FACING).getAxis()) {
                    case X:
                        return DOWN_AABB_X;
                    case Z:
                    default:
                        return DOWN_AABB_Z;
                }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_54640_, Level p_54641_, BlockPos p_54642_, Player p_54643_, BlockHitResult p_54645_) {
        if (p_54641_.isClientSide) {
            BlockState blockstate = p_54640_.cycle(POWERED);
            if (blockstate.getValue(POWERED)) {
                makeParticle(blockstate, p_54641_, p_54642_, 1.0F);
            }
        } else {
            this.pull(p_54640_, p_54641_, p_54642_, null);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onExplosionHit(BlockState p_309641_, ServerLevel p_367152_, BlockPos p_310069_, Explosion p_312793_, BiConsumer<ItemStack, BlockPos> p_310075_) {
        if (p_312793_.canTriggerBlocks()) {
            this.pull(p_309641_, p_367152_, p_310069_, null);
        }

        super.onExplosionHit(p_309641_, p_367152_, p_310069_, p_312793_, p_310075_);
    }

    public void pull(BlockState pState, Level pLevel, BlockPos pPos, @Nullable Player pPlayer) {
        pState = pState.cycle(POWERED);
        pLevel.setBlock(pPos, pState, 3);
        this.updateNeighbours(pState, pLevel, pPos);
        playSound(pPlayer, pLevel, pPos, pState);
        pLevel.gameEvent(pPlayer, pState.getValue(POWERED) ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pPos);
    }

    protected static void playSound(@Nullable Player pPlayer, LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        float f = pState.getValue(POWERED) ? 0.6F : 0.5F;
        pLevel.playSound(pPlayer, pPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, f);
    }

    private static void makeParticle(BlockState pState, LevelAccessor pLevel, BlockPos pPos, float pAlpha) {
        Direction direction = pState.getValue(FACING).getOpposite();
        Direction direction1 = getConnectedDirection(pState).getOpposite();
        double d0 = (double)pPos.getX() + 0.5 + 0.1 * (double)direction.getStepX() + 0.2 * (double)direction1.getStepX();
        double d1 = (double)pPos.getY() + 0.5 + 0.1 * (double)direction.getStepY() + 0.2 * (double)direction1.getStepY();
        double d2 = (double)pPos.getZ() + 0.5 + 0.1 * (double)direction.getStepZ() + 0.2 * (double)direction1.getStepZ();
        pLevel.addParticle(new DustParticleOptions(16711680, pAlpha), d0, d1, d2, 0.0, 0.0, 0.0);
    }

    @Override
    public void animateTick(BlockState p_221395_, Level p_221396_, BlockPos p_221397_, RandomSource p_221398_) {
        if (p_221395_.getValue(POWERED) && p_221398_.nextFloat() < 0.25F) {
            makeParticle(p_221395_, p_221396_, p_221397_, 0.5F);
        }
    }

    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving && !pState.is(pNewState.getBlock())) {
            if (pState.getValue(POWERED)) {
                this.updateNeighbours(pState, pLevel, pPos);
            }

            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        }
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWERED) && getConnectedDirection(pBlockState) == pSide ? 15 : 0;
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    private void updateNeighbours(BlockState pState, Level pLevel, BlockPos pPos) {
        Direction direction = getConnectedDirection(pState).getOpposite();
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(
            pLevel, direction, direction.getAxis().isHorizontal() ? Direction.UP : pState.getValue(FACING)
        );
        pLevel.updateNeighborsAt(pPos, this, orientation);
        pLevel.updateNeighborsAt(pPos.relative(direction), this, orientation);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACE, FACING, POWERED);
    }
}