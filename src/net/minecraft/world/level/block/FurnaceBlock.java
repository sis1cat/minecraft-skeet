package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class FurnaceBlock extends AbstractFurnaceBlock {
    public static final MapCodec<FurnaceBlock> CODEC = simpleCodec(FurnaceBlock::new);

    @Override
    public MapCodec<FurnaceBlock> codec() {
        return CODEC;
    }

    protected FurnaceBlock(BlockBehaviour.Properties p_53627_) {
        super(p_53627_);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153277_, BlockState p_153278_) {
        return new FurnaceBlockEntity(p_153277_, p_153278_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153273_, BlockState p_153274_, BlockEntityType<T> p_153275_) {
        return createFurnaceTicker(p_153273_, p_153275_, BlockEntityType.FURNACE);
    }

    @Override
    protected void openContainer(Level pLevel, BlockPos pPos, Player pPlayer) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity instanceof FurnaceBlockEntity) {
            pPlayer.openMenu((MenuProvider)blockentity);
            pPlayer.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }

    @Override
    public void animateTick(BlockState p_221253_, Level p_221254_, BlockPos p_221255_, RandomSource p_221256_) {
        if (p_221253_.getValue(LIT)) {
            double d0 = (double)p_221255_.getX() + 0.5;
            double d1 = (double)p_221255_.getY();
            double d2 = (double)p_221255_.getZ() + 0.5;
            if (p_221256_.nextDouble() < 0.1) {
                p_221254_.playLocalSound(d0, d1, d2, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }

            Direction direction = p_221253_.getValue(FACING);
            Direction.Axis direction$axis = direction.getAxis();
            double d3 = 0.52;
            double d4 = p_221256_.nextDouble() * 0.6 - 0.3;
            double d5 = direction$axis == Direction.Axis.X ? (double)direction.getStepX() * 0.52 : d4;
            double d6 = p_221256_.nextDouble() * 6.0 / 16.0;
            double d7 = direction$axis == Direction.Axis.Z ? (double)direction.getStepZ() * 0.52 : d4;
            p_221254_.addParticle(ParticleTypes.SMOKE, d0 + d5, d1 + d6, d2 + d7, 0.0, 0.0, 0.0);
            p_221254_.addParticle(ParticleTypes.FLAME, d0 + d5, d1 + d6, d2 + d7, 0.0, 0.0, 0.0);
        }
    }
}