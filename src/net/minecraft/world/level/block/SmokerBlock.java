package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SmokerBlock extends AbstractFurnaceBlock {
    public static final MapCodec<SmokerBlock> CODEC = simpleCodec(SmokerBlock::new);

    @Override
    public MapCodec<SmokerBlock> codec() {
        return CODEC;
    }

    protected SmokerBlock(BlockBehaviour.Properties p_56439_) {
        super(p_56439_);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_154644_, BlockState p_154645_) {
        return new SmokerBlockEntity(p_154644_, p_154645_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_154640_, BlockState p_154641_, BlockEntityType<T> p_154642_) {
        return createFurnaceTicker(p_154640_, p_154642_, BlockEntityType.SMOKER);
    }

    @Override
    protected void openContainer(Level pLevel, BlockPos pPos, Player pPlayer) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity instanceof SmokerBlockEntity) {
            pPlayer.openMenu((MenuProvider)blockentity);
            pPlayer.awardStat(Stats.INTERACT_WITH_SMOKER);
        }
    }

    @Override
    public void animateTick(BlockState p_222443_, Level p_222444_, BlockPos p_222445_, RandomSource p_222446_) {
        if (p_222443_.getValue(LIT)) {
            double d0 = (double)p_222445_.getX() + 0.5;
            double d1 = (double)p_222445_.getY();
            double d2 = (double)p_222445_.getZ() + 0.5;
            if (p_222446_.nextDouble() < 0.1) {
                p_222444_.playLocalSound(d0, d1, d2, SoundEvents.SMOKER_SMOKE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }

            p_222444_.addParticle(ParticleTypes.SMOKE, d0, d1 + 1.1, d2, 0.0, 0.0, 0.0);
        }
    }
}