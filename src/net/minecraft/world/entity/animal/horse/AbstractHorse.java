package net.minecraft.world.entity.animal.horse;

import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStandGoal;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

public abstract class AbstractHorse extends Animal implements ContainerListener, HasCustomInventoryScreen, OwnableEntity, PlayerRideableJumping, Saddleable {
    public static final int EQUIPMENT_SLOT_OFFSET = 400;
    public static final int CHEST_SLOT_OFFSET = 499;
    public static final int INVENTORY_SLOT_OFFSET = 500;
    public static final double BREEDING_CROSS_FACTOR = 0.15;
    private static final float MIN_MOVEMENT_SPEED = (float)generateSpeed(() -> 0.0);
    private static final float MAX_MOVEMENT_SPEED = (float)generateSpeed(() -> 1.0);
    private static final float MIN_JUMP_STRENGTH = (float)generateJumpStrength(() -> 0.0);
    private static final float MAX_JUMP_STRENGTH = (float)generateJumpStrength(() -> 1.0);
    private static final float MIN_HEALTH = generateMaxHealth(p_272505_ -> 0);
    private static final float MAX_HEALTH = generateMaxHealth(p_272504_ -> p_272504_ - 1);
    private static final float BACKWARDS_MOVE_SPEED_FACTOR = 0.25F;
    private static final float SIDEWAYS_MOVE_SPEED_FACTOR = 0.5F;
    private static final TargetingConditions.Selector PARENT_HORSE_SELECTOR = (p_359215_, p_359216_) -> {
        if (p_359215_ instanceof AbstractHorse abstracthorse && abstracthorse.isBred()) {
            return true;
        }

        return false;
    };
    private static final TargetingConditions MOMMY_TARGETING = TargetingConditions.forNonCombat().range(16.0).ignoreLineOfSight().selector(PARENT_HORSE_SELECTOR);
    private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.BYTE);
    private static final int FLAG_TAME = 2;
    private static final int FLAG_SADDLE = 4;
    private static final int FLAG_BRED = 8;
    private static final int FLAG_EATING = 16;
    private static final int FLAG_STANDING = 32;
    private static final int FLAG_OPEN_MOUTH = 64;
    public static final int INV_SLOT_SADDLE = 0;
    public static final int INV_BASE_COUNT = 1;
    public static final int INVENTORY_ROWS = 3;
    private int eatingCounter;
    private int mouthCounter;
    private int standCounter;
    public int tailCounter;
    public int sprintCounter;
    protected boolean isJumping;
    protected SimpleContainer inventory;
    protected int temper;
    protected float playerJumpPendingScale;
    protected boolean allowStandSliding;
    private float eatAnim;
    private float eatAnimO;
    private float standAnim;
    private float standAnimO;
    private float mouthAnim;
    private float mouthAnimO;
    protected boolean canGallop = true;
    protected int gallopSoundCounter;
    @Nullable
    private UUID owner;
    private final Container bodyArmorAccess = new ContainerSingleItem() {
        @Override
        public ItemStack getTheItem() {
            return AbstractHorse.this.getBodyArmorItem();
        }

        @Override
        public void setTheItem(ItemStack p_331421_) {
            AbstractHorse.this.setBodyArmorItem(p_331421_);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player p_335622_) {
            return p_335622_.getVehicle() == AbstractHorse.this || p_335622_.canInteractWithEntity(AbstractHorse.this, 4.0);
        }
    };

    protected AbstractHorse(EntityType<? extends AbstractHorse> p_30531_, Level p_30532_) {
        super(p_30531_, p_30532_);
        this.createInventory();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2));
        this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0, AbstractHorse.class));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        if (this.canPerformRearing()) {
            this.goalSelector.addGoal(9, new RandomStandGoal(this));
        }

        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, p_326996_ -> p_326996_.is(ItemTags.HORSE_TEMPT_ITEMS), false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_334643_) {
        super.defineSynchedData(p_334643_);
        p_334643_.define(DATA_ID_FLAGS, (byte)0);
    }

    protected boolean getFlag(int pFlagId) {
        return (this.entityData.get(DATA_ID_FLAGS) & pFlagId) != 0;
    }

    protected void setFlag(int pFlagId, boolean pValue) {
        byte b0 = this.entityData.get(DATA_ID_FLAGS);
        if (pValue) {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b0 | pFlagId));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b0 & ~pFlagId));
        }
    }

    public boolean isTamed() {
        return this.getFlag(2);
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.owner;
    }

    public void setOwnerUUID(@Nullable UUID pUuid) {
        this.owner = pUuid;
    }

    public boolean isJumping() {
        return this.isJumping;
    }

    public void setTamed(boolean pTamed) {
        this.setFlag(2, pTamed);
    }

    public void setIsJumping(boolean pJumping) {
        this.isJumping = pJumping;
    }

    @Override
    public boolean handleLeashAtDistance(Entity p_344799_, float p_342991_) {
        if (p_342991_ > 6.0F && this.isEating()) {
            this.setEating(false);
        }

        return true;
    }

    public boolean isEating() {
        return this.getFlag(16);
    }

    public boolean isStanding() {
        return this.getFlag(32);
    }

    public boolean isBred() {
        return this.getFlag(8);
    }

    public void setBred(boolean pBreeding) {
        this.setFlag(8, pBreeding);
    }

    @Override
    public boolean isSaddleable() {
        return this.isAlive() && !this.isBaby() && this.isTamed();
    }

    @Override
    public void equipSaddle(ItemStack p_343290_, @Nullable SoundSource p_30546_) {
        this.inventory.setItem(0, p_343290_);
    }

    public void equipBodyArmor(Player pPlayer, ItemStack pStack) {
        if (this.isEquippableInSlot(pStack, EquipmentSlot.BODY)) {
            this.setBodyArmorItem(pStack.consumeAndReturn(1, pPlayer));
        }
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_363135_) {
        return p_363135_ == EquipmentSlot.BODY && this.isTamed() || super.canDispenserEquipIntoSlot(p_363135_);
    }

    @Override
    public boolean isSaddled() {
        return this.getFlag(4);
    }

    public int getTemper() {
        return this.temper;
    }

    public void setTemper(int pTemper) {
        this.temper = pTemper;
    }

    public int modifyTemper(int pAddedTemper) {
        int i = Mth.clamp(this.getTemper() + pAddedTemper, 0, this.getMaxTemper());
        this.setTemper(i);
        return i;
    }

    @Override
    public boolean isPushable() {
        return !this.isVehicle();
    }

    private void eating() {
        this.openMouth();
        if (!this.isSilent()) {
            SoundEvent soundevent = this.getEatingSound();
            if (soundevent != null) {
                this.level()
                    .playSound(
                        null,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        soundevent,
                        this.getSoundSource(),
                        1.0F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
                    );
            }
        }
    }

    @Override
    public boolean causeFallDamage(float p_149499_, float p_149500_, DamageSource p_149501_) {
        if (p_149499_ > 1.0F) {
            this.playSound(SoundEvents.HORSE_LAND, 0.4F, 1.0F);
        }

        int i = this.calculateFallDamage(p_149499_, p_149500_);
        if (i <= 0) {
            return false;
        } else {
            this.hurt(p_149501_, (float)i);
            if (this.isVehicle()) {
                for (Entity entity : this.getIndirectPassengers()) {
                    entity.hurt(p_149501_, (float)i);
                }
            }

            this.playBlockFallSound();
            return true;
        }
    }

    public final int getInventorySize() {
        return getInventorySize(this.getInventoryColumns());
    }

    public static int getInventorySize(int pColumns) {
        return pColumns * 3 + 1;
    }

    protected void createInventory() {
        SimpleContainer simplecontainer = this.inventory;
        this.inventory = new SimpleContainer(this.getInventorySize());
        if (simplecontainer != null) {
            simplecontainer.removeListener(this);
            int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

            for (int j = 0; j < i; j++) {
                ItemStack itemstack = simplecontainer.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.inventory.setItem(j, itemstack.copy());
                }
            }
        }

        this.inventory.addListener(this);
        this.syncSaddleToClients();
    }

    protected void syncSaddleToClients() {
        if (!this.level().isClientSide) {
            this.setFlag(4, !this.inventory.getItem(0).isEmpty());
        }
    }

    @Override
    public void containerChanged(Container pInvBasic) {
        boolean flag = this.isSaddled();
        this.syncSaddleToClients();
        if (this.tickCount > 20 && !flag && this.isSaddled()) {
            this.playSound(this.getSaddleSoundEvent(), 0.5F, 1.0F);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel p_362456_, DamageSource p_368293_, float p_368244_) {
        boolean flag = super.hurtServer(p_362456_, p_368293_, p_368244_);
        if (flag && this.random.nextInt(3) == 0) {
            this.standIfPossible();
        }

        return flag;
    }

    protected boolean canPerformRearing() {
        return true;
    }

    @Nullable
    protected SoundEvent getEatingSound() {
        return null;
    }

    @Nullable
    protected SoundEvent getAngrySound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
        if (!pBlock.liquid()) {
            BlockState blockstate = this.level().getBlockState(pPos.above());
            SoundType soundtype = pBlock.getSoundType();
            if (blockstate.is(Blocks.SNOW)) {
                soundtype = blockstate.getSoundType();
            }

            if (this.isVehicle() && this.canGallop) {
                this.gallopSoundCounter++;
                if (this.gallopSoundCounter > 5 && this.gallopSoundCounter % 3 == 0) {
                    this.playGallopSound(soundtype);
                } else if (this.gallopSoundCounter <= 5) {
                    this.playSound(SoundEvents.HORSE_STEP_WOOD, soundtype.getVolume() * 0.15F, soundtype.getPitch());
                }
            } else if (this.isWoodSoundType(soundtype)) {
                this.playSound(SoundEvents.HORSE_STEP_WOOD, soundtype.getVolume() * 0.15F, soundtype.getPitch());
            } else {
                this.playSound(SoundEvents.HORSE_STEP, soundtype.getVolume() * 0.15F, soundtype.getPitch());
            }
        }
    }

    private boolean isWoodSoundType(SoundType pSoundType) {
        return pSoundType == SoundType.WOOD
            || pSoundType == SoundType.NETHER_WOOD
            || pSoundType == SoundType.STEM
            || pSoundType == SoundType.CHERRY_WOOD
            || pSoundType == SoundType.BAMBOO_WOOD;
    }

    protected void playGallopSound(SoundType pSoundType) {
        this.playSound(SoundEvents.HORSE_GALLOP, pSoundType.getVolume() * 0.15F, pSoundType.getPitch());
    }

    public static AttributeSupplier.Builder createBaseHorseAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.JUMP_STRENGTH, 0.7)
            .add(Attributes.MAX_HEALTH, 53.0)
            .add(Attributes.MOVEMENT_SPEED, 0.225F)
            .add(Attributes.STEP_HEIGHT, 1.0)
            .add(Attributes.SAFE_FALL_DISTANCE, 6.0)
            .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.5);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 6;
    }

    public int getMaxTemper() {
        return 100;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    public void openCustomInventoryScreen(Player p_218808_) {
        if (!this.level().isClientSide && (!this.isVehicle() || this.hasPassenger(p_218808_)) && this.isTamed()) {
            p_218808_.openHorseInventory(this, this.inventory);
        }
    }

    public InteractionResult fedFood(Player pPlayer, ItemStack pStack) {
        boolean flag = this.handleEating(pPlayer, pStack);
        if (flag) {
            pStack.consume(1, pPlayer);
        }

        return (InteractionResult)(!flag && !this.level().isClientSide ? InteractionResult.PASS : InteractionResult.SUCCESS_SERVER);
    }

    protected boolean handleEating(Player pPlayer, ItemStack pStack) {
        boolean flag = false;
        float f = 0.0F;
        int i = 0;
        int j = 0;
        if (pStack.is(Items.WHEAT)) {
            f = 2.0F;
            i = 20;
            j = 3;
        } else if (pStack.is(Items.SUGAR)) {
            f = 1.0F;
            i = 30;
            j = 3;
        } else if (pStack.is(Blocks.HAY_BLOCK.asItem())) {
            f = 20.0F;
            i = 180;
        } else if (pStack.is(Items.APPLE)) {
            f = 3.0F;
            i = 60;
            j = 3;
        } else if (pStack.is(Items.GOLDEN_CARROT)) {
            f = 4.0F;
            i = 60;
            j = 5;
            if (!this.level().isClientSide && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
                flag = true;
                this.setInLove(pPlayer);
            }
        } else if (pStack.is(Items.GOLDEN_APPLE) || pStack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            f = 10.0F;
            i = 240;
            j = 10;
            if (!this.level().isClientSide && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
                flag = true;
                this.setInLove(pPlayer);
            }
        }

        if (this.getHealth() < this.getMaxHealth() && f > 0.0F) {
            this.heal(f);
            flag = true;
        }

        if (this.isBaby() && i > 0) {
            this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
            if (!this.level().isClientSide) {
                this.ageUp(i);
                flag = true;
            }
        }

        if (j > 0 && (flag || !this.isTamed()) && this.getTemper() < this.getMaxTemper() && !this.level().isClientSide) {
            this.modifyTemper(j);
            flag = true;
        }

        if (flag) {
            this.eating();
            this.gameEvent(GameEvent.EAT);
        }

        return flag;
    }

    protected void doPlayerRide(Player pPlayer) {
        this.setEating(false);
        this.setStanding(false);
        if (!this.level().isClientSide) {
            pPlayer.setYRot(this.getYRot());
            pPlayer.setXRot(this.getXRot());
            pPlayer.startRiding(this);
        }
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() && this.isVehicle() && this.isSaddled() || this.isEating() || this.isStanding();
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(ItemTags.HORSE_FOOD);
    }

    private void moveTail() {
        this.tailCounter = 1;
    }

    @Override
    protected void dropEquipment(ServerLevel p_369371_) {
        super.dropEquipment(p_369371_);
        if (this.inventory != null) {
            for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                ItemStack itemstack = this.inventory.getItem(i);
                if (!itemstack.isEmpty() && !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                    this.spawnAtLocation(p_369371_, itemstack);
                }
            }
        }
    }

    @Override
    public void aiStep() {
        if (this.random.nextInt(200) == 0) {
            this.moveTail();
        }

        super.aiStep();
        if (this.level() instanceof ServerLevel serverlevel && this.isAlive()) {
            if (this.random.nextInt(900) == 0 && this.deathTime == 0) {
                this.heal(1.0F);
            }

            if (this.canEatGrass()) {
                if (!this.isEating()
                    && !this.isVehicle()
                    && this.random.nextInt(300) == 0
                    && serverlevel.getBlockState(this.blockPosition().below()).is(Blocks.GRASS_BLOCK)) {
                    this.setEating(true);
                }

                if (this.isEating() && ++this.eatingCounter > 50) {
                    this.eatingCounter = 0;
                    this.setEating(false);
                }
            }

            this.followMommy(serverlevel);
            return;
        }
    }

    protected void followMommy(ServerLevel pLevel) {
        if (this.isBred() && this.isBaby() && !this.isEating()) {
            LivingEntity livingentity = pLevel.getNearestEntity(
                AbstractHorse.class, MOMMY_TARGETING, this, this.getX(), this.getY(), this.getZ(), this.getBoundingBox().inflate(16.0)
            );
            if (livingentity != null && this.distanceToSqr(livingentity) > 4.0) {
                this.navigation.createPath(livingentity, 0);
            }
        }
    }

    public boolean canEatGrass() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.mouthCounter > 0 && ++this.mouthCounter > 30) {
            this.mouthCounter = 0;
            this.setFlag(64, false);
        }

        if (this.isEffectiveAi() && this.standCounter > 0 && ++this.standCounter > 20) {
            this.standCounter = 0;
            this.setStanding(false);
        }

        if (this.tailCounter > 0 && ++this.tailCounter > 8) {
            this.tailCounter = 0;
        }

        if (this.sprintCounter > 0) {
            this.sprintCounter++;
            if (this.sprintCounter > 300) {
                this.sprintCounter = 0;
            }
        }

        this.eatAnimO = this.eatAnim;
        if (this.isEating()) {
            this.eatAnim = this.eatAnim + (1.0F - this.eatAnim) * 0.4F + 0.05F;
            if (this.eatAnim > 1.0F) {
                this.eatAnim = 1.0F;
            }
        } else {
            this.eatAnim = this.eatAnim + ((0.0F - this.eatAnim) * 0.4F - 0.05F);
            if (this.eatAnim < 0.0F) {
                this.eatAnim = 0.0F;
            }
        }

        this.standAnimO = this.standAnim;
        if (this.isStanding()) {
            this.eatAnim = 0.0F;
            this.eatAnimO = this.eatAnim;
            this.standAnim = this.standAnim + (1.0F - this.standAnim) * 0.4F + 0.05F;
            if (this.standAnim > 1.0F) {
                this.standAnim = 1.0F;
            }
        } else {
            this.allowStandSliding = false;
            this.standAnim = this.standAnim + ((0.8F * this.standAnim * this.standAnim * this.standAnim - this.standAnim) * 0.6F - 0.05F);
            if (this.standAnim < 0.0F) {
                this.standAnim = 0.0F;
            }
        }

        this.mouthAnimO = this.mouthAnim;
        if (this.getFlag(64)) {
            this.mouthAnim = this.mouthAnim + (1.0F - this.mouthAnim) * 0.7F + 0.05F;
            if (this.mouthAnim > 1.0F) {
                this.mouthAnim = 1.0F;
            }
        } else {
            this.mouthAnim = this.mouthAnim + ((0.0F - this.mouthAnim) * 0.7F - 0.05F);
            if (this.mouthAnim < 0.0F) {
                this.mouthAnim = 0.0F;
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player p_252289_, InteractionHand p_248927_) {
        if (this.isVehicle() || this.isBaby()) {
            return super.mobInteract(p_252289_, p_248927_);
        } else if (this.isTamed() && p_252289_.isSecondaryUseActive()) {
            this.openCustomInventoryScreen(p_252289_);
            return InteractionResult.SUCCESS;
        } else {
            ItemStack itemstack = p_252289_.getItemInHand(p_248927_);
            if (!itemstack.isEmpty()) {
                InteractionResult interactionresult = itemstack.interactLivingEntity(p_252289_, this, p_248927_);
                if (interactionresult.consumesAction()) {
                    return interactionresult;
                }

                if (this.isEquippableInSlot(itemstack, EquipmentSlot.BODY) && !this.isWearingBodyArmor()) {
                    this.equipBodyArmor(p_252289_, itemstack);
                    return InteractionResult.SUCCESS;
                }
            }

            this.doPlayerRide(p_252289_);
            return InteractionResult.SUCCESS;
        }
    }

    private void openMouth() {
        if (!this.level().isClientSide) {
            this.mouthCounter = 1;
            this.setFlag(64, true);
        }
    }

    public void setEating(boolean pEating) {
        this.setFlag(16, pEating);
    }

    public void setStanding(boolean pStanding) {
        if (pStanding) {
            this.setEating(false);
        }

        this.setFlag(32, pStanding);
    }

    @Nullable
    public SoundEvent getAmbientStandSound() {
        return this.getAmbientSound();
    }

    public void standIfPossible() {
        if (this.canPerformRearing() && this.isEffectiveAi()) {
            this.standCounter = 1;
            this.setStanding(true);
        }
    }

    public void makeMad() {
        if (!this.isStanding()) {
            this.standIfPossible();
            this.makeSound(this.getAngrySound());
        }
    }

    public boolean tameWithName(Player pPlayer) {
        this.setOwnerUUID(pPlayer.getUUID());
        this.setTamed(true);
        if (pPlayer instanceof ServerPlayer) {
            CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayer)pPlayer, this);
        }

        this.level().broadcastEntityEvent(this, (byte)7);
        return true;
    }

    @Override
    protected void tickRidden(Player p_278233_, Vec3 p_275693_) {
        super.tickRidden(p_278233_, p_275693_);
        Vec2 vec2 = this.getRiddenRotation(p_278233_);
        this.setRot(vec2.y, vec2.x);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        if (this.isControlledByLocalInstance()) {
            if (p_275693_.z <= 0.0) {
                this.gallopSoundCounter = 0;
            }

            if (this.onGround()) {
                this.setIsJumping(false);
                if (this.playerJumpPendingScale > 0.0F && !this.isJumping()) {
                    this.executeRidersJump(this.playerJumpPendingScale, p_275693_);
                }

                this.playerJumpPendingScale = 0.0F;
            }
        }
    }

    protected Vec2 getRiddenRotation(LivingEntity pEntity) {
        return new Vec2(pEntity.getXRot() * 0.5F, pEntity.getYRot());
    }

    @Override
    protected Vec3 getRiddenInput(Player p_278278_, Vec3 p_275506_) {
        if (this.onGround() && this.playerJumpPendingScale == 0.0F && this.isStanding() && !this.allowStandSliding) {
            return Vec3.ZERO;
        } else {
            float f = p_278278_.xxa * 0.5F;
            float f1 = p_278278_.zza;
            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }

            return new Vec3((double)f, 0.0, (double)f1);
        }
    }

    @Override
    protected float getRiddenSpeed(Player p_278336_) {
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    protected void executeRidersJump(float pPlayerJumpPendingScale, Vec3 pTravelVector) {
        double d0 = (double)this.getJumpPower(pPlayerJumpPendingScale);
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.x, d0, vec3.z);
        this.setIsJumping(true);
        this.hasImpulse = true;
        if (pTravelVector.z > 0.0) {
            float f = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
            float f1 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
            this.setDeltaMovement(this.getDeltaMovement().add((double)(-0.4F * f * pPlayerJumpPendingScale), 0.0, (double)(0.4F * f1 * pPlayerJumpPendingScale)));
        }
    }

    protected void playJumpSound() {
        this.playSound(SoundEvents.HORSE_JUMP, 0.4F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("EatingHaystack", this.isEating());
        pCompound.putBoolean("Bred", this.isBred());
        pCompound.putInt("Temper", this.getTemper());
        pCompound.putBoolean("Tame", this.isTamed());
        if (this.getOwnerUUID() != null) {
            pCompound.putUUID("Owner", this.getOwnerUUID());
        }

        if (!this.inventory.getItem(0).isEmpty()) {
            pCompound.put("SaddleItem", this.inventory.getItem(0).save(this.registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setEating(pCompound.getBoolean("EatingHaystack"));
        this.setBred(pCompound.getBoolean("Bred"));
        this.setTemper(pCompound.getInt("Temper"));
        this.setTamed(pCompound.getBoolean("Tame"));
        UUID uuid;
        if (pCompound.hasUUID("Owner")) {
            uuid = pCompound.getUUID("Owner");
        } else {
            String s = pCompound.getString("Owner");
            uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
        }

        if (uuid != null) {
            this.setOwnerUUID(uuid);
        }

        if (pCompound.contains("SaddleItem", 10)) {
            ItemStack itemstack = ItemStack.parse(this.registryAccess(), pCompound.getCompound("SaddleItem")).orElse(ItemStack.EMPTY);
            if (itemstack.is(Items.SADDLE)) {
                this.inventory.setItem(0, itemstack);
            }
        }

        this.syncSaddleToClients();
    }

    @Override
    public boolean canMate(Animal pOtherAnimal) {
        return false;
    }

    protected boolean canParent() {
        return !this.isVehicle() && !this.isPassenger() && this.isTamed() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_149506_, AgeableMob p_149507_) {
        return null;
    }

    protected void setOffspringAttributes(AgeableMob pParent, AbstractHorse pChild) {
        this.setOffspringAttribute(pParent, pChild, Attributes.MAX_HEALTH, (double)MIN_HEALTH, (double)MAX_HEALTH);
        this.setOffspringAttribute(pParent, pChild, Attributes.JUMP_STRENGTH, (double)MIN_JUMP_STRENGTH, (double)MAX_JUMP_STRENGTH);
        this.setOffspringAttribute(pParent, pChild, Attributes.MOVEMENT_SPEED, (double)MIN_MOVEMENT_SPEED, (double)MAX_MOVEMENT_SPEED);
    }

    private void setOffspringAttribute(AgeableMob pParent, AbstractHorse pChild, Holder<Attribute> pAttribute, double pMin, double pMax) {
        double d0 = createOffspringAttribute(this.getAttributeBaseValue(pAttribute), pParent.getAttributeBaseValue(pAttribute), pMin, pMax, this.random);
        pChild.getAttribute(pAttribute).setBaseValue(d0);
    }

    static double createOffspringAttribute(double pValue1, double pValue2, double pMin, double pMax, RandomSource pRandom) {
        if (pMax <= pMin) {
            throw new IllegalArgumentException("Incorrect range for an attribute");
        } else {
            pValue1 = Mth.clamp(pValue1, pMin, pMax);
            pValue2 = Mth.clamp(pValue2, pMin, pMax);
            double d0 = 0.15 * (pMax - pMin);
            double d1 = Math.abs(pValue1 - pValue2) + d0 * 2.0;
            double d2 = (pValue1 + pValue2) / 2.0;
            double d3 = (pRandom.nextDouble() + pRandom.nextDouble() + pRandom.nextDouble()) / 3.0 - 0.5;
            double d4 = d2 + d1 * d3;
            if (d4 > pMax) {
                double d6 = d4 - pMax;
                return pMax - d6;
            } else if (d4 < pMin) {
                double d5 = pMin - d4;
                return pMin + d5;
            } else {
                return d4;
            }
        }
    }

    public float getEatAnim(float pPartialTick) {
        return Mth.lerp(pPartialTick, this.eatAnimO, this.eatAnim);
    }

    public float getStandAnim(float pPartialTick) {
        return Mth.lerp(pPartialTick, this.standAnimO, this.standAnim);
    }

    public float getMouthAnim(float pPartialTick) {
        return Mth.lerp(pPartialTick, this.mouthAnimO, this.mouthAnim);
    }

    @Override
    public void onPlayerJump(int pJumpPower) {
        if (this.isSaddled()) {
            if (pJumpPower < 0) {
                pJumpPower = 0;
            } else {
                this.allowStandSliding = true;
                this.standIfPossible();
            }

            if (pJumpPower >= 90) {
                this.playerJumpPendingScale = 1.0F;
            } else {
                this.playerJumpPendingScale = 0.4F + 0.4F * (float)pJumpPower / 90.0F;
            }
        }
    }

    @Override
    public boolean canJump() {
        return this.isSaddled();
    }

    @Override
    public void handleStartJump(int pJumpPower) {
        this.allowStandSliding = true;
        this.standIfPossible();
        this.playJumpSound();
    }

    @Override
    public void handleStopJump() {
    }

    protected void spawnTamingParticles(boolean pTamed) {
        ParticleOptions particleoptions = pTamed ? ParticleTypes.HEART : ParticleTypes.SMOKE;

        for (int i = 0; i < 7; i++) {
            double d0 = this.random.nextGaussian() * 0.02;
            double d1 = this.random.nextGaussian() * 0.02;
            double d2 = this.random.nextGaussian() * 0.02;
            this.level().addParticle(particleoptions, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), d0, d1, d2);
        }
    }

    @Override
    public void handleEntityEvent(byte p_30541_) {
        if (p_30541_ == 7) {
            this.spawnTamingParticles(true);
        } else if (p_30541_ == 6) {
            this.spawnTamingParticles(false);
        } else {
            super.handleEntityEvent(p_30541_);
        }
    }

    @Override
    protected void positionRider(Entity p_289569_, Entity.MoveFunction p_289558_) {
        super.positionRider(p_289569_, p_289558_);
        if (p_289569_ instanceof LivingEntity) {
            ((LivingEntity)p_289569_).yBodyRot = this.yBodyRot;
        }
    }

    protected static float generateMaxHealth(IntUnaryOperator pOperator) {
        return 15.0F + (float)pOperator.applyAsInt(8) + (float)pOperator.applyAsInt(9);
    }

    protected static double generateJumpStrength(DoubleSupplier pSupplier) {
        return 0.4F + pSupplier.getAsDouble() * 0.2 + pSupplier.getAsDouble() * 0.2 + pSupplier.getAsDouble() * 0.2;
    }

    protected static double generateSpeed(DoubleSupplier pSupplier) {
        return (0.45F + pSupplier.getAsDouble() * 0.3 + pSupplier.getAsDouble() * 0.3 + pSupplier.getAsDouble() * 0.3) * 0.25;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public SlotAccess getSlot(int p_149514_) {
        int i = p_149514_ - 400;
        if (i == 0) {
            return new SlotAccess() {
                @Override
                public ItemStack get() {
                    return AbstractHorse.this.inventory.getItem(0);
                }

                @Override
                public boolean set(ItemStack p_331180_) {
                    if (!p_331180_.isEmpty() && !p_331180_.is(Items.SADDLE)) {
                        return false;
                    } else {
                        AbstractHorse.this.inventory.setItem(0, p_331180_);
                        AbstractHorse.this.syncSaddleToClients();
                        return true;
                    }
                }
            };
        } else {
            int j = p_149514_ - 500 + 1;
            return j >= 1 && j < this.inventory.getContainerSize() ? SlotAccess.forContainer(this.inventory, j) : super.getSlot(p_149514_);
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (this.isSaddled()) {
            Entity entity = this.getFirstPassenger();
            if (entity instanceof Player) {
                return (Player)entity;
            }
        }

        return super.getControllingPassenger();
    }

    @Nullable
    private Vec3 getDismountLocationInDirection(Vec3 pDirection, LivingEntity pPassenger) {
        double d0 = this.getX() + pDirection.x;
        double d1 = this.getBoundingBox().minY;
        double d2 = this.getZ() + pDirection.z;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Pose pose : pPassenger.getDismountPoses()) {
            blockpos$mutableblockpos.set(d0, d1, d2);
            double d3 = this.getBoundingBox().maxY + 0.75;

            do {
                double d4 = this.level().getBlockFloorHeight(blockpos$mutableblockpos);
                if ((double)blockpos$mutableblockpos.getY() + d4 > d3) {
                    break;
                }

                if (DismountHelper.isBlockFloorValid(d4)) {
                    AABB aabb = pPassenger.getLocalBoundsForPose(pose);
                    Vec3 vec3 = new Vec3(d0, (double)blockpos$mutableblockpos.getY() + d4, d2);
                    if (DismountHelper.canDismountTo(this.level(), pPassenger, aabb.move(vec3))) {
                        pPassenger.setPose(pose);
                        return vec3;
                    }
                }

                blockpos$mutableblockpos.move(Direction.UP);
            } while (!((double)blockpos$mutableblockpos.getY() < d3));
        }

        return null;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity pLivingEntity) {
        Vec3 vec3 = getCollisionHorizontalEscapeVector(
            (double)this.getBbWidth(), (double)pLivingEntity.getBbWidth(), this.getYRot() + (pLivingEntity.getMainArm() == HumanoidArm.RIGHT ? 90.0F : -90.0F)
        );
        Vec3 vec31 = this.getDismountLocationInDirection(vec3, pLivingEntity);
        if (vec31 != null) {
            return vec31;
        } else {
            Vec3 vec32 = getCollisionHorizontalEscapeVector(
                (double)this.getBbWidth(), (double)pLivingEntity.getBbWidth(), this.getYRot() + (pLivingEntity.getMainArm() == HumanoidArm.LEFT ? 90.0F : -90.0F)
            );
            Vec3 vec33 = this.getDismountLocationInDirection(vec32, pLivingEntity);
            return vec33 != null ? vec33 : this.position();
        }
    }

    protected void randomizeAttributes(RandomSource pRandom) {
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_30555_, DifficultyInstance p_30556_, EntitySpawnReason p_365113_, @Nullable SpawnGroupData p_30558_) {
        if (p_30558_ == null) {
            p_30558_ = new AgeableMob.AgeableMobGroupData(0.2F);
        }

        this.randomizeAttributes(p_30555_.getRandom());
        return super.finalizeSpawn(p_30555_, p_30556_, p_365113_, p_30558_);
    }

    public boolean hasInventoryChanged(Container pInventory) {
        return this.inventory != pInventory;
    }

    public int getAmbientStandInterval() {
        return this.getAmbientSoundInterval();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity p_301103_, EntityDimensions p_298879_, float p_299886_) {
        return super.getPassengerAttachmentPoint(p_301103_, p_298879_, p_299886_)
            .add(
                new Vec3(0.0, 0.15 * (double)this.standAnimO * (double)p_299886_, -0.7 * (double)this.standAnimO * (double)p_299886_)
                    .yRot(-this.getYRot() * (float) (Math.PI / 180.0))
            );
    }

    public final Container getBodyArmorAccess() {
        return this.bodyArmorAccess;
    }

    public int getInventoryColumns() {
        return 0;
    }
}