package net.minecraft.world.entity.monster;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class Skeleton extends AbstractSkeleton {
    private static final int TOTAL_CONVERSION_TIME = 300;
    private static final EntityDataAccessor<Boolean> DATA_STRAY_CONVERSION_ID = SynchedEntityData.defineId(Skeleton.class, EntityDataSerializers.BOOLEAN);
    public static final String CONVERSION_TAG = "StrayConversionTime";
    private int inPowderSnowTime;
    private int conversionTime;

    public Skeleton(EntityType<? extends Skeleton> p_33570_, Level p_33571_) {
        super(p_33570_, p_33571_);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_335528_) {
        super.defineSynchedData(p_335528_);
        p_335528_.define(DATA_STRAY_CONVERSION_ID, false);
    }

    public boolean isFreezeConverting() {
        return this.getEntityData().get(DATA_STRAY_CONVERSION_ID);
    }

    public void setFreezeConverting(boolean pIsFrozen) {
        this.entityData.set(DATA_STRAY_CONVERSION_ID, pIsFrozen);
    }

    @Override
    public boolean isShaking() {
        return this.isFreezeConverting();
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && this.isAlive() && !this.isNoAi()) {
            if (this.isInPowderSnow) {
                if (this.isFreezeConverting()) {
                    this.conversionTime--;
                    if (this.conversionTime < 0) {
                        this.doFreezeConversion();
                    }
                } else {
                    this.inPowderSnowTime++;
                    if (this.inPowderSnowTime >= 140) {
                        this.startFreezeConversion(300);
                    }
                }
            } else {
                this.inPowderSnowTime = -1;
                this.setFreezeConverting(false);
            }
        }

        super.tick();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag p_149836_) {
        super.addAdditionalSaveData(p_149836_);
        p_149836_.putInt("StrayConversionTime", this.isFreezeConverting() ? this.conversionTime : -1);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_149833_) {
        super.readAdditionalSaveData(p_149833_);
        if (p_149833_.contains("StrayConversionTime", 99) && p_149833_.getInt("StrayConversionTime") > -1) {
            this.startFreezeConversion(p_149833_.getInt("StrayConversionTime"));
        }
    }

    @VisibleForTesting
    public void startFreezeConversion(int pConversionTime) {
        this.conversionTime = pConversionTime;
        this.setFreezeConverting(true);
    }

    protected void doFreezeConversion() {
        this.convertTo(EntityType.STRAY, ConversionParams.single(this, true, true), p_375136_ -> {
            if (!this.isSilent()) {
                this.level().levelEvent(null, 1048, this.blockPosition(), 0);
            }
        });
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    SoundEvent getStepSound() {
        return SoundEvents.SKELETON_STEP;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel p_342869_, DamageSource p_33574_, boolean p_33576_) {
        super.dropCustomDeathLoot(p_342869_, p_33574_, p_33576_);
        if (p_33574_.getEntity() instanceof Creeper creeper && creeper.canDropMobsSkull()) {
            creeper.increaseDroppedSkulls();
            this.spawnAtLocation(p_342869_, Items.SKELETON_SKULL);
        }
    }
}