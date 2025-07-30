package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TripWireBlock extends Block {
    public static final MapCodec<TripWireBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360459_ -> p_360459_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("hook").forGetter(p_312791_ -> p_312791_.hook), propertiesCodec())
                .apply(p_360459_, TripWireBlock::new)
    );
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
    public static final BooleanProperty DISARMED = BlockStateProperties.DISARMED;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = CrossCollisionBlock.PROPERTY_BY_DIRECTION;
    protected static final VoxelShape AABB = Block.box(0.0, 1.0, 0.0, 16.0, 2.5, 16.0);
    protected static final VoxelShape NOT_ATTACHED_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final int RECHECK_PERIOD = 10;
    private final Block hook;

    @Override
    public MapCodec<TripWireBlock> codec() {
        return CODEC;
    }

    public TripWireBlock(Block pHook, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(POWERED, Boolean.valueOf(false))
                .setValue(ATTACHED, Boolean.valueOf(false))
                .setValue(DISARMED, Boolean.valueOf(false))
                .setValue(NORTH, Boolean.valueOf(false))
                .setValue(EAST, Boolean.valueOf(false))
                .setValue(SOUTH, Boolean.valueOf(false))
                .setValue(WEST, Boolean.valueOf(false))
        );
        this.hook = pHook;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return pState.getValue(ATTACHED) ? AABB : NOT_ATTACHED_AABB;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockGetter blockgetter = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        return this.defaultBlockState()
            .setValue(NORTH, Boolean.valueOf(this.shouldConnectTo(blockgetter.getBlockState(blockpos.north()), Direction.NORTH)))
            .setValue(EAST, Boolean.valueOf(this.shouldConnectTo(blockgetter.getBlockState(blockpos.east()), Direction.EAST)))
            .setValue(SOUTH, Boolean.valueOf(this.shouldConnectTo(blockgetter.getBlockState(blockpos.south()), Direction.SOUTH)))
            .setValue(WEST, Boolean.valueOf(this.shouldConnectTo(blockgetter.getBlockState(blockpos.west()), Direction.WEST)));
    }

    @Override
    protected BlockState updateShape(
        BlockState p_57645_,
        LevelReader p_366467_,
        ScheduledTickAccess p_366611_,
        BlockPos p_57649_,
        Direction p_57646_,
        BlockPos p_57650_,
        BlockState p_57647_,
        RandomSource p_369813_
    ) {
        return p_57646_.getAxis().isHorizontal()
            ? p_57645_.setValue(PROPERTY_BY_DIRECTION.get(p_57646_), Boolean.valueOf(this.shouldConnectTo(p_57647_, p_57646_)))
            : super.updateShape(p_57645_, p_366467_, p_366611_, p_57649_, p_57646_, p_57650_, p_57647_, p_369813_);
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pOldState.is(pState.getBlock())) {
            this.updateSource(pLevel, pPos, pState);
        }
    }

    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving && !pState.is(pNewState.getBlock())) {
            this.updateSource(pLevel, pPos, pState.setValue(POWERED, Boolean.valueOf(true)));
        }
    }

    @Override
    public BlockState playerWillDestroy(Level p_57615_, BlockPos p_57616_, BlockState p_57617_, Player p_57618_) {
        if (!p_57615_.isClientSide && !p_57618_.getMainHandItem().isEmpty() && p_57618_.getMainHandItem().is(Items.SHEARS)) {
            p_57615_.setBlock(p_57616_, p_57617_.setValue(DISARMED, Boolean.valueOf(true)), 4);
            p_57615_.gameEvent(p_57618_, GameEvent.SHEAR, p_57616_);
        }

        return super.playerWillDestroy(p_57615_, p_57616_, p_57617_, p_57618_);
    }

    private void updateSource(Level pLevel, BlockPos pPos, BlockState pState) {
        for (Direction direction : new Direction[]{Direction.SOUTH, Direction.WEST}) {
            for (int i = 1; i < 42; i++) {
                BlockPos blockpos = pPos.relative(direction, i);
                BlockState blockstate = pLevel.getBlockState(blockpos);
                if (blockstate.is(this.hook)) {
                    if (blockstate.getValue(TripWireHookBlock.FACING) == direction.getOpposite()) {
                        TripWireHookBlock.calculateState(pLevel, blockpos, blockstate, false, true, i, pState);
                    }
                    break;
                }

                if (!blockstate.is(this)) {
                    break;
                }
            }
        }
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState p_367024_, Level p_364645_, BlockPos p_366199_) {
        return p_367024_.getShape(p_364645_, p_366199_);
    }

    @Override
    protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        if (!pLevel.isClientSide) {
            if (!pState.getValue(POWERED)) {
                this.checkPressed(pLevel, pPos, List.of(pEntity));
            }
        }
    }

    @Override
    protected void tick(BlockState p_222598_, ServerLevel p_222599_, BlockPos p_222600_, RandomSource p_222601_) {
        if (p_222599_.getBlockState(p_222600_).getValue(POWERED)) {
            this.checkPressed(p_222599_, p_222600_);
        }
    }

    private void checkPressed(Level pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        List<? extends Entity> list = pLevel.getEntities(null, blockstate.getShape(pLevel, pPos).bounds().move(pPos));
        this.checkPressed(pLevel, pPos, list);
    }

    private void checkPressed(Level pLevel, BlockPos pPos, List<? extends Entity> pEntities) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        boolean flag = blockstate.getValue(POWERED);
        boolean flag1 = false;
        if (!pEntities.isEmpty()) {
            for (Entity entity : pEntities) {
                if (!entity.isIgnoringBlockTriggers()) {
                    flag1 = true;
                    break;
                }
            }
        }

        if (flag1 != flag) {
            blockstate = blockstate.setValue(POWERED, Boolean.valueOf(flag1));
            pLevel.setBlock(pPos, blockstate, 3);
            this.updateSource(pLevel, pPos, blockstate);
        }

        if (flag1) {
            pLevel.scheduleTick(new BlockPos(pPos), this, 10);
        }
    }

    public boolean shouldConnectTo(BlockState pState, Direction pDirection) {
        return pState.is(this.hook) ? pState.getValue(TripWireHookBlock.FACING) == pDirection.getOpposite() : pState.is(this);
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRot) {
        switch (pRot) {
            case CLOCKWISE_180:
                return pState.setValue(NORTH, pState.getValue(SOUTH))
                    .setValue(EAST, pState.getValue(WEST))
                    .setValue(SOUTH, pState.getValue(NORTH))
                    .setValue(WEST, pState.getValue(EAST));
            case COUNTERCLOCKWISE_90:
                return pState.setValue(NORTH, pState.getValue(EAST))
                    .setValue(EAST, pState.getValue(SOUTH))
                    .setValue(SOUTH, pState.getValue(WEST))
                    .setValue(WEST, pState.getValue(NORTH));
            case CLOCKWISE_90:
                return pState.setValue(NORTH, pState.getValue(WEST))
                    .setValue(EAST, pState.getValue(NORTH))
                    .setValue(SOUTH, pState.getValue(EAST))
                    .setValue(WEST, pState.getValue(SOUTH));
            default:
                return pState;
        }
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        switch (pMirror) {
            case LEFT_RIGHT:
                return pState.setValue(NORTH, pState.getValue(SOUTH)).setValue(SOUTH, pState.getValue(NORTH));
            case FRONT_BACK:
                return pState.setValue(EAST, pState.getValue(WEST)).setValue(WEST, pState.getValue(EAST));
            default:
                return super.mirror(pState, pMirror);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(POWERED, ATTACHED, DISARMED, NORTH, EAST, WEST, SOUTH);
    }
}