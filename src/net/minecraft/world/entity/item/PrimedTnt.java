package net.minecraft.world.entity.item;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.TeleportTransition;

public class PrimedTnt extends Entity implements TraceableEntity {
    private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(PrimedTnt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE_ID = SynchedEntityData.defineId(PrimedTnt.class, EntityDataSerializers.BLOCK_STATE);
    private static final int DEFAULT_FUSE_TIME = 80;
    private static final float DEFAULT_EXPLOSION_POWER = 4.0F;
    private static final String TAG_BLOCK_STATE = "block_state";
    public static final String TAG_FUSE = "fuse";
    private static final String TAG_EXPLOSION_POWER = "explosion_power";
    private static final ExplosionDamageCalculator USED_PORTAL_DAMAGE_CALCULATOR = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldBlockExplode(Explosion p_345242_, BlockGetter p_343858_, BlockPos p_345073_, BlockState p_343662_, float p_344776_) {
            return p_343662_.is(Blocks.NETHER_PORTAL) ? false : super.shouldBlockExplode(p_345242_, p_343858_, p_345073_, p_343662_, p_344776_);
        }

        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion p_342148_, BlockGetter p_342177_, BlockPos p_342771_, BlockState p_344877_, FluidState p_343569_) {
            return p_344877_.is(Blocks.NETHER_PORTAL) ? Optional.empty() : super.getBlockExplosionResistance(p_342148_, p_342177_, p_342771_, p_344877_, p_343569_);
        }
    };
    @Nullable
    private LivingEntity owner;
    private boolean usedPortal;
    private float explosionPower = 4.0F;

    public PrimedTnt(EntityType<? extends PrimedTnt> p_32076_, Level p_32077_) {
        super(p_32076_, p_32077_);
        this.blocksBuilding = true;
    }

    public PrimedTnt(Level pLevel, double pX, double pY, double pZ, @Nullable LivingEntity pOwner) {
        this(EntityType.TNT, pLevel);
        this.setPos(pX, pY, pZ);
        double d0 = pLevel.random.nextDouble() * (float) (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(d0) * 0.02, 0.2F, -Math.cos(d0) * 0.02);
        this.setFuse(80);
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
        this.owner = pOwner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_331629_) {
        p_331629_.define(DATA_FUSE_ID, 80);
        p_331629_.define(DATA_BLOCK_STATE_ID, Blocks.TNT.defaultBlockState());
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        this.handlePortal();
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.applyEffectsFromBlocks();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
        }

        int i = this.getFuse() - 1;
        this.setFuse(i);
        if (i <= 0) {
            this.discard();
            if (!this.level().isClientSide) {
                this.explode();
            }
        } else {
            this.updateInWaterStateAndDoFluidPushing();
            if (this.level().isClientSide) {
                this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    private void explode() {
        this.level()
            .explode(
                this,
                Explosion.getDefaultDamageSource(this.level(), this),
                this.usedPortal ? USED_PORTAL_DAMAGE_CALCULATOR : null,
                this.getX(),
                this.getY(0.0625),
                this.getZ(),
                this.explosionPower,
                false,
                Level.ExplosionInteraction.TNT
            );
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putShort("fuse", (short)this.getFuse());
        pCompound.put("block_state", NbtUtils.writeBlockState(this.getBlockState()));
        if (this.explosionPower != 4.0F) {
            pCompound.putFloat("explosion_power", this.explosionPower);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        this.setFuse(pCompound.getShort("fuse"));
        if (pCompound.contains("block_state", 10)) {
            this.setBlockState(NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), pCompound.getCompound("block_state")));
        }

        if (pCompound.contains("explosion_power", 99)) {
            this.explosionPower = Mth.clamp(pCompound.getFloat("explosion_power"), 0.0F, 128.0F);
        }
    }

    @Nullable
    public LivingEntity getOwner() {
        return this.owner;
    }

    @Override
    public void restoreFrom(Entity p_310336_) {
        super.restoreFrom(p_310336_);
        if (p_310336_ instanceof PrimedTnt primedtnt) {
            this.owner = primedtnt.owner;
        }
    }

    public void setFuse(int pLife) {
        this.entityData.set(DATA_FUSE_ID, pLife);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE_ID);
    }

    public void setBlockState(BlockState pBlockState) {
        this.entityData.set(DATA_BLOCK_STATE_ID, pBlockState);
    }

    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE_ID);
    }

    private void setUsedPortal(boolean pUsedPortal) {
        this.usedPortal = pUsedPortal;
    }

    @Nullable
    @Override
    public Entity teleport(TeleportTransition p_366559_) {
        Entity entity = super.teleport(p_366559_);
        if (entity instanceof PrimedTnt primedtnt) {
            primedtnt.setUsedPortal(true);
        }

        return entity;
    }

    @Override
    public final boolean hurtServer(ServerLevel p_367466_, DamageSource p_362017_, float p_362976_) {
        return false;
    }
}