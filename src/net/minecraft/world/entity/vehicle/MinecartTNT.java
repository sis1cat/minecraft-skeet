package net.minecraft.world.entity.vehicle;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class MinecartTNT extends AbstractMinecart {
    private static final byte EVENT_PRIME = 10;
    private static final String TAG_EXPLOSION_POWER = "explosion_power";
    private static final String TAG_EXPLOSION_SPEED_FACTOR = "explosion_speed_factor";
    private static final String TAG_FUSE = "fuse";
    private static final float DEFAULT_EXPLOSION_POWER_BASE = 4.0F;
    private static final float DEFAULT_EXPLOSION_SPEED_FACTOR = 1.0F;
    private int fuse = -1;
    private float explosionPowerBase = 4.0F;
    private float explosionSpeedFactor = 1.0F;

    public MinecartTNT(EntityType<? extends MinecartTNT> p_38649_, Level p_38650_) {
        super(p_38649_, p_38650_);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.TNT.defaultBlockState();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.fuse > 0) {
            this.fuse--;
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
        } else if (this.fuse == 0) {
            this.explode(this.getDeltaMovement().horizontalDistanceSqr());
        }

        if (this.horizontalCollision) {
            double d0 = this.getDeltaMovement().horizontalDistanceSqr();
            if (d0 >= 0.01F) {
                this.explode(d0);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel p_369878_, DamageSource p_366706_, float p_368719_) {
        if (p_366706_.getDirectEntity() instanceof Projectile projectile && projectile.isOnFire()) {
            DamageSource damagesource = this.damageSources().explosion(this, p_366706_.getEntity());
            this.explode(damagesource, projectile.getDeltaMovement().lengthSqr());
        }

        return super.hurtServer(p_369878_, p_366706_, p_368719_);
    }

    @Override
    public void destroy(ServerLevel p_366240_, DamageSource p_38664_) {
        double d0 = this.getDeltaMovement().horizontalDistanceSqr();
        if (!damageSourceIgnitesTnt(p_38664_) && !(d0 >= 0.01F)) {
            this.destroy(p_366240_, this.getDropItem());
        } else {
            if (this.fuse < 0) {
                this.primeFuse();
                this.fuse = this.random.nextInt(20) + this.random.nextInt(20);
            }
        }
    }

    @Override
    protected Item getDropItem() {
        return Items.TNT_MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.TNT_MINECART);
    }

    protected void explode(double pRadiusModifier) {
        this.explode(null, pRadiusModifier);
    }

    protected void explode(@Nullable DamageSource pDamageSource, double pRadiusModifier) {
        if (this.level() instanceof ServerLevel serverlevel) {
            double d0 = Math.min(Math.sqrt(pRadiusModifier), 5.0);
            serverlevel.explode(
                this,
                pDamageSource,
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                (float)((double)this.explosionPowerBase + (double)this.explosionSpeedFactor * this.random.nextDouble() * 1.5 * d0),
                false,
                Level.ExplosionInteraction.TNT
            );
            this.discard();
        }
    }

    @Override
    public boolean causeFallDamage(float p_150347_, float p_150348_, DamageSource p_150349_) {
        if (p_150347_ >= 3.0F) {
            float f = p_150347_ / 10.0F;
            this.explode((double)(f * f));
        }

        return super.causeFallDamage(p_150347_, p_150348_, p_150349_);
    }

    @Override
    public void activateMinecart(int pX, int pY, int pZ, boolean pReceivingPower) {
        if (pReceivingPower && this.fuse < 0) {
            this.primeFuse();
        }
    }

    @Override
    public void handleEntityEvent(byte p_38657_) {
        if (p_38657_ == 10) {
            this.primeFuse();
        } else {
            super.handleEntityEvent(p_38657_);
        }
    }

    public void primeFuse() {
        this.fuse = 80;
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte)10);
            if (!this.isSilent()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    public int getFuse() {
        return this.fuse;
    }

    public boolean isPrimed() {
        return this.fuse > -1;
    }

    @Override
    public float getBlockExplosionResistance(Explosion pExplosion, BlockGetter pLevel, BlockPos pPos, BlockState pBlockState, FluidState pFluidState, float pExplosionPower) {
        return !this.isPrimed() || !pBlockState.is(BlockTags.RAILS) && !pLevel.getBlockState(pPos.above()).is(BlockTags.RAILS)
            ? super.getBlockExplosionResistance(pExplosion, pLevel, pPos, pBlockState, pFluidState, pExplosionPower)
            : 0.0F;
    }

    @Override
    public boolean shouldBlockExplode(Explosion pExplosion, BlockGetter pLevel, BlockPos pPos, BlockState pBlockState, float pExplosionPower) {
        return !this.isPrimed() || !pBlockState.is(BlockTags.RAILS) && !pLevel.getBlockState(pPos.above()).is(BlockTags.RAILS)
            ? super.shouldBlockExplode(pExplosion, pLevel, pPos, pBlockState, pExplosionPower)
            : false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("fuse", 99)) {
            this.fuse = pCompound.getInt("fuse");
        }

        if (pCompound.contains("explosion_power", 99)) {
            this.explosionPowerBase = Mth.clamp(pCompound.getFloat("explosion_power"), 0.0F, 128.0F);
        }

        if (pCompound.contains("explosion_speed_factor", 99)) {
            this.explosionSpeedFactor = Mth.clamp(pCompound.getFloat("explosion_speed_factor"), 0.0F, 128.0F);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("fuse", this.fuse);
        if (this.explosionPowerBase != 4.0F) {
            pCompound.putFloat("explosion_power", this.explosionPowerBase);
        }

        if (this.explosionSpeedFactor != 1.0F) {
            pCompound.putFloat("explosion_speed_factor", this.explosionSpeedFactor);
        }
    }

    @Override
    boolean shouldSourceDestroy(DamageSource p_310072_) {
        return damageSourceIgnitesTnt(p_310072_);
    }

    private static boolean damageSourceIgnitesTnt(DamageSource pSource) {
        return pSource.is(DamageTypeTags.IS_FIRE) || pSource.is(DamageTypeTags.IS_EXPLOSION);
    }
}