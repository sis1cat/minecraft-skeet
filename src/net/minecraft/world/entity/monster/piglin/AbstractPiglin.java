package net.minecraft.world.entity.monster.piglin;

import com.google.common.annotations.VisibleForTesting;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

public abstract class AbstractPiglin extends Monster {
    protected static final EntityDataAccessor<Boolean> DATA_IMMUNE_TO_ZOMBIFICATION = SynchedEntityData.defineId(AbstractPiglin.class, EntityDataSerializers.BOOLEAN);
    public static final int CONVERSION_TIME = 300;
    protected int timeInOverworld;

    public AbstractPiglin(EntityType<? extends AbstractPiglin> p_34652_, Level p_34653_) {
        super(p_34652_, p_34653_);
        this.setCanPickUpLoot(true);
        this.applyOpenDoorsAbility();
        this.setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    private void applyOpenDoorsAbility() {
        if (GoalUtils.hasGroundPathNavigation(this)) {
            ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        }
    }

    protected abstract boolean canHunt();

    public void setImmuneToZombification(boolean pImmuneToZombification) {
        this.getEntityData().set(DATA_IMMUNE_TO_ZOMBIFICATION, pImmuneToZombification);
    }

    protected boolean isImmuneToZombification() {
        return this.getEntityData().get(DATA_IMMUNE_TO_ZOMBIFICATION);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_327959_) {
        super.defineSynchedData(p_327959_);
        p_327959_.define(DATA_IMMUNE_TO_ZOMBIFICATION, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (this.isImmuneToZombification()) {
            pCompound.putBoolean("IsImmuneToZombification", true);
        }

        pCompound.putInt("TimeInOverworld", this.timeInOverworld);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setImmuneToZombification(pCompound.getBoolean("IsImmuneToZombification"));
        this.timeInOverworld = pCompound.getInt("TimeInOverworld");
    }

    @Override
    protected void customServerAiStep(ServerLevel p_360786_) {
        super.customServerAiStep(p_360786_);
        if (this.isConverting()) {
            this.timeInOverworld++;
        } else {
            this.timeInOverworld = 0;
        }

        if (this.timeInOverworld > 300) {
            this.playConvertedSound();
            this.finishConversion(p_360786_);
        }
    }

    @VisibleForTesting
    public void setTimeInOverworld(int pTimeInOverworld) {
        this.timeInOverworld = pTimeInOverworld;
    }

    public boolean isConverting() {
        return !this.level().dimensionType().piglinSafe() && !this.isImmuneToZombification() && !this.isNoAi();
    }

    protected void finishConversion(ServerLevel pServerLevel) {
        this.convertTo(
            EntityType.ZOMBIFIED_PIGLIN,
            ConversionParams.single(this, true, true),
            p_369673_ -> p_369673_.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0))
        );
    }

    public boolean isAdult() {
        return !this.isBaby();
    }

    public abstract PiglinArmPose getArmPose();

    @Nullable
    @Override
    public LivingEntity getTarget() {
        return this.getTargetFromBrain();
    }

    protected boolean isHoldingMeleeWeapon() {
        return this.getMainHandItem().has(DataComponents.TOOL);
    }

    @Override
    public void playAmbientSound() {
        if (PiglinAi.isIdle(this)) {
            super.playAmbientSound();
        }
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    protected abstract void playConvertedSound();
}