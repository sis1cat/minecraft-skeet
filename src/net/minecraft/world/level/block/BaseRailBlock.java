package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseRailBlock extends Block implements SimpleWaterloggedBlock {
    protected static final VoxelShape FLAT_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    protected static final VoxelShape HALF_BLOCK_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final boolean isStraight;

    public static boolean isRail(Level pLevel, BlockPos pPos) {
        return isRail(pLevel.getBlockState(pPos));
    }

    public static boolean isRail(BlockState pState) {
        return pState.is(BlockTags.RAILS) && pState.getBlock() instanceof BaseRailBlock;
    }

    protected BaseRailBlock(boolean pIsStraight, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.isStraight = pIsStraight;
    }

    @Override
    protected abstract MapCodec<? extends BaseRailBlock> codec();

    public boolean isStraight() {
        return this.isStraight;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        RailShape railshape = pState.is(this) ? pState.getValue(this.getShapeProperty()) : null;
        return railshape != null && railshape.isSlope() ? HALF_BLOCK_AABB : FLAT_AABB;
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return canSupportRigidBlock(pLevel, pPos.below());
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pOldState.is(pState.getBlock())) {
            this.updateState(pState, pLevel, pPos, pIsMoving);
        }
    }

    protected BlockState updateState(BlockState pState, Level pLevel, BlockPos pPos, boolean pMovedByPiston) {
        pState = this.updateDir(pLevel, pPos, pState, true);
        if (this.isStraight) {
            pLevel.neighborChanged(pState, pPos, this, null, pMovedByPiston);
        }

        return pState;
    }

    @Override
    protected void neighborChanged(BlockState p_49377_, Level p_49378_, BlockPos p_49379_, Block p_49380_, @Nullable Orientation p_362860_, boolean p_49382_) {
        if (!p_49378_.isClientSide && p_49378_.getBlockState(p_49379_).is(this)) {
            RailShape railshape = p_49377_.getValue(this.getShapeProperty());
            if (shouldBeRemoved(p_49379_, p_49378_, railshape)) {
                dropResources(p_49377_, p_49378_, p_49379_);
                p_49378_.removeBlock(p_49379_, p_49382_);
            } else {
                this.updateState(p_49377_, p_49378_, p_49379_, p_49380_);
            }
        }
    }

    private static boolean shouldBeRemoved(BlockPos pPos, Level pLevel, RailShape pShape) {
        if (!canSupportRigidBlock(pLevel, pPos.below())) {
            return true;
        } else {
            switch (pShape) {
                case ASCENDING_EAST:
                    return !canSupportRigidBlock(pLevel, pPos.east());
                case ASCENDING_WEST:
                    return !canSupportRigidBlock(pLevel, pPos.west());
                case ASCENDING_NORTH:
                    return !canSupportRigidBlock(pLevel, pPos.north());
                case ASCENDING_SOUTH:
                    return !canSupportRigidBlock(pLevel, pPos.south());
                default:
                    return false;
            }
        }
    }

    protected void updateState(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock) {
    }

    protected BlockState updateDir(Level pLevel, BlockPos pPos, BlockState pState, boolean pAlwaysPlace) {
        if (pLevel.isClientSide) {
            return pState;
        } else {
            RailShape railshape = pState.getValue(this.getShapeProperty());
            return new RailState(pLevel, pPos, pState).place(pLevel.hasNeighborSignal(pPos), pAlwaysPlace, railshape).getState();
        }
    }

    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving) {
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
            if (pState.getValue(this.getShapeProperty()).isSlope()) {
                pLevel.updateNeighborsAt(pPos.above(), this);
            }

            if (this.isStraight) {
                pLevel.updateNeighborsAt(pPos, this);
                pLevel.updateNeighborsAt(pPos.below(), this);
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        boolean flag = fluidstate.getType() == Fluids.WATER;
        BlockState blockstate = super.defaultBlockState();
        Direction direction = pContext.getHorizontalDirection();
        boolean flag1 = direction == Direction.EAST || direction == Direction.WEST;
        return blockstate.setValue(this.getShapeProperty(), flag1 ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH).setValue(WATERLOGGED, Boolean.valueOf(flag));
    }

    public abstract Property<RailShape> getShapeProperty();

    @Override
    protected BlockState updateShape(
        BlockState p_152151_,
        LevelReader p_363749_,
        ScheduledTickAccess p_365089_,
        BlockPos p_152155_,
        Direction p_152152_,
        BlockPos p_152156_,
        BlockState p_152153_,
        RandomSource p_368260_
    ) {
        if (p_152151_.getValue(WATERLOGGED)) {
            p_365089_.scheduleTick(p_152155_, Fluids.WATER, Fluids.WATER.getTickDelay(p_363749_));
        }

        return super.updateShape(p_152151_, p_363749_, p_365089_, p_152155_, p_152152_, p_152156_, p_152153_, p_368260_);
    }

    @Override
    protected FluidState getFluidState(BlockState p_152158_) {
        return p_152158_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_152158_);
    }
}