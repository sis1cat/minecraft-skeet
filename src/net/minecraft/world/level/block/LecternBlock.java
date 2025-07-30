package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LecternBlock extends BaseEntityBlock {
    public static final MapCodec<LecternBlock> CODEC = simpleCodec(LecternBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty HAS_BOOK = BlockStateProperties.HAS_BOOK;
    public static final VoxelShape SHAPE_BASE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    public static final VoxelShape SHAPE_POST = Block.box(4.0, 2.0, 4.0, 12.0, 14.0, 12.0);
    public static final VoxelShape SHAPE_COMMON = Shapes.or(SHAPE_BASE, SHAPE_POST);
    public static final VoxelShape SHAPE_TOP_PLATE = Block.box(0.0, 15.0, 0.0, 16.0, 15.0, 16.0);
    public static final VoxelShape SHAPE_COLLISION = Shapes.or(SHAPE_COMMON, SHAPE_TOP_PLATE);
    public static final VoxelShape SHAPE_WEST = Shapes.or(
        Block.box(1.0, 10.0, 0.0, 5.333333, 14.0, 16.0),
        Block.box(5.333333, 12.0, 0.0, 9.666667, 16.0, 16.0),
        Block.box(9.666667, 14.0, 0.0, 14.0, 18.0, 16.0),
        SHAPE_COMMON
    );
    public static final VoxelShape SHAPE_NORTH = Shapes.or(
        Block.box(0.0, 10.0, 1.0, 16.0, 14.0, 5.333333),
        Block.box(0.0, 12.0, 5.333333, 16.0, 16.0, 9.666667),
        Block.box(0.0, 14.0, 9.666667, 16.0, 18.0, 14.0),
        SHAPE_COMMON
    );
    public static final VoxelShape SHAPE_EAST = Shapes.or(
        Block.box(10.666667, 10.0, 0.0, 15.0, 14.0, 16.0),
        Block.box(6.333333, 12.0, 0.0, 10.666667, 16.0, 16.0),
        Block.box(2.0, 14.0, 0.0, 6.333333, 18.0, 16.0),
        SHAPE_COMMON
    );
    public static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(0.0, 10.0, 10.666667, 16.0, 14.0, 15.0),
        Block.box(0.0, 12.0, 6.333333, 16.0, 16.0, 10.666667),
        Block.box(0.0, 14.0, 2.0, 16.0, 18.0, 6.333333),
        SHAPE_COMMON
    );
    private static final int PAGE_CHANGE_IMPULSE_TICKS = 2;

    @Override
    public MapCodec<LecternBlock> codec() {
        return CODEC;
    }

    protected LecternBlock(BlockBehaviour.Properties p_54479_) {
        super(p_54479_);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(HAS_BOOK, Boolean.valueOf(false))
        );
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState p_54584_) {
        return SHAPE_COMMON;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState pState) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Level level = pContext.getLevel();
        ItemStack itemstack = pContext.getItemInHand();
        Player player = pContext.getPlayer();
        boolean flag = false;
        if (!level.isClientSide && player != null && player.canUseGameMasterBlocks()) {
            CustomData customdata = itemstack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
            if (customdata.contains("Book")) {
                flag = true;
            }
        }

        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()).setValue(HAS_BOOK, Boolean.valueOf(flag));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE_COLLISION;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        switch ((Direction)pState.getValue(FACING)) {
            case NORTH:
                return SHAPE_NORTH;
            case SOUTH:
                return SHAPE_SOUTH;
            case EAST:
                return SHAPE_EAST;
            case WEST:
                return SHAPE_WEST;
            default:
                return SHAPE_COMMON;
        }
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, POWERED, HAS_BOOK);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153573_, BlockState p_153574_) {
        return new LecternBlockEntity(p_153573_, p_153574_);
    }

    public static boolean tryPlaceBook(@Nullable LivingEntity pEntity, Level pLevel, BlockPos pPos, BlockState pState, ItemStack pStack) {
        if (!pState.getValue(HAS_BOOK)) {
            if (!pLevel.isClientSide) {
                placeBook(pEntity, pLevel, pPos, pState, pStack);
            }

            return true;
        } else {
            return false;
        }
    }

    private static void placeBook(@Nullable LivingEntity pEntity, Level pLevel, BlockPos pPos, BlockState pState, ItemStack pStack) {
        if (pLevel.getBlockEntity(pPos) instanceof LecternBlockEntity lecternblockentity) {
            lecternblockentity.setBook(pStack.consumeAndReturn(1, pEntity));
            resetBookState(pEntity, pLevel, pPos, pState, true);
            pLevel.playSound(null, pPos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static void resetBookState(@Nullable Entity pEntity, Level pLevel, BlockPos pPos, BlockState pState, boolean pHasBook) {
        BlockState blockstate = pState.setValue(POWERED, Boolean.valueOf(false)).setValue(HAS_BOOK, Boolean.valueOf(pHasBook));
        pLevel.setBlock(pPos, blockstate, 3);
        pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockstate));
        updateBelow(pLevel, pPos, pState);
    }

    public static void signalPageChange(Level pLevel, BlockPos pPos, BlockState pState) {
        changePowered(pLevel, pPos, pState, true);
        pLevel.scheduleTick(pPos, pState.getBlock(), 2);
        pLevel.levelEvent(1043, pPos, 0);
    }

    private static void changePowered(Level pLevel, BlockPos pPos, BlockState pState, boolean pPowered) {
        pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(pPowered)), 3);
        updateBelow(pLevel, pPos, pState);
    }

    private static void updateBelow(Level pLevel, BlockPos pPos, BlockState pState) {
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(pLevel, pState.getValue(FACING).getOpposite(), Direction.UP);
        pLevel.updateNeighborsAt(pPos.below(), pState.getBlock(), orientation);
    }

    @Override
    protected void tick(BlockState p_221388_, ServerLevel p_221389_, BlockPos p_221390_, RandomSource p_221391_) {
        changePowered(p_221389_, p_221390_, p_221388_, false);
    }

    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            if (pState.getValue(HAS_BOOK)) {
                this.popBook(pState, pLevel, pPos);
            }

            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
            if (pState.getValue(POWERED)) {
                updateBelow(pLevel, pPos, pState);
            }
        }
    }

    private void popBook(BlockState pState, Level pLevel, BlockPos pPos) {
        if (pLevel.getBlockEntity(pPos) instanceof LecternBlockEntity lecternblockentity) {
            Direction direction = pState.getValue(FACING);
            ItemStack itemstack = lecternblockentity.getBook().copy();
            float f = 0.25F * (float)direction.getStepX();
            float f1 = 0.25F * (float)direction.getStepZ();
            ItemEntity itementity = new ItemEntity(
                pLevel,
                (double)pPos.getX() + 0.5 + (double)f,
                (double)(pPos.getY() + 1),
                (double)pPos.getZ() + 0.5 + (double)f1,
                itemstack
            );
            itementity.setDefaultPickUpDelay();
            pLevel.addFreshEntity(itementity);
            lecternblockentity.clearContent();
        }
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pSide == Direction.UP && pBlockState.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
        if (pBlockState.getValue(HAS_BOOK)) {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof LecternBlockEntity) {
                return ((LecternBlockEntity)blockentity).getRedstoneSignal();
            }
        }

        return 0;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_333093_, BlockState p_335984_, Level p_334086_, BlockPos p_332284_, Player p_332545_, InteractionHand p_328802_, BlockHitResult p_328840_
    ) {
        if (p_335984_.getValue(HAS_BOOK)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        } else if (p_333093_.is(ItemTags.LECTERN_BOOKS)) {
            return (InteractionResult)(tryPlaceBook(p_332545_, p_334086_, p_332284_, p_335984_, p_333093_)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS);
        } else {
            return (InteractionResult)(p_333093_.isEmpty() && p_328802_ == InteractionHand.MAIN_HAND
                ? InteractionResult.PASS
                : InteractionResult.TRY_WITH_EMPTY_HAND);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_331321_, Level p_329665_, BlockPos p_335448_, Player p_333152_, BlockHitResult p_331406_) {
        if (p_331321_.getValue(HAS_BOOK)) {
            if (!p_329665_.isClientSide) {
                this.openScreen(p_329665_, p_335448_, p_333152_);
            }

            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return !pState.getValue(HAS_BOOK) ? null : super.getMenuProvider(pState, pLevel, pPos);
    }

    private void openScreen(Level pLevel, BlockPos pPos, Player pPlayer) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity instanceof LecternBlockEntity) {
            pPlayer.openMenu((LecternBlockEntity)blockentity);
            pPlayer.awardStat(Stats.INTERACT_WITH_LECTERN);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState p_54510_, PathComputationType p_54513_) {
        return false;
    }
}