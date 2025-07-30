package net.minecraft.world.entity.item;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public class ItemEntity extends Entity implements TraceableEntity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final float FLOAT_HEIGHT = 0.1F;
    public static final float EYE_HEIGHT = 0.2125F;
    private static final int LIFETIME = 6000;
    private static final int INFINITE_PICKUP_DELAY = 32767;
    private static final int INFINITE_LIFETIME = -32768;
    private int age;
    private int pickupDelay;
    private int health = 5;
    @Nullable
    private UUID thrower;
    @Nullable
    private Entity cachedThrower;
    @Nullable
    private UUID target;
    public final float bobOffs;

    public ItemEntity(EntityType<? extends ItemEntity> p_31991_, Level p_31992_) {
        super(p_31991_, p_31992_);
        this.bobOffs = this.random.nextFloat() * (float) Math.PI * 2.0F;
        this.setYRot(this.random.nextFloat() * 360.0F);
    }

    public ItemEntity(Level pLevel, double pPosX, double pPosY, double pPosZ, ItemStack pItemStack) {
        this(pLevel, pPosX, pPosY, pPosZ, pItemStack, pLevel.random.nextDouble() * 0.2 - 0.1, 0.2, pLevel.random.nextDouble() * 0.2 - 0.1);
    }

    public ItemEntity(
        Level pLevel, double pPosX, double pPosY, double pPosZ, ItemStack pItemStack, double pDeltaX, double pDeltaY, double pDeltaZ
    ) {
        this(EntityType.ITEM, pLevel);
        this.setPos(pPosX, pPosY, pPosZ);
        this.setDeltaMovement(pDeltaX, pDeltaY, pDeltaZ);
        this.setItem(pItemStack);
    }

    private ItemEntity(ItemEntity pOther) {
        super(pOther.getType(), pOther.level());
        this.setItem(pOther.getItem().copy());
        this.copyPosition(pOther);
        this.age = pOther.age;
        this.bobOffs = pOther.bobOffs;
    }

    @Override
    public boolean dampensVibrations() {
        return this.getItem().is(ItemTags.DAMPENS_VIBRATIONS);
    }

    @Nullable
    @Override
    public Entity getOwner() {
        if (this.cachedThrower != null && !this.cachedThrower.isRemoved()) {
            return this.cachedThrower;
        } else if (this.thrower != null && this.level() instanceof ServerLevel serverlevel) {
            this.cachedThrower = serverlevel.getEntity(this.thrower);
            return this.cachedThrower;
        } else {
            return null;
        }
    }

    @Override
    public void restoreFrom(Entity p_309647_) {
        super.restoreFrom(p_309647_);
        if (p_309647_ instanceof ItemEntity itementity) {
            this.cachedThrower = itementity.cachedThrower;
        }
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_332164_) {
        p_332164_.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        if (this.getItem().isEmpty()) {
            this.discard();
        } else {
            super.tick();
            if (this.pickupDelay > 0 && this.pickupDelay != 32767) {
                this.pickupDelay--;
            }

            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            Vec3 vec3 = this.getDeltaMovement();
            if (this.isInWater() && this.getFluidHeight(FluidTags.WATER) > 0.1F) {
                this.setUnderwaterMovement();
            } else if (this.isInLava() && this.getFluidHeight(FluidTags.LAVA) > 0.1F) {
                this.setUnderLavaMovement();
            } else {
                this.applyGravity();
            }

            if (this.level().isClientSide) {
                this.noPhysics = false;
            } else {
                this.noPhysics = !this.level().noCollision(this, this.getBoundingBox().deflate(1.0E-7));
                if (this.noPhysics) {
                    this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
                }
            }

            if (!this.onGround() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5F || (this.tickCount + this.getId()) % 4 == 0) {
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.applyEffectsFromBlocks();
                float f = 0.98F;
                if (this.onGround()) {
                    f = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.98F;
                }

                this.setDeltaMovement(this.getDeltaMovement().multiply((double)f, 0.98, (double)f));
                if (this.onGround()) {
                    Vec3 vec31 = this.getDeltaMovement();
                    if (vec31.y < 0.0) {
                        this.setDeltaMovement(vec31.multiply(1.0, -0.5, 1.0));
                    }
                }
            }

            boolean flag = Mth.floor(this.xo) != Mth.floor(this.getX())
                || Mth.floor(this.yo) != Mth.floor(this.getY())
                || Mth.floor(this.zo) != Mth.floor(this.getZ());
            int i = flag ? 2 : 40;
            if (this.tickCount % i == 0 && !this.level().isClientSide && this.isMergable()) {
                this.mergeWithNeighbours();
            }

            if (this.age != -32768) {
                this.age++;
            }

            this.hasImpulse = this.hasImpulse | this.updateInWaterStateAndDoFluidPushing();
            if (!this.level().isClientSide) {
                double d0 = this.getDeltaMovement().subtract(vec3).lengthSqr();
                if (d0 > 0.01) {
                    this.hasImpulse = true;
                }
            }

            if (!this.level().isClientSide && this.age >= 6000) {
                this.discard();
            }
        }
    }

    @Override
    public BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return this.getOnPos(0.999999F);
    }

    private void setUnderwaterMovement() {
        this.setFluidMovement(0.99F);
    }

    private void setUnderLavaMovement() {
        this.setFluidMovement(0.95F);
    }

    private void setFluidMovement(double pMultiplier) {
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.x * pMultiplier, vec3.y + (double)(vec3.y < 0.06F ? 5.0E-4F : 0.0F), vec3.z * pMultiplier);
    }

    private void mergeWithNeighbours() {
        if (this.isMergable()) {
            for (ItemEntity itementity : this.level()
                .getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(0.5, 0.0, 0.5), p_186268_ -> p_186268_ != this && p_186268_.isMergable())) {
                if (itementity.isMergable()) {
                    this.tryToMerge(itementity);
                    if (this.isRemoved()) {
                        break;
                    }
                }
            }
        }
    }

    private boolean isMergable() {
        ItemStack itemstack = this.getItem();
        return this.isAlive() && this.pickupDelay != 32767 && this.age != -32768 && this.age < 6000 && itemstack.getCount() < itemstack.getMaxStackSize();
    }

    private void tryToMerge(ItemEntity pItemEntity) {
        ItemStack itemstack = this.getItem();
        ItemStack itemstack1 = pItemEntity.getItem();
        if (Objects.equals(this.target, pItemEntity.target) && areMergable(itemstack, itemstack1)) {
            if (itemstack1.getCount() < itemstack.getCount()) {
                merge(this, itemstack, pItemEntity, itemstack1);
            } else {
                merge(pItemEntity, itemstack1, this, itemstack);
            }
        }
    }

    public static boolean areMergable(ItemStack pDestinationStack, ItemStack pOriginStack) {
        return pOriginStack.getCount() + pDestinationStack.getCount() > pOriginStack.getMaxStackSize() ? false : ItemStack.isSameItemSameComponents(pDestinationStack, pOriginStack);
    }

    public static ItemStack merge(ItemStack pDestinationStack, ItemStack pOriginStack, int pAmount) {
        int i = Math.min(Math.min(pDestinationStack.getMaxStackSize(), pAmount) - pDestinationStack.getCount(), pOriginStack.getCount());
        ItemStack itemstack = pDestinationStack.copyWithCount(pDestinationStack.getCount() + i);
        pOriginStack.shrink(i);
        return itemstack;
    }

    private static void merge(ItemEntity pDestinationEntity, ItemStack pDestinationStack, ItemStack pOriginStack) {
        ItemStack itemstack = merge(pDestinationStack, pOriginStack, 64);
        pDestinationEntity.setItem(itemstack);
    }

    private static void merge(ItemEntity pDestinationEntity, ItemStack pDestinationStack, ItemEntity pOriginEntity, ItemStack pOriginStack) {
        merge(pDestinationEntity, pDestinationStack, pOriginStack);
        pDestinationEntity.pickupDelay = Math.max(pDestinationEntity.pickupDelay, pOriginEntity.pickupDelay);
        pDestinationEntity.age = Math.min(pDestinationEntity.age, pOriginEntity.age);
        if (pOriginStack.isEmpty()) {
            pOriginEntity.discard();
        }
    }

    @Override
    public boolean fireImmune() {
        return !this.getItem().canBeHurtBy(this.damageSources().inFire()) || super.fireImmune();
    }

    @Override
    protected boolean shouldPlayLavaHurtSound() {
        return this.health <= 0 ? true : this.tickCount % 10 == 0;
    }

    @Override
    public final boolean hurtClient(DamageSource p_366723_) {
        return this.isInvulnerableToBase(p_366723_) ? false : this.getItem().canBeHurtBy(p_366723_);
    }

    @Override
    public final boolean hurtServer(ServerLevel p_362991_, DamageSource p_364841_, float p_362683_) {
        if (this.isInvulnerableToBase(p_364841_)) {
            return false;
        } else if (!p_362991_.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && p_364841_.getEntity() instanceof Mob) {
            return false;
        } else if (!this.getItem().canBeHurtBy(p_364841_)) {
            return false;
        } else {
            this.markHurt();
            this.health = (int)((float)this.health - p_362683_);
            this.gameEvent(GameEvent.ENTITY_DAMAGE, p_364841_.getEntity());
            if (this.health <= 0) {
                this.getItem().onDestroyed(this);
                this.discard();
            }

            return true;
        }
    }

    @Override
    public boolean ignoreExplosion(Explosion p_369761_) {
        return p_369761_.shouldAffectBlocklikeEntities() ? super.ignoreExplosion(p_369761_) : true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putShort("Health", (short)this.health);
        pCompound.putShort("Age", (short)this.age);
        pCompound.putShort("PickupDelay", (short)this.pickupDelay);
        if (this.thrower != null) {
            pCompound.putUUID("Thrower", this.thrower);
        }

        if (this.target != null) {
            pCompound.putUUID("Owner", this.target);
        }

        if (!this.getItem().isEmpty()) {
            pCompound.put("Item", this.getItem().save(this.registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        this.health = pCompound.getShort("Health");
        this.age = pCompound.getShort("Age");
        if (pCompound.contains("PickupDelay")) {
            this.pickupDelay = pCompound.getShort("PickupDelay");
        }

        if (pCompound.hasUUID("Owner")) {
            this.target = pCompound.getUUID("Owner");
        }

        if (pCompound.hasUUID("Thrower")) {
            this.thrower = pCompound.getUUID("Thrower");
            this.cachedThrower = null;
        }

        if (pCompound.contains("Item", 10)) {
            CompoundTag compoundtag = pCompound.getCompound("Item");
            this.setItem(ItemStack.parse(this.registryAccess(), compoundtag).orElse(ItemStack.EMPTY));
        } else {
            this.setItem(ItemStack.EMPTY);
        }

        if (this.getItem().isEmpty()) {
            this.discard();
        }
    }

    @Override
    public void playerTouch(Player pEntity) {
        if (!this.level().isClientSide) {
            ItemStack itemstack = this.getItem();
            Item item = itemstack.getItem();
            int i = itemstack.getCount();
            if (this.pickupDelay == 0 && (this.target == null || this.target.equals(pEntity.getUUID())) && pEntity.getInventory().add(itemstack)) {
                pEntity.take(this, i);
                if (itemstack.isEmpty()) {
                    this.discard();
                    itemstack.setCount(i);
                }

                pEntity.awardStat(Stats.ITEM_PICKED_UP.get(item), i);
                pEntity.onItemPickup(this);
            }
        }
    }

    @Override
    public Component getName() {
        Component component = this.getCustomName();
        return component != null ? component : this.getItem().getItemName();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Nullable
    @Override
    public Entity teleport(TeleportTransition p_365554_) {
        Entity entity = super.teleport(p_365554_);
        if (!this.level().isClientSide && entity instanceof ItemEntity itementity) {
            itementity.mergeWithNeighbours();
        }

        return entity;
    }

    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM);
    }

    public void setItem(ItemStack pStack) {
        this.getEntityData().set(DATA_ITEM, pStack);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (DATA_ITEM.equals(pKey)) {
            this.getItem().setEntityRepresentation(this);
        }
    }

    public void setTarget(@Nullable UUID pTarget) {
        this.target = pTarget;
    }

    public void setThrower(Entity pThrower) {
        this.thrower = pThrower.getUUID();
        this.cachedThrower = pThrower;
    }

    public int getAge() {
        return this.age;
    }

    public void setDefaultPickUpDelay() {
        this.pickupDelay = 10;
    }

    public void setNoPickUpDelay() {
        this.pickupDelay = 0;
    }

    public void setNeverPickUp() {
        this.pickupDelay = 32767;
    }

    public void setPickUpDelay(int pPickupDelay) {
        this.pickupDelay = pPickupDelay;
    }

    public boolean hasPickUpDelay() {
        return this.pickupDelay > 0;
    }

    public void setUnlimitedLifetime() {
        this.age = -32768;
    }

    public void setExtendedLifetime() {
        this.age = -6000;
    }

    public void makeFakeItem() {
        this.setNeverPickUp();
        this.age = 5999;
    }

    public static float getSpin(float pAge, float pBobOffset) {
        return pAge / 20.0F + pBobOffset;
    }

    public ItemEntity copy() {
        return new ItemEntity(this);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.AMBIENT;
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return 180.0F - getSpin((float)this.getAge() + 0.5F, this.bobOffs) / (float) (Math.PI * 2) * 360.0F;
    }

    @Override
    public SlotAccess getSlot(int p_329686_) {
        return p_329686_ == 0 ? SlotAccess.of(this::getItem, this::setItem) : super.getSlot(p_329686_);
    }
}