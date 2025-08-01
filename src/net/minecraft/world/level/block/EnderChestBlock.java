package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EnderChestBlock extends AbstractChestBlock<EnderChestBlockEntity> implements SimpleWaterloggedBlock {
    public static final MapCodec<EnderChestBlock> CODEC = simpleCodec(EnderChestBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
    private static final Component CONTAINER_TITLE = Component.translatable("container.enderchest");

    @Override
    public MapCodec<EnderChestBlock> codec() {
        return CODEC;
    }

    protected EnderChestBlock(BlockBehaviour.Properties p_53121_) {
        super(p_53121_, () -> BlockEntityType.ENDER_CHEST);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(
        BlockState pState, Level pLevel, BlockPos pPos, boolean pOverride
    ) {
        return DoubleBlockCombiner.Combiner::acceptNone;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_53137_, Level p_53138_, BlockPos p_53139_, Player p_53140_, BlockHitResult p_53142_) {
        PlayerEnderChestContainer playerenderchestcontainer = p_53140_.getEnderChestInventory();
        if (playerenderchestcontainer != null && p_53138_.getBlockEntity(p_53139_) instanceof EnderChestBlockEntity enderchestblockentity) {
            BlockPos $$9 = p_53139_.above();
            if (p_53138_.getBlockState($$9).isRedstoneConductor(p_53138_, $$9)) {
                return InteractionResult.SUCCESS;
            } else {
                if (p_53138_ instanceof ServerLevel serverlevel) {
                    playerenderchestcontainer.setActiveChest(enderchestblockentity);
                    p_53140_.openMenu(
                        new SimpleMenuProvider((p_53124_, p_53125_, p_53126_) -> ChestMenu.threeRows(p_53124_, p_53125_, playerenderchestcontainer), CONTAINER_TITLE)
                    );
                    p_53140_.awardStat(Stats.OPEN_ENDERCHEST);
                    PiglinAi.angerNearbyPiglins(serverlevel, p_53140_, true);
                }

                return InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153208_, BlockState p_153209_) {
        return new EnderChestBlockEntity(p_153208_, p_153209_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153199_, BlockState p_153200_, BlockEntityType<T> p_153201_) {
        return p_153199_.isClientSide ? createTickerHelper(p_153201_, BlockEntityType.ENDER_CHEST, EnderChestBlockEntity::lidAnimateTick) : null;
    }

    @Override
    public void animateTick(BlockState p_221117_, Level p_221118_, BlockPos p_221119_, RandomSource p_221120_) {
        for (int i = 0; i < 3; i++) {
            int j = p_221120_.nextInt(2) * 2 - 1;
            int k = p_221120_.nextInt(2) * 2 - 1;
            double d0 = (double)p_221119_.getX() + 0.5 + 0.25 * (double)j;
            double d1 = (double)((float)p_221119_.getY() + p_221120_.nextFloat());
            double d2 = (double)p_221119_.getZ() + 0.5 + 0.25 * (double)k;
            double d3 = (double)(p_221120_.nextFloat() * (float)j);
            double d4 = ((double)p_221120_.nextFloat() - 0.5) * 0.125;
            double d5 = (double)(p_221120_.nextFloat() * (float)k);
            p_221118_.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
        }
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_53160_,
        LevelReader p_370015_,
        ScheduledTickAccess p_361609_,
        BlockPos p_53164_,
        Direction p_53161_,
        BlockPos p_53165_,
        BlockState p_53162_,
        RandomSource p_362906_
    ) {
        if (p_53160_.getValue(WATERLOGGED)) {
            p_361609_.scheduleTick(p_53164_, Fluids.WATER, Fluids.WATER.getTickDelay(p_370015_));
        }

        return super.updateShape(p_53160_, p_370015_, p_361609_, p_53164_, p_53161_, p_53165_, p_53162_, p_362906_);
    }

    @Override
    protected boolean isPathfindable(BlockState p_53132_, PathComputationType p_53135_) {
        return false;
    }

    @Override
    protected void tick(BlockState p_221112_, ServerLevel p_221113_, BlockPos p_221114_, RandomSource p_221115_) {
        BlockEntity blockentity = p_221113_.getBlockEntity(p_221114_);
        if (blockentity instanceof EnderChestBlockEntity) {
            ((EnderChestBlockEntity)blockentity).recheckOpen();
        }
    }
}