package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public class TntBlock extends Block {
    public static final MapCodec<TntBlock> CODEC = simpleCodec(TntBlock::new);
    public static final BooleanProperty UNSTABLE = BlockStateProperties.UNSTABLE;

    @Override
    public MapCodec<TntBlock> codec() {
        return CODEC;
    }

    public TntBlock(BlockBehaviour.Properties p_57422_) {
        super(p_57422_);
        this.registerDefaultState(this.defaultBlockState().setValue(UNSTABLE, Boolean.valueOf(false)));
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pOldState.is(pState.getBlock())) {
            if (pLevel.hasNeighborSignal(pPos)) {
                explode(pLevel, pPos);
                pLevel.removeBlock(pPos, false);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState p_57457_, Level p_57458_, BlockPos p_57459_, Block p_57460_, @Nullable Orientation p_362896_, boolean p_57462_) {
        if (p_57458_.hasNeighborSignal(p_57459_)) {
            explode(p_57458_, p_57459_);
            p_57458_.removeBlock(p_57459_, false);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level p_57445_, BlockPos p_57446_, BlockState p_57447_, Player p_57448_) {
        if (!p_57445_.isClientSide() && !p_57448_.isCreative() && p_57447_.getValue(UNSTABLE)) {
            explode(p_57445_, p_57446_);
        }

        return super.playerWillDestroy(p_57445_, p_57446_, p_57447_, p_57448_);
    }

    @Override
    public void wasExploded(ServerLevel p_368198_, BlockPos p_57442_, Explosion p_57443_) {
        PrimedTnt primedtnt = new PrimedTnt(
            p_368198_, (double)p_57442_.getX() + 0.5, (double)p_57442_.getY(), (double)p_57442_.getZ() + 0.5, p_57443_.getIndirectSourceEntity()
        );
        int i = primedtnt.getFuse();
        primedtnt.setFuse((short)(p_368198_.random.nextInt(i / 4) + i / 8));
        p_368198_.addFreshEntity(primedtnt);
    }

    public static void explode(Level pLevel, BlockPos pPos) {
        explode(pLevel, pPos, null);
    }

    private static void explode(Level pLevel, BlockPos pPos, @Nullable LivingEntity pEntity) {
        if (!pLevel.isClientSide) {
            PrimedTnt primedtnt = new PrimedTnt(
                pLevel, (double)pPos.getX() + 0.5, (double)pPos.getY(), (double)pPos.getZ() + 0.5, pEntity
            );
            pLevel.addFreshEntity(primedtnt);
            pLevel.playSound(null, primedtnt.getX(), primedtnt.getY(), primedtnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            pLevel.gameEvent(pEntity, GameEvent.PRIME_FUSE, pPos);
        }
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_329029_, BlockState p_331413_, Level p_329038_, BlockPos p_333168_, Player p_335932_, InteractionHand p_331663_, BlockHitResult p_330020_
    ) {
        if (!p_329029_.is(Items.FLINT_AND_STEEL) && !p_329029_.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(p_329029_, p_331413_, p_329038_, p_333168_, p_335932_, p_331663_, p_330020_);
        } else {
            explode(p_329038_, p_333168_, p_335932_);
            p_329038_.setBlock(p_333168_, Blocks.AIR.defaultBlockState(), 11);
            Item item = p_329029_.getItem();
            if (p_329029_.is(Items.FLINT_AND_STEEL)) {
                p_329029_.hurtAndBreak(1, p_335932_, LivingEntity.getSlotForHand(p_331663_));
            } else {
                p_329029_.consume(1, p_335932_);
            }

            p_335932_.awardStat(Stats.ITEM_USED.get(item));
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
        if (pLevel instanceof ServerLevel serverlevel) {
            BlockPos blockpos = pHit.getBlockPos();
            Entity entity = pProjectile.getOwner();
            if (pProjectile.isOnFire() && pProjectile.mayInteract(serverlevel, blockpos)) {
                explode(pLevel, blockpos, entity instanceof LivingEntity ? (LivingEntity)entity : null);
                pLevel.removeBlock(blockpos, false);
            }
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion pExplosion) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(UNSTABLE);
    }
}