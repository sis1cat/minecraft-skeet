package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChainBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<ChainBlock> CODEC = simpleCodec(ChainBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected static final float AABB_MIN = 6.5F;
    protected static final float AABB_MAX = 9.5F;
    protected static final VoxelShape Y_AXIS_AABB = Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(6.5, 6.5, 0.0, 9.5, 9.5, 16.0);
    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0, 6.5, 6.5, 16.0, 9.5, 9.5);

    @Override
    public MapCodec<ChainBlock> codec() {
        return CODEC;
    }

    public ChainBlock(BlockBehaviour.Properties p_51452_) {
        super(p_51452_);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, Boolean.valueOf(false)).setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        switch ((Direction.Axis)pState.getValue(AXIS)) {
            case X:
            default:
                return X_AXIS_AABB;
            case Z:
                return Z_AXIS_AABB;
            case Y:
                return Y_AXIS_AABB;
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        boolean flag = fluidstate.getType() == Fluids.WATER;
        return super.getStateForPlacement(pContext).setValue(WATERLOGGED, Boolean.valueOf(flag));
    }

    @Override
    protected BlockState updateShape(
        BlockState p_51461_,
        LevelReader p_361536_,
        ScheduledTickAccess p_364977_,
        BlockPos p_51465_,
        Direction p_51462_,
        BlockPos p_51466_,
        BlockState p_51463_,
        RandomSource p_367386_
    ) {
        if (p_51461_.getValue(WATERLOGGED)) {
            p_364977_.scheduleTick(p_51465_, Fluids.WATER, Fluids.WATER.getTickDelay(p_361536_));
        }

        return super.updateShape(p_51461_, p_361536_, p_364977_, p_51465_, p_51462_, p_51466_, p_51463_, p_367386_);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(WATERLOGGED).add(AXIS);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected boolean isPathfindable(BlockState p_51456_, PathComputationType p_51459_) {
        return false;
    }
}