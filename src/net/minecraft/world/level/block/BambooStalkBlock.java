package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BambooStalkBlock extends Block implements BonemealableBlock {
    public static final MapCodec<BambooStalkBlock> CODEC = simpleCodec(BambooStalkBlock::new);
    protected static final float SMALL_LEAVES_AABB_OFFSET = 3.0F;
    protected static final float LARGE_LEAVES_AABB_OFFSET = 5.0F;
    protected static final float COLLISION_AABB_OFFSET = 1.5F;
    protected static final VoxelShape SMALL_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
    protected static final VoxelShape LARGE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    protected static final VoxelShape COLLISION_SHAPE = Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_1;
    public static final EnumProperty<BambooLeaves> LEAVES = BlockStateProperties.BAMBOO_LEAVES;
    public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
    public static final int MAX_HEIGHT = 16;
    public static final int STAGE_GROWING = 0;
    public static final int STAGE_DONE_GROWING = 1;
    public static final int AGE_THIN_BAMBOO = 0;
    public static final int AGE_THICK_BAMBOO = 1;

    @Override
    public MapCodec<BambooStalkBlock> codec() {
        return CODEC;
    }

    public BambooStalkBlock(BlockBehaviour.Properties p_261753_) {
        super(p_261753_);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(AGE, Integer.valueOf(0)).setValue(LEAVES, BambooLeaves.NONE).setValue(STAGE, Integer.valueOf(0))
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_261641_) {
        p_261641_.add(AGE, LEAVES, STAGE);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_261479_) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState p_261515_, BlockGetter p_261586_, BlockPos p_261526_, CollisionContext p_261930_) {
        VoxelShape voxelshape = p_261515_.getValue(LEAVES) == BambooLeaves.LARGE ? LARGE_SHAPE : SMALL_SHAPE;
        Vec3 vec3 = p_261515_.getOffset(p_261526_);
        return voxelshape.move(vec3.x, vec3.y, vec3.z);
    }

    @Override
    protected boolean isPathfindable(BlockState p_262166_, PathComputationType p_261513_) {
        return false;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState p_261560_, BlockGetter p_261965_, BlockPos p_261950_, CollisionContext p_261571_) {
        Vec3 vec3 = p_261560_.getOffset(p_261950_);
        return COLLISION_SHAPE.move(vec3.x, vec3.y, vec3.z);
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState p_262062_, BlockGetter p_261848_, BlockPos p_261466_) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_261764_) {
        FluidState fluidstate = p_261764_.getLevel().getFluidState(p_261764_.getClickedPos());
        if (!fluidstate.isEmpty()) {
            return null;
        } else {
            BlockState blockstate = p_261764_.getLevel().getBlockState(p_261764_.getClickedPos().below());
            if (blockstate.is(BlockTags.BAMBOO_PLANTABLE_ON)) {
                if (blockstate.is(Blocks.BAMBOO_SAPLING)) {
                    return this.defaultBlockState().setValue(AGE, Integer.valueOf(0));
                } else if (blockstate.is(Blocks.BAMBOO)) {
                    int i = blockstate.getValue(AGE) > 0 ? 1 : 0;
                    return this.defaultBlockState().setValue(AGE, Integer.valueOf(i));
                } else {
                    BlockState blockstate1 = p_261764_.getLevel().getBlockState(p_261764_.getClickedPos().above());
                    return blockstate1.is(Blocks.BAMBOO)
                        ? this.defaultBlockState().setValue(AGE, blockstate1.getValue(AGE))
                        : Blocks.BAMBOO_SAPLING.defaultBlockState();
                }
            } else {
                return null;
            }
        }
    }

    @Override
    protected void tick(BlockState p_261612_, ServerLevel p_261527_, BlockPos p_261846_, RandomSource p_261638_) {
        if (!p_261612_.canSurvive(p_261527_, p_261846_)) {
            p_261527_.destroyBlock(p_261846_, true);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState p_262083_) {
        return p_262083_.getValue(STAGE) == 0;
    }

    @Override
    protected void randomTick(BlockState p_261931_, ServerLevel p_261751_, BlockPos p_261616_, RandomSource p_261766_) {
        if (p_261931_.getValue(STAGE) == 0) {
            if (p_261766_.nextInt(3) == 0 && p_261751_.isEmptyBlock(p_261616_.above()) && p_261751_.getRawBrightness(p_261616_.above(), 0) >= 9) {
                int i = this.getHeightBelowUpToMax(p_261751_, p_261616_) + 1;
                if (i < 16) {
                    this.growBamboo(p_261931_, p_261751_, p_261616_, p_261766_, i);
                }
            }
        }
    }

    @Override
    protected boolean canSurvive(BlockState p_261860_, LevelReader p_262154_, BlockPos p_261493_) {
        return p_262154_.getBlockState(p_261493_.below()).is(BlockTags.BAMBOO_PLANTABLE_ON);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_261476_,
        LevelReader p_362157_,
        ScheduledTickAccess p_366749_,
        BlockPos p_261876_,
        Direction p_261512_,
        BlockPos p_262140_,
        BlockState p_262167_,
        RandomSource p_364105_
    ) {
        if (!p_261476_.canSurvive(p_362157_, p_261876_)) {
            p_366749_.scheduleTick(p_261876_, this, 1);
        }

        return p_261512_ == Direction.UP && p_262167_.is(Blocks.BAMBOO) && p_262167_.getValue(AGE) > p_261476_.getValue(AGE)
            ? p_261476_.cycle(AGE)
            : super.updateShape(p_261476_, p_362157_, p_366749_, p_261876_, p_261512_, p_262140_, p_262167_, p_364105_);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_262065_, BlockPos p_262033_, BlockState p_261700_) {
        int i = this.getHeightAboveUpToMax(p_262065_, p_262033_);
        int j = this.getHeightBelowUpToMax(p_262065_, p_262033_);
        return i + j + 1 < 16 && p_262065_.getBlockState(p_262033_.above(i)).getValue(STAGE) != 1;
    }

    @Override
    public boolean isBonemealSuccess(Level p_261870_, RandomSource p_261802_, BlockPos p_262123_, BlockState p_261972_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_261845_, RandomSource p_262034_, BlockPos p_261955_, BlockState p_261685_) {
        int i = this.getHeightAboveUpToMax(p_261845_, p_261955_);
        int j = this.getHeightBelowUpToMax(p_261845_, p_261955_);
        int k = i + j + 1;
        int l = 1 + p_262034_.nextInt(2);

        for (int i1 = 0; i1 < l; i1++) {
            BlockPos blockpos = p_261955_.above(i);
            BlockState blockstate = p_261845_.getBlockState(blockpos);
            if (k >= 16 || blockstate.getValue(STAGE) == 1 || !p_261845_.isEmptyBlock(blockpos.above())) {
                return;
            }

            this.growBamboo(blockstate, p_261845_, blockpos, p_262034_, k);
            i++;
            k++;
        }
    }

    @Override
    protected float getDestroyProgress(BlockState p_261691_, Player p_262171_, BlockGetter p_261621_, BlockPos p_261500_) {
        return p_262171_.getMainHandItem().getItem() instanceof SwordItem ? 1.0F : super.getDestroyProgress(p_261691_, p_262171_, p_261621_, p_261500_);
    }

    protected void growBamboo(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom, int pAge) {
        BlockState blockstate = pLevel.getBlockState(pPos.below());
        BlockPos blockpos = pPos.below(2);
        BlockState blockstate1 = pLevel.getBlockState(blockpos);
        BambooLeaves bambooleaves = BambooLeaves.NONE;
        if (pAge >= 1) {
            if (!blockstate.is(Blocks.BAMBOO) || blockstate.getValue(LEAVES) == BambooLeaves.NONE) {
                bambooleaves = BambooLeaves.SMALL;
            } else if (blockstate.is(Blocks.BAMBOO) && blockstate.getValue(LEAVES) != BambooLeaves.NONE) {
                bambooleaves = BambooLeaves.LARGE;
                if (blockstate1.is(Blocks.BAMBOO)) {
                    pLevel.setBlock(pPos.below(), blockstate.setValue(LEAVES, BambooLeaves.SMALL), 3);
                    pLevel.setBlock(blockpos, blockstate1.setValue(LEAVES, BambooLeaves.NONE), 3);
                }
            }
        }

        int i = pState.getValue(AGE) != 1 && !blockstate1.is(Blocks.BAMBOO) ? 0 : 1;
        int j = (pAge < 11 || !(pRandom.nextFloat() < 0.25F)) && pAge != 15 ? 0 : 1;
        pLevel.setBlock(
            pPos.above(),
            this.defaultBlockState().setValue(AGE, Integer.valueOf(i)).setValue(LEAVES, bambooleaves).setValue(STAGE, Integer.valueOf(j)),
            3
        );
    }

    protected int getHeightAboveUpToMax(BlockGetter pLevel, BlockPos pPos) {
        int i = 0;

        while (i < 16 && pLevel.getBlockState(pPos.above(i + 1)).is(Blocks.BAMBOO)) {
            i++;
        }

        return i;
    }

    protected int getHeightBelowUpToMax(BlockGetter pLevel, BlockPos pPos) {
        int i = 0;

        while (i < 16 && pLevel.getBlockState(pPos.below(i + 1)).is(Blocks.BAMBOO)) {
            i++;
        }

        return i;
    }
}