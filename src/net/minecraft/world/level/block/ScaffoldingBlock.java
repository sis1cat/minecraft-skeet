package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ScaffoldingBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<ScaffoldingBlock> CODEC = simpleCodec(ScaffoldingBlock::new);
    private static final int TICK_DELAY = 1;
    private static final VoxelShape STABLE_SHAPE;
    private static final VoxelShape UNSTABLE_SHAPE;
    private static final VoxelShape UNSTABLE_SHAPE_BOTTOM = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    private static final VoxelShape BELOW_BLOCK = Shapes.block().move(0.0, -1.0, 0.0);
    public static final int STABILITY_MAX_DISTANCE = 7;
    public static final IntegerProperty DISTANCE = BlockStateProperties.STABILITY_DISTANCE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;

    @Override
    public MapCodec<ScaffoldingBlock> codec() {
        return CODEC;
    }

    protected ScaffoldingBlock(BlockBehaviour.Properties p_56021_) {
        super(p_56021_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(DISTANCE, Integer.valueOf(7))
                .setValue(WATERLOGGED, Boolean.valueOf(false))
                .setValue(BOTTOM, Boolean.valueOf(false))
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(DISTANCE, WATERLOGGED, BOTTOM);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (!pContext.isHoldingItem(pState.getBlock().asItem())) {
            return pState.getValue(BOTTOM) ? UNSTABLE_SHAPE : STABLE_SHAPE;
        } else {
            return Shapes.block();
        }
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return Shapes.block();
    }

    @Override
    protected boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        return pUseContext.getItemInHand().is(this.asItem());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockPos blockpos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        int i = getDistance(level, blockpos);
        return this.defaultBlockState()
            .setValue(WATERLOGGED, Boolean.valueOf(level.getFluidState(blockpos).getType() == Fluids.WATER))
            .setValue(DISTANCE, Integer.valueOf(i))
            .setValue(BOTTOM, Boolean.valueOf(this.isBottom(level, blockpos, i)));
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pLevel.isClientSide) {
            pLevel.scheduleTick(pPos, this, 1);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_56044_,
        LevelReader p_365588_,
        ScheduledTickAccess p_361394_,
        BlockPos p_56048_,
        Direction p_56045_,
        BlockPos p_56049_,
        BlockState p_56046_,
        RandomSource p_369734_
    ) {
        if (p_56044_.getValue(WATERLOGGED)) {
            p_361394_.scheduleTick(p_56048_, Fluids.WATER, Fluids.WATER.getTickDelay(p_365588_));
        }

        if (!p_365588_.isClientSide()) {
            p_361394_.scheduleTick(p_56048_, this, 1);
        }

        return p_56044_;
    }

    @Override
    protected void tick(BlockState p_222019_, ServerLevel p_222020_, BlockPos p_222021_, RandomSource p_222022_) {
        int i = getDistance(p_222020_, p_222021_);
        BlockState blockstate = p_222019_.setValue(DISTANCE, Integer.valueOf(i)).setValue(BOTTOM, Boolean.valueOf(this.isBottom(p_222020_, p_222021_, i)));
        if (blockstate.getValue(DISTANCE) == 7) {
            if (p_222019_.getValue(DISTANCE) == 7) {
                FallingBlockEntity.fall(p_222020_, p_222021_, blockstate);
            } else {
                p_222020_.destroyBlock(p_222021_, true);
            }
        } else if (p_222019_ != blockstate) {
            p_222020_.setBlock(p_222021_, blockstate, 3);
        }
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return getDistance(pLevel, pPos) < 7;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (pContext.isAbove(Shapes.block(), pPos, true) && !pContext.isDescending()) {
            return STABLE_SHAPE;
        } else {
            return pState.getValue(DISTANCE) != 0 && pState.getValue(BOTTOM) && pContext.isAbove(BELOW_BLOCK, pPos, true) ? UNSTABLE_SHAPE_BOTTOM : Shapes.empty();
        }
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    private boolean isBottom(BlockGetter pLevel, BlockPos pPos, int pDistance) {
        return pDistance > 0 && !pLevel.getBlockState(pPos.below()).is(this);
    }

    public static int getDistance(BlockGetter pLevel, BlockPos pPos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable().move(Direction.DOWN);
        BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
        int i = 7;
        if (blockstate.is(Blocks.SCAFFOLDING)) {
            i = blockstate.getValue(DISTANCE);
        } else if (blockstate.isFaceSturdy(pLevel, blockpos$mutableblockpos, Direction.UP)) {
            return 0;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos.setWithOffset(pPos, direction));
            if (blockstate1.is(Blocks.SCAFFOLDING)) {
                i = Math.min(i, blockstate1.getValue(DISTANCE) + 1);
                if (i == 1) {
                    break;
                }
            }
        }

        return i;
    }

    static {
        VoxelShape voxelshape = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);
        VoxelShape voxelshape1 = Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 2.0);
        VoxelShape voxelshape2 = Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 2.0);
        VoxelShape voxelshape3 = Block.box(0.0, 0.0, 14.0, 2.0, 16.0, 16.0);
        VoxelShape voxelshape4 = Block.box(14.0, 0.0, 14.0, 16.0, 16.0, 16.0);
        STABLE_SHAPE = Shapes.or(voxelshape, voxelshape1, voxelshape2, voxelshape3, voxelshape4);
        VoxelShape voxelshape5 = Block.box(0.0, 0.0, 0.0, 2.0, 2.0, 16.0);
        VoxelShape voxelshape6 = Block.box(14.0, 0.0, 0.0, 16.0, 2.0, 16.0);
        VoxelShape voxelshape7 = Block.box(0.0, 0.0, 14.0, 16.0, 2.0, 16.0);
        VoxelShape voxelshape8 = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 2.0);
        UNSTABLE_SHAPE = Shapes.or(ScaffoldingBlock.UNSTABLE_SHAPE_BOTTOM, STABLE_SHAPE, voxelshape6, voxelshape5, voxelshape8, voxelshape7);
    }
}