package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LiquidBlock extends Block implements BucketPickup {
    private static final Codec<FlowingFluid> FLOWING_FLUID = BuiltInRegistries.FLUID
        .byNameCodec()
        .comapFlatMap(
            p_309784_ -> p_309784_ instanceof FlowingFluid flowingfluid
                    ? DataResult.success(flowingfluid)
                    : DataResult.error(() -> "Not a flowing fluid: " + p_309784_),
            p_311315_ -> (Fluid)p_311315_
        );
    public static final MapCodec<LiquidBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360438_ -> p_360438_.group(FLOWING_FLUID.fieldOf("fluid").forGetter(p_312827_ -> p_312827_.fluid), propertiesCodec()).apply(p_360438_, LiquidBlock::new)
    );
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
    protected final FlowingFluid fluid;
    private final List<FluidState> stateCache;
    public static final VoxelShape STABLE_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    public static final ImmutableList<Direction> POSSIBLE_FLOW_DIRECTIONS = ImmutableList.of(Direction.DOWN, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST);

    @Override
    public MapCodec<LiquidBlock> codec() {
        return CODEC;
    }

    protected LiquidBlock(FlowingFluid pFluid, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.fluid = pFluid;
        this.stateCache = Lists.newArrayList();
        this.stateCache.add(pFluid.getSource(false));

        for (int i = 1; i < 8; i++) {
            this.stateCache.add(pFluid.getFlowing(8 - i, false));
        }

        this.stateCache.add(pFluid.getFlowing(8, true));
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, Integer.valueOf(0)));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return pContext.isAbove(STABLE_SHAPE, pPos, true)
                && pState.getValue(LEVEL) == 0
                && pContext.canStandOnFluid(pLevel.getFluidState(pPos.above()), pState.getFluidState())
            ? STABLE_SHAPE
            : Shapes.empty();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState pState) {
        return pState.getFluidState().isRandomlyTicking();
    }

    @Override
    protected void randomTick(BlockState p_221410_, ServerLevel p_221411_, BlockPos p_221412_, RandomSource p_221413_) {
        p_221410_.getFluidState().randomTick(p_221411_, p_221412_, p_221413_);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_54745_) {
        return false;
    }

    @Override
    protected boolean isPathfindable(BlockState p_54704_, PathComputationType p_54707_) {
        return !this.fluid.is(FluidTags.LAVA);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        int i = pState.getValue(LEVEL);
        return this.stateCache.get(Math.min(i, 8));
    }

    @Override
    protected boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pSide) {
        return pAdjacentBlockState.getFluidState().getType().isSame(this.fluid);
    }

    @Override
    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState p_54720_, LootParams.Builder p_287727_) {
        return Collections.emptyList();
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (this.shouldSpreadLiquid(pLevel, pPos, pState)) {
            pLevel.scheduleTick(pPos, pState.getFluidState().getType(), this.fluid.getTickDelay(pLevel));
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_54723_,
        LevelReader p_363921_,
        ScheduledTickAccess p_363294_,
        BlockPos p_54727_,
        Direction p_54724_,
        BlockPos p_54728_,
        BlockState p_54725_,
        RandomSource p_364601_
    ) {
        if (p_54723_.getFluidState().isSource() || p_54725_.getFluidState().isSource()) {
            p_363294_.scheduleTick(p_54727_, p_54723_.getFluidState().getType(), this.fluid.getTickDelay(p_363921_));
        }

        return super.updateShape(p_54723_, p_363921_, p_363294_, p_54727_, p_54724_, p_54728_, p_54725_, p_364601_);
    }

    @Override
    protected void neighborChanged(BlockState p_54709_, Level p_54710_, BlockPos p_54711_, Block p_54712_, @Nullable Orientation p_368724_, boolean p_54714_) {
        if (this.shouldSpreadLiquid(p_54710_, p_54711_, p_54709_)) {
            p_54710_.scheduleTick(p_54711_, p_54709_.getFluidState().getType(), this.fluid.getTickDelay(p_54710_));
        }
    }

    private boolean shouldSpreadLiquid(Level pLevel, BlockPos pPos, BlockState pState) {
        if (this.fluid.is(FluidTags.LAVA)) {
            boolean flag = pLevel.getBlockState(pPos.below()).is(Blocks.SOUL_SOIL);

            for (Direction direction : POSSIBLE_FLOW_DIRECTIONS) {
                BlockPos blockpos = pPos.relative(direction.getOpposite());
                if (pLevel.getFluidState(blockpos).is(FluidTags.WATER)) {
                    Block block = pLevel.getFluidState(pPos).isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
                    pLevel.setBlockAndUpdate(pPos, block.defaultBlockState());
                    this.fizz(pLevel, pPos);
                    return false;
                }

                if (flag && pLevel.getBlockState(blockpos).is(Blocks.BLUE_ICE)) {
                    pLevel.setBlockAndUpdate(pPos, Blocks.BASALT.defaultBlockState());
                    this.fizz(pLevel, pPos);
                    return false;
                }
            }
        }

        return true;
    }

    private void fizz(LevelAccessor pLevel, BlockPos pPos) {
        pLevel.levelEvent(1501, pPos, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(LEVEL);
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player p_299124_, LevelAccessor p_153772_, BlockPos p_153773_, BlockState p_153774_) {
        if (p_153774_.getValue(LEVEL) == 0) {
            p_153772_.setBlock(p_153773_, Blocks.AIR.defaultBlockState(), 11);
            return new ItemStack(this.fluid.getBucket());
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return this.fluid.getPickupSound();
    }
}