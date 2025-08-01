package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.EquipmentDispenseItemBehavior;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class DispenserBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<DispenserBlock> CODEC = simpleCodec(DispenserBlock::new);
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    private static final DefaultDispenseItemBehavior DEFAULT_BEHAVIOR = new DefaultDispenseItemBehavior();
    public static final Map<Item, DispenseItemBehavior> DISPENSER_REGISTRY = new IdentityHashMap<>();
    private static final int TRIGGER_DURATION = 4;

    @Override
    public MapCodec<? extends DispenserBlock> codec() {
        return CODEC;
    }

    public static void registerBehavior(ItemLike pItem, DispenseItemBehavior pBehavior) {
        DISPENSER_REGISTRY.put(pItem.asItem(), pBehavior);
    }

    public static void registerProjectileBehavior(ItemLike pItem) {
        DISPENSER_REGISTRY.put(pItem.asItem(), new ProjectileDispenseBehavior(pItem.asItem()));
    }

    protected DispenserBlock(BlockBehaviour.Properties p_52664_) {
        super(p_52664_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, Boolean.valueOf(false)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_52693_, Level p_52694_, BlockPos p_52695_, Player p_52696_, BlockHitResult p_52698_) {
        if (!p_52694_.isClientSide && p_52694_.getBlockEntity(p_52695_) instanceof DispenserBlockEntity dispenserblockentity) {
            p_52696_.openMenu(dispenserblockentity);
            p_52696_.awardStat(dispenserblockentity instanceof DropperBlockEntity ? Stats.INSPECT_DROPPER : Stats.INSPECT_DISPENSER);
        }

        return InteractionResult.SUCCESS;
    }

    protected void dispenseFrom(ServerLevel pLevel, BlockState pState, BlockPos pPos) {
        DispenserBlockEntity dispenserblockentity = pLevel.getBlockEntity(pPos, BlockEntityType.DISPENSER).orElse(null);
        if (dispenserblockentity == null) {
            LOGGER.warn("Ignoring dispensing attempt for Dispenser without matching block entity at {}", pPos);
        } else {
            BlockSource blocksource = new BlockSource(pLevel, pPos, pState, dispenserblockentity);
            int i = dispenserblockentity.getRandomSlot(pLevel.random);
            if (i < 0) {
                pLevel.levelEvent(1001, pPos, 0);
                pLevel.gameEvent(GameEvent.BLOCK_ACTIVATE, pPos, GameEvent.Context.of(dispenserblockentity.getBlockState()));
            } else {
                ItemStack itemstack = dispenserblockentity.getItem(i);
                DispenseItemBehavior dispenseitembehavior = this.getDispenseMethod(pLevel, itemstack);
                if (dispenseitembehavior != DispenseItemBehavior.NOOP) {
                    dispenserblockentity.setItem(i, dispenseitembehavior.dispense(blocksource, itemstack));
                }
            }
        }
    }

    protected DispenseItemBehavior getDispenseMethod(Level pLevel, ItemStack pItem) {
        if (!pItem.isItemEnabled(pLevel.enabledFeatures())) {
            return DEFAULT_BEHAVIOR;
        } else {
            DispenseItemBehavior dispenseitembehavior = DISPENSER_REGISTRY.get(pItem.getItem());
            return dispenseitembehavior != null ? dispenseitembehavior : getDefaultDispenseMethod(pItem);
        }
    }

    private static DispenseItemBehavior getDefaultDispenseMethod(ItemStack pStack) {
        return (DispenseItemBehavior)(pStack.has(DataComponents.EQUIPPABLE) ? EquipmentDispenseItemBehavior.INSTANCE : DEFAULT_BEHAVIOR);
    }

    @Override
    protected void neighborChanged(BlockState p_52700_, Level p_52701_, BlockPos p_52702_, Block p_52703_, @Nullable Orientation p_365036_, boolean p_52705_) {
        boolean flag = p_52701_.hasNeighborSignal(p_52702_) || p_52701_.hasNeighborSignal(p_52702_.above());
        boolean flag1 = p_52700_.getValue(TRIGGERED);
        if (flag && !flag1) {
            p_52701_.scheduleTick(p_52702_, this, 4);
            p_52701_.setBlock(p_52702_, p_52700_.setValue(TRIGGERED, Boolean.valueOf(true)), 2);
        } else if (!flag && flag1) {
            p_52701_.setBlock(p_52702_, p_52700_.setValue(TRIGGERED, Boolean.valueOf(false)), 2);
        }
    }

    @Override
    protected void tick(BlockState p_221075_, ServerLevel p_221076_, BlockPos p_221077_, RandomSource p_221078_) {
        this.dispenseFrom(p_221076_, p_221075_, p_221077_);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153162_, BlockState p_153163_) {
        return new DispenserBlockEntity(p_153162_, p_153163_);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        Containers.dropContentsOnDestroy(pState, pNewState, pLevel, pPos);
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    public static Position getDispensePosition(BlockSource pBlockSource) {
        return getDispensePosition(pBlockSource, 0.7, Vec3.ZERO);
    }

    public static Position getDispensePosition(BlockSource pBlockSource, double pMultiplier, Vec3 pOffset) {
        Direction direction = pBlockSource.state().getValue(FACING);
        return pBlockSource.center()
            .add(
                pMultiplier * (double)direction.getStepX() + pOffset.x(),
                pMultiplier * (double)direction.getStepY() + pOffset.y(),
                pMultiplier * (double)direction.getStepZ() + pOffset.z()
            );
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(pLevel.getBlockEntity(pPos));
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
        pBuilder.add(FACING, TRIGGERED);
    }
}