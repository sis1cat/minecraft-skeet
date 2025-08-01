package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallTorchBlock extends TorchBlock {
    public static final MapCodec<WallTorchBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360464_ -> p_360464_.group(PARTICLE_OPTIONS_FIELD.forGetter(p_312123_ -> p_312123_.flameParticle), propertiesCodec()).apply(p_360464_, WallTorchBlock::new)
    );
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    protected static final float AABB_OFFSET = 2.5F;
    private static final Map<Direction, VoxelShape> AABBS = Maps.newEnumMap(
        ImmutableMap.of(
            Direction.NORTH,
            Block.box(5.5, 3.0, 11.0, 10.5, 13.0, 16.0),
            Direction.SOUTH,
            Block.box(5.5, 3.0, 0.0, 10.5, 13.0, 5.0),
            Direction.WEST,
            Block.box(11.0, 3.0, 5.5, 16.0, 13.0, 10.5),
            Direction.EAST,
            Block.box(0.0, 3.0, 5.5, 5.0, 13.0, 10.5)
        )
    );

    @Override
    public MapCodec<WallTorchBlock> codec() {
        return CODEC;
    }

    protected WallTorchBlock(SimpleParticleType p_312024_, BlockBehaviour.Properties p_58123_) {
        super(p_312024_, p_58123_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getShape(pState);
    }

    public static VoxelShape getShape(BlockState pState) {
        return AABBS.get(pState.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return canSurvive(pLevel, pPos, pState.getValue(FACING));
    }

    public static boolean canSurvive(LevelReader pLevel, BlockPos pPos, Direction pFacing) {
        BlockPos blockpos = pPos.relative(pFacing.getOpposite());
        BlockState blockstate = pLevel.getBlockState(blockpos);
        return blockstate.isFaceSturdy(pLevel, blockpos, pFacing);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockstate = this.defaultBlockState();
        LevelReader levelreader = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        Direction[] adirection = pContext.getNearestLookingDirections();

        for (Direction direction : adirection) {
            if (direction.getAxis().isHorizontal()) {
                Direction direction1 = direction.getOpposite();
                blockstate = blockstate.setValue(FACING, direction1);
                if (blockstate.canSurvive(levelreader, blockpos)) {
                    return blockstate;
                }
            }
        }

        return null;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_58143_,
        LevelReader p_365982_,
        ScheduledTickAccess p_363189_,
        BlockPos p_58147_,
        Direction p_58144_,
        BlockPos p_58148_,
        BlockState p_58145_,
        RandomSource p_366545_
    ) {
        return p_58144_.getOpposite() == p_58143_.getValue(FACING) && !p_58143_.canSurvive(p_365982_, p_58147_) ? Blocks.AIR.defaultBlockState() : p_58143_;
    }

    @Override
    public void animateTick(BlockState p_222660_, Level p_222661_, BlockPos p_222662_, RandomSource p_222663_) {
        Direction direction = p_222660_.getValue(FACING);
        double d0 = (double)p_222662_.getX() + 0.5;
        double d1 = (double)p_222662_.getY() + 0.7;
        double d2 = (double)p_222662_.getZ() + 0.5;
        double d3 = 0.22;
        double d4 = 0.27;
        Direction direction1 = direction.getOpposite();
        p_222661_.addParticle(
            ParticleTypes.SMOKE, d0 + 0.27 * (double)direction1.getStepX(), d1 + 0.22, d2 + 0.27 * (double)direction1.getStepZ(), 0.0, 0.0, 0.0
        );
        p_222661_.addParticle(this.flameParticle, d0 + 0.27 * (double)direction1.getStepX(), d1 + 0.22, d2 + 0.27 * (double)direction1.getStepZ(), 0.0, 0.0, 0.0);
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
        pBuilder.add(FACING);
    }
}