package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CandleCakeBlock extends AbstractCandleBlock {
    public static final MapCodec<CandleCakeBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360414_ -> p_360414_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("candle").forGetter(p_327254_ -> p_327254_.candleBlock), propertiesCodec())
                .apply(p_360414_, CandleCakeBlock::new)
    );
    public static final BooleanProperty LIT = AbstractCandleBlock.LIT;
    protected static final float AABB_OFFSET = 1.0F;
    protected static final VoxelShape CAKE_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 8.0, 15.0);
    protected static final VoxelShape CANDLE_SHAPE = Block.box(7.0, 8.0, 7.0, 9.0, 14.0, 9.0);
    protected static final VoxelShape SHAPE = Shapes.or(CAKE_SHAPE, CANDLE_SHAPE);
    private static final Map<CandleBlock, CandleCakeBlock> BY_CANDLE = Maps.newHashMap();
    private static final Iterable<Vec3> PARTICLE_OFFSETS = ImmutableList.of(new Vec3(0.5, 1.0, 0.5));
    private final CandleBlock candleBlock;

    @Override
    public MapCodec<CandleCakeBlock> codec() {
        return CODEC;
    }

    protected CandleCakeBlock(Block pCandleBlock, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.valueOf(false)));
        if (pCandleBlock instanceof CandleBlock candleblock) {
            BY_CANDLE.put(candleblock, this);
            this.candleBlock = candleblock;
        } else {
            throw new IllegalArgumentException("Expected block to be of " + CandleBlock.class + " was " + pCandleBlock.getClass());
        }
    }

    @Override
    protected Iterable<Vec3> getParticleOffsets(BlockState p_152868_) {
        return PARTICLE_OFFSETS;
    }

    @Override
    protected VoxelShape getShape(BlockState p_152875_, BlockGetter p_152876_, BlockPos p_152877_, CollisionContext p_152878_) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_334372_, BlockState p_331468_, Level p_329195_, BlockPos p_334764_, Player p_333583_, InteractionHand p_329811_, BlockHitResult p_330359_
    ) {
        if (p_334372_.is(Items.FLINT_AND_STEEL) || p_334372_.is(Items.FIRE_CHARGE)) {
            return InteractionResult.PASS;
        } else if (candleHit(p_330359_) && p_334372_.isEmpty() && p_331468_.getValue(LIT)) {
            extinguish(p_333583_, p_331468_, p_329195_, p_334764_);
            return InteractionResult.SUCCESS;
        } else {
            return super.useItemOn(p_334372_, p_331468_, p_329195_, p_334764_, p_333583_, p_329811_, p_330359_);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_330001_, Level p_329319_, BlockPos p_331642_, Player p_333608_, BlockHitResult p_336319_) {
        InteractionResult interactionresult = CakeBlock.eat(p_329319_, p_331642_, Blocks.CAKE.defaultBlockState(), p_333608_);
        if (interactionresult.consumesAction()) {
            dropResources(p_330001_, p_329319_, p_331642_);
        }

        return interactionresult;
    }

    private static boolean candleHit(BlockHitResult pHit) {
        return pHit.getLocation().y - (double)pHit.getBlockPos().getY() > 0.5;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_152905_) {
        p_152905_.add(LIT);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_312018_, BlockPos p_152863_, BlockState p_152864_, boolean p_376540_) {
        return new ItemStack(Blocks.CAKE);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_152898_,
        LevelReader p_368310_,
        ScheduledTickAccess p_362687_,
        BlockPos p_152902_,
        Direction p_152899_,
        BlockPos p_152903_,
        BlockState p_152900_,
        RandomSource p_367940_
    ) {
        return p_152899_ == Direction.DOWN && !p_152898_.canSurvive(p_368310_, p_152902_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_152898_, p_368310_, p_362687_, p_152902_, p_152899_, p_152903_, p_152900_, p_367940_);
    }

    @Override
    protected boolean canSurvive(BlockState p_152891_, LevelReader p_152892_, BlockPos p_152893_) {
        return p_152892_.getBlockState(p_152893_.below()).isSolid();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_152880_, Level p_152881_, BlockPos p_152882_) {
        return CakeBlock.FULL_CAKE_SIGNAL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_152909_) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState p_152870_, PathComputationType p_152873_) {
        return false;
    }

    public static BlockState byCandle(CandleBlock pCandle) {
        return BY_CANDLE.get(pCandle).defaultBlockState();
    }

    public static boolean canLight(BlockState pState) {
        return pState.is(BlockTags.CANDLE_CAKES, p_152896_ -> p_152896_.hasProperty(LIT) && !pState.getValue(LIT));
    }
}