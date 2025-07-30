package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeLivingEntity;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fluids.FluidType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public abstract class Mob extends LivingEntity implements EquipmentUser, Leashable, Targeting, IForgeLivingEntity {
    private static final EntityDataAccessor<Byte> DATA_MOB_FLAGS_ID = SynchedEntityData.defineId(Mob.class, EntityDataSerializers.BYTE);
    private static final int MOB_FLAG_NO_AI = 1;
    private static final int MOB_FLAG_LEFTHANDED = 2;
    private static final int MOB_FLAG_AGGRESSIVE = 4;
    protected static final int PICKUP_REACH = 1;
    private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 0, 1);
    private static final List<EquipmentSlot> EQUIPMENT_POPULATION_ORDER = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    public static final float MAX_WEARING_ARMOR_CHANCE = 0.15F;
    public static final float MAX_PICKUP_LOOT_CHANCE = 0.55F;
    public static final float MAX_ENCHANTED_ARMOR_CHANCE = 0.5F;
    public static final float MAX_ENCHANTED_WEAPON_CHANCE = 0.25F;
    public static final float DEFAULT_EQUIPMENT_DROP_CHANCE = 0.085F;
    public static final float PRESERVE_ITEM_DROP_CHANCE_THRESHOLD = 1.0F;
    public static final int PRESERVE_ITEM_DROP_CHANCE = 2;
    public static final int UPDATE_GOAL_SELECTOR_EVERY_N_TICKS = 2;
    private static final double DEFAULT_ATTACK_REACH = Math.sqrt(2.04F) - 0.6F;
    protected static final ResourceLocation RANDOM_SPAWN_BONUS_ID = ResourceLocation.withDefaultNamespace("random_spawn_bonus");
    public int ambientSoundTime;
    protected int xpReward;
    protected LookControl lookControl;
    protected MoveControl moveControl;
    protected JumpControl jumpControl;
    private final BodyRotationControl bodyRotationControl;
    protected PathNavigation navigation;
    protected final GoalSelector goalSelector;
    protected final GoalSelector targetSelector;
    @Nullable
    private LivingEntity target;
    private final Sensing sensing;
    private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    protected final float[] handDropChances = new float[2];
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
    protected final float[] armorDropChances = new float[4];
    private ItemStack bodyArmorItem = ItemStack.EMPTY;
    protected float bodyArmorDropChance;
    private boolean canPickUpLoot;
    private boolean persistenceRequired;
    private final Map<PathType, Float> pathfindingMalus = Maps.newEnumMap(PathType.class);
    private Optional<ResourceKey<LootTable>> lootTable = Optional.empty();
    private long lootTableSeed;
    @Nullable
    private Leashable.LeashData leashData;
    private BlockPos restrictCenter = BlockPos.ZERO;
    private float restrictRadius = -1.0F;
    private EntitySpawnReason spawnReason;
    private boolean spawnCancelled = false;

    protected Mob(EntityType<? extends Mob> p_21368_, Level p_21369_) {
        super(p_21368_, p_21369_);
        this.goalSelector = new GoalSelector();
        this.targetSelector = new GoalSelector();
        this.lookControl = new LookControl(this);
        this.moveControl = new MoveControl(this);
        this.jumpControl = new JumpControl(this);
        this.bodyRotationControl = this.createBodyControl();
        this.navigation = this.createNavigation(p_21369_);
        this.sensing = new Sensing(this);
        Arrays.fill(this.armorDropChances, 0.085F);
        Arrays.fill(this.handDropChances, 0.085F);
        this.bodyArmorDropChance = 0.085F;
        if (p_21369_ instanceof ServerLevel) {
            this.registerGoals();
        }
    }

    protected void registerGoals() {
    }

    public static AttributeSupplier.Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 16.0);
    }

    protected PathNavigation createNavigation(Level pLevel) {
        return new GroundPathNavigation(this, pLevel);
    }

    protected boolean shouldPassengersInheritMalus() {
        return false;
    }

    public float getPathfindingMalus(PathType pPathType) {
        Mob mob;
        label17: {
            if (this.getControlledVehicle() instanceof Mob mob1 && mob1.shouldPassengersInheritMalus()) {
                mob = mob1;
                break label17;
            }

            mob = this;
        }

        Float f = mob.pathfindingMalus.get(pPathType);
        return f == null ? pPathType.getMalus() : f;
    }

    public void setPathfindingMalus(PathType pPathType, float pMalus) {
        this.pathfindingMalus.put(pPathType, pMalus);
    }

    public void onPathfindingStart() {
    }

    public void onPathfindingDone() {
    }

    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this);
    }

    public LookControl getLookControl() {
        return this.lookControl;
    }

    public MoveControl getMoveControl() {
        return this.getControlledVehicle() instanceof Mob mob ? mob.getMoveControl() : this.moveControl;
    }

    public JumpControl getJumpControl() {
        return this.jumpControl;
    }

    public PathNavigation getNavigation() {
        return this.getControlledVehicle() instanceof Mob mob ? mob.getNavigation() : this.navigation;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity entity = this.getFirstPassenger();
        if (!this.isNoAi() && entity instanceof Mob mob && entity.canControlVehicle()) {
            return mob;
        }

        return null;
    }

    public Sensing getSensing() {
        return this.sensing;
    }

    @Nullable
    @Override
    public LivingEntity getTarget() {
        return this.target;
    }

    @Nullable
    protected final LivingEntity getTargetFromBrain() {
        return this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    public void setTarget(@Nullable LivingEntity pTarget) {
        if (Reflector.ForgeEventFactory_onLivingChangeTargetMob.exists()) {
            LivingChangeTargetEvent livingchangetargetevent = (LivingChangeTargetEvent)Reflector.ForgeEventFactory_onLivingChangeTargetMob.call(this, pTarget);
            if (!livingchangetargetevent.isCanceled()) {
                this.target = livingchangetargetevent.getNewTarget();
            }
        } else {
            this.target = pTarget;
        }
    }

    @Override
    public boolean canAttackType(EntityType<?> pType) {
        return pType != EntityType.GHAST;
    }

    public boolean canFireProjectileWeapon(ProjectileWeaponItem pProjectileWeapon) {
        return false;
    }

    public void ate() {
        this.gameEvent(GameEvent.EAT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_335882_) {
        super.defineSynchedData(p_335882_);
        p_335882_.define(DATA_MOB_FLAGS_ID, (byte)0);
    }

    public int getAmbientSoundInterval() {
        return 80;
    }

    public void playAmbientSound() {
        this.makeSound(this.getAmbientSound());
    }

    @Override
    public void baseTick() {
        super.baseTick();
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("mobBaseTick");
        if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
            this.resetAmbientSoundTime();
            this.playAmbientSound();
        }

        profilerfiller.pop();
    }

    @Override
    protected void playHurtSound(DamageSource pSource) {
        this.resetAmbientSoundTime();
        super.playHurtSound(pSource);
    }

    private void resetAmbientSoundTime() {
        this.ambientSoundTime = -this.getAmbientSoundInterval();
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel p_369877_) {
        if (this.xpReward > 0) {
            int i = this.xpReward;

            for (int j = 0; j < this.armorItems.size(); j++) {
                if (!this.armorItems.get(j).isEmpty() && this.armorDropChances[j] <= 1.0F) {
                    i += 1 + this.random.nextInt(3);
                }
            }

            for (int k = 0; k < this.handItems.size(); k++) {
                if (!this.handItems.get(k).isEmpty() && this.handDropChances[k] <= 1.0F) {
                    i += 1 + this.random.nextInt(3);
                }
            }

            if (!this.bodyArmorItem.isEmpty() && this.bodyArmorDropChance <= 1.0F) {
                i += 1 + this.random.nextInt(3);
            }

            return i;
        } else {
            return this.xpReward;
        }
    }

    public void spawnAnim() {
        if (this.level().isClientSide) {
            this.makePoofParticles();
        } else {
            this.level().broadcastEntityEvent(this, (byte)20);
        }
    }

    @Override
    public void handleEntityEvent(byte p_21375_) {
        if (p_21375_ == 20) {
            this.spawnAnim();
        } else {
            super.handleEntityEvent(p_21375_);
        }
    }

    @Override
    public void tick() {
        if (Config.isSmoothWorld() && this.canSkipUpdate()) {
            this.onUpdateMinimal();
        } else {
            super.tick();
            if (!this.level().isClientSide && this.tickCount % 5 == 0) {
                this.updateControlFlags();
            }
        }
    }

    protected void updateControlFlags() {
        boolean flag = !(this.getControllingPassenger() instanceof Mob);
        boolean flag1 = !(this.getVehicle() instanceof AbstractBoat);
        this.goalSelector.setControlFlag(Goal.Flag.MOVE, flag);
        this.goalSelector.setControlFlag(Goal.Flag.JUMP, flag && flag1);
        this.goalSelector.setControlFlag(Goal.Flag.LOOK, flag);
    }

    @Override
    protected float tickHeadTurn(float p_21538_, float p_21539_) {
        this.bodyRotationControl.clientTick();
        return p_21539_;
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("CanPickUpLoot", this.canPickUpLoot());
        pCompound.putBoolean("PersistenceRequired", this.persistenceRequired);
        ListTag listtag = new ListTag();

        for (ItemStack itemstack : this.armorItems) {
            if (!itemstack.isEmpty()) {
                listtag.add(itemstack.save(this.registryAccess()));
            } else {
                listtag.add(new CompoundTag());
            }
        }

        pCompound.put("ArmorItems", listtag);
        ListTag listtag1 = new ListTag();

        for (float f : this.armorDropChances) {
            listtag1.add(FloatTag.valueOf(f));
        }

        pCompound.put("ArmorDropChances", listtag1);
        ListTag listtag2 = new ListTag();

        for (ItemStack itemstack1 : this.handItems) {
            if (!itemstack1.isEmpty()) {
                listtag2.add(itemstack1.save(this.registryAccess()));
            } else {
                listtag2.add(new CompoundTag());
            }
        }

        pCompound.put("HandItems", listtag2);
        ListTag listtag3 = new ListTag();

        for (float f1 : this.handDropChances) {
            listtag3.add(FloatTag.valueOf(f1));
        }

        pCompound.put("HandDropChances", listtag3);
        if (!this.bodyArmorItem.isEmpty()) {
            pCompound.put("body_armor_item", this.bodyArmorItem.save(this.registryAccess()));
            pCompound.putFloat("body_armor_drop_chance", this.bodyArmorDropChance);
        }

        this.writeLeashData(pCompound, this.leashData);
        pCompound.putBoolean("LeftHanded", this.isLeftHanded());
        if (this.lootTable.isPresent()) {
            pCompound.putString("DeathLootTable", this.lootTable.get().location().toString());
        }

        if (this.lootTableSeed != 0L) {
            pCompound.putLong("DeathLootTableSeed", this.lootTableSeed);
        }

        if (this.isNoAi()) {
            pCompound.putBoolean("NoAI", this.isNoAi());
        }

        if (this.spawnReason != null) {
            pCompound.putString("forge:spawn_type", this.spawnReason.name());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setCanPickUpLoot(pCompound.getBoolean("CanPickUpLoot"));
        this.persistenceRequired = pCompound.getBoolean("PersistenceRequired");
        if (pCompound.contains("ArmorItems", 9)) {
            ListTag listtag = pCompound.getList("ArmorItems", 10);

            for (int i = 0; i < this.armorItems.size(); i++) {
                CompoundTag compoundtag = listtag.getCompound(i);
                this.armorItems.set(i, ItemStack.parseOptional(this.registryAccess(), compoundtag));
            }
        } else {
            this.armorItems.replaceAll(voidIn -> ItemStack.EMPTY);
        }

        if (pCompound.contains("ArmorDropChances", 9)) {
            ListTag listtag1 = pCompound.getList("ArmorDropChances", 5);

            for (int j = 0; j < listtag1.size(); j++) {
                this.armorDropChances[j] = listtag1.getFloat(j);
            }
        } else {
            Arrays.fill(this.armorDropChances, 0.0F);
        }

        if (pCompound.contains("HandItems", 9)) {
            ListTag listtag2 = pCompound.getList("HandItems", 10);

            for (int k = 0; k < this.handItems.size(); k++) {
                CompoundTag compoundtag1 = listtag2.getCompound(k);
                this.handItems.set(k, ItemStack.parseOptional(this.registryAccess(), compoundtag1));
            }
        } else {
            this.handItems.replaceAll(voidIn -> ItemStack.EMPTY);
        }

        if (pCompound.contains("HandDropChances", 9)) {
            ListTag listtag3 = pCompound.getList("HandDropChances", 5);

            for (int l = 0; l < listtag3.size(); l++) {
                this.handDropChances[l] = listtag3.getFloat(l);
            }
        } else {
            Arrays.fill(this.handDropChances, 0.0F);
        }

        if (pCompound.contains("body_armor_item", 10)) {
            this.bodyArmorItem = ItemStack.parse(this.registryAccess(), pCompound.getCompound("body_armor_item")).orElse(ItemStack.EMPTY);
            this.bodyArmorDropChance = pCompound.getFloat("body_armor_drop_chance");
        } else {
            this.bodyArmorItem = ItemStack.EMPTY;
        }

        this.readLeashData(pCompound);
        this.setLeftHanded(pCompound.getBoolean("LeftHanded"));
        if (pCompound.contains("DeathLootTable", 8)) {
            this.lootTable = Optional.of(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(pCompound.getString("DeathLootTable"))));
        } else {
            this.lootTable = Optional.empty();
        }

        this.lootTableSeed = pCompound.getLong("DeathLootTableSeed");
        this.setNoAi(pCompound.getBoolean("NoAI"));
        if (pCompound.contains("forge:spawn_type")) {
            try {
                this.spawnReason = EntitySpawnReason.valueOf(pCompound.getString("forge:spawn_type"));
            } catch (Exception exception) {
                pCompound.remove("forge:spawn_type");
            }
        }
    }

    @Override
    protected void dropFromLootTable(ServerLevel p_367479_, DamageSource p_21389_, boolean p_21390_) {
        super.dropFromLootTable(p_367479_, p_21389_, p_21390_);
        this.lootTable = Optional.empty();
    }

    @Override
    public final Optional<ResourceKey<LootTable>> getLootTable() {
        return this.lootTable.isPresent() ? this.lootTable : super.getLootTable();
    }

    @Override
    public long getLootTableSeed() {
        return this.lootTableSeed;
    }

    public void setZza(float pAmount) {
        this.zza = pAmount;
    }

    public void setYya(float pAmount) {
        this.yya = pAmount;
    }

    public void setXxa(float pAmount) {
        this.xxa = pAmount;
    }

    @Override
    public void setSpeed(float pSpeed) {
        super.setSpeed(pSpeed);
        this.setZza(pSpeed);
    }

    public void stopInPlace() {
        this.getNavigation().stop();
        this.setXxa(0.0F);
        this.setYya(0.0F);
        this.setSpeed(0.0F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("looting");
        boolean flag = this.level() instanceof ServerLevel serverlevel ? serverlevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) : false;
        if (Reflector.ForgeEventFactory_getMobGriefingEvent.exists() && this.level() instanceof ServerLevel serverlevel1) {
            flag = Reflector.callBoolean(Reflector.ForgeEventFactory_getMobGriefingEvent, serverlevel1, this);
        }

        if (this.level() instanceof ServerLevel serverlevel2 && this.canPickUpLoot() && this.isAlive() && !this.dead && flag) {
            Vec3i vec3i = this.getPickupReach();

            for (ItemEntity itementity : this.level()
                .getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate((double)vec3i.getX(), (double)vec3i.getY(), (double)vec3i.getZ()))) {
                if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && this.wantsToPickUp(serverlevel2, itementity.getItem())
                    )
                 {
                    this.pickUpItem(serverlevel2, itementity);
                }
            }
        }

        profilerfiller.pop();
    }

    protected Vec3i getPickupReach() {
        return ITEM_PICKUP_REACH;
    }

    protected void pickUpItem(ServerLevel pLevel, ItemEntity pEntity) {
        ItemStack itemstack = pEntity.getItem();
        ItemStack itemstack1 = this.equipItemIfPossible(pLevel, itemstack.copy());
        if (!itemstack1.isEmpty()) {
            this.onItemPickup(pEntity);
            this.take(pEntity, itemstack1.getCount());
            itemstack.shrink(itemstack1.getCount());
            if (itemstack.isEmpty()) {
                pEntity.discard();
            }
        }
    }

    public ItemStack equipItemIfPossible(ServerLevel pLevel, ItemStack pStack) {
        EquipmentSlot equipmentslot = this.getEquipmentSlotForItem(pStack);
        ItemStack itemstack = this.getItemBySlot(equipmentslot);
        boolean flag = this.canReplaceCurrentItem(pStack, itemstack, equipmentslot);
        if (equipmentslot.isArmor() && !flag) {
            equipmentslot = EquipmentSlot.MAINHAND;
            itemstack = this.getItemBySlot(equipmentslot);
            flag = itemstack.isEmpty();
        }

        if (flag && this.canHoldItem(pStack)) {
            double d0 = (double)this.getEquipmentDropChance(equipmentslot);
            if (!itemstack.isEmpty() && (double)Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d0) {
                this.spawnAtLocation(pLevel, itemstack);
            }

            ItemStack itemstack1 = equipmentslot.limit(pStack);
            this.setItemSlotAndDropWhenKilled(equipmentslot, itemstack1);
            return itemstack1;
        } else {
            return ItemStack.EMPTY;
        }
    }

    protected void setItemSlotAndDropWhenKilled(EquipmentSlot pSlot, ItemStack pStack) {
        this.setItemSlot(pSlot, pStack);
        this.setGuaranteedDrop(pSlot);
        this.persistenceRequired = true;
    }

    public void setGuaranteedDrop(EquipmentSlot pSlot) {
        switch (pSlot.getType()) {
            case HAND:
                this.handDropChances[pSlot.getIndex()] = 2.0F;
                break;
            case HUMANOID_ARMOR:
                this.armorDropChances[pSlot.getIndex()] = 2.0F;
                break;
            case ANIMAL_ARMOR:
                this.bodyArmorDropChance = 2.0F;
        }
    }

    protected boolean canReplaceCurrentItem(ItemStack pNewItem, ItemStack pCurrentItem, EquipmentSlot pSlot) {
        if (pCurrentItem.isEmpty()) {
            return true;
        } else if (pSlot.isArmor()) {
            return this.compareArmor(pNewItem, pCurrentItem, pSlot);
        } else {
            return pSlot == EquipmentSlot.MAINHAND ? this.compareWeapons(pNewItem, pCurrentItem, pSlot) : false;
        }
    }

    private boolean compareArmor(ItemStack pNewItem, ItemStack pCurrentItem, EquipmentSlot pSlot) {
        if (EnchantmentHelper.has(pCurrentItem, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return false;
        } else {
            double d0 = this.getApproximateAttributeWith(pNewItem, Attributes.ARMOR, pSlot);
            double d1 = this.getApproximateAttributeWith(pCurrentItem, Attributes.ARMOR, pSlot);
            double d2 = this.getApproximateAttributeWith(pNewItem, Attributes.ARMOR_TOUGHNESS, pSlot);
            double d3 = this.getApproximateAttributeWith(pCurrentItem, Attributes.ARMOR_TOUGHNESS, pSlot);
            if (d0 != d1) {
                return d0 > d1;
            } else {
                return d2 != d3 ? d2 > d3 : this.canReplaceEqualItem(pNewItem, pCurrentItem);
            }
        }
    }

    private boolean compareWeapons(ItemStack pNewItem, ItemStack pCurrentItem, EquipmentSlot pSlot) {
        TagKey<Item> tagkey = this.getPreferredWeaponType();
        if (tagkey != null) {
            if (pCurrentItem.is(tagkey) && !pNewItem.is(tagkey)) {
                return false;
            }

            if (!pCurrentItem.is(tagkey) && pNewItem.is(tagkey)) {
                return true;
            }
        }

        double d0 = this.getApproximateAttributeWith(pNewItem, Attributes.ATTACK_DAMAGE, pSlot);
        double d1 = this.getApproximateAttributeWith(pCurrentItem, Attributes.ATTACK_DAMAGE, pSlot);
        return d0 != d1 ? d0 > d1 : this.canReplaceEqualItem(pNewItem, pCurrentItem);
    }

    private double getApproximateAttributeWith(ItemStack pItem, Holder<Attribute> pAttribute, EquipmentSlot pSlot) {
        double d0 = this.getAttributes().hasAttribute(pAttribute) ? this.getAttributeBaseValue(pAttribute) : 0.0;
        ItemAttributeModifiers itemattributemodifiers = pItem.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return itemattributemodifiers.compute(d0, pSlot);
    }

    public boolean canReplaceEqualItem(ItemStack pCandidate, ItemStack pExisting) {
        Set<Entry<Holder<Enchantment>>> set = pExisting.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
        Set<Entry<Holder<Enchantment>>> set1 = pCandidate.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
        if (set1.size() != set.size()) {
            return set1.size() > set.size();
        } else {
            int i = pCandidate.getDamageValue();
            int j = pExisting.getDamageValue();
            return i != j ? i < j : pCandidate.has(DataComponents.CUSTOM_NAME) && !pExisting.has(DataComponents.CUSTOM_NAME);
        }
    }

    public boolean canHoldItem(ItemStack pStack) {
        return true;
    }

    public boolean wantsToPickUp(ServerLevel pLevel, ItemStack pStack) {
        return this.canHoldItem(pStack);
    }

    @Nullable
    public TagKey<Item> getPreferredWeaponType() {
        return null;
    }

    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return true;
    }

    public boolean requiresCustomPersistence() {
        return this.isPassenger();
    }

    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
            this.discard();
        } else if (!this.isPersistenceRequired() && !this.requiresCustomPersistence()) {
            Entity entity = this.level().getNearestPlayer(this, -1.0);
            if (Reflector.ForgeEventFactory_canEntityDespawn.exists()) {
                Object object = Reflector.ForgeEventFactory_canEntityDespawn.call(this, this.level());
                if (object == Event.Result.DENY) {
                    this.noActionTime = 0;
                    entity = null;
                } else if (object == Event.Result.ALLOW) {
                    this.discard();
                    entity = null;
                }
            }

            if (entity != null) {
                double d0 = entity.distanceToSqr(this);
                int i = this.getType().getCategory().getDespawnDistance();
                int j = i * i;
                if (d0 > (double)j && this.removeWhenFarAway(d0)) {
                    this.discard();
                }

                int k = this.getType().getCategory().getNoDespawnDistance();
                int l = k * k;
                if (this.noActionTime > 600 && this.random.nextInt(800) == 0 && d0 > (double)l && this.removeWhenFarAway(d0)) {
                    this.discard();
                } else if (d0 < (double)l) {
                    this.noActionTime = 0;
                }
            }
        } else {
            this.noActionTime = 0;
        }
    }

    @Override
    protected final void serverAiStep() {
        this.noActionTime++;
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("sensing");
        this.sensing.tick();
        profilerfiller.pop();
        int i = this.tickCount + this.getId();
        if (i % 2 != 0 && this.tickCount > 1) {
            profilerfiller.push("targetSelector");
            this.targetSelector.tickRunningGoals(false);
            profilerfiller.pop();
            profilerfiller.push("goalSelector");
            this.goalSelector.tickRunningGoals(false);
            profilerfiller.pop();
        } else {
            profilerfiller.push("targetSelector");
            this.targetSelector.tick();
            profilerfiller.pop();
            profilerfiller.push("goalSelector");
            this.goalSelector.tick();
            profilerfiller.pop();
        }

        profilerfiller.push("navigation");
        this.navigation.tick();
        profilerfiller.pop();
        profilerfiller.push("mob tick");
        this.customServerAiStep((ServerLevel)this.level());
        profilerfiller.pop();
        profilerfiller.push("controls");
        profilerfiller.push("move");
        this.moveControl.tick();
        profilerfiller.popPush("look");
        this.lookControl.tick();
        profilerfiller.popPush("jump");
        this.jumpControl.tick();
        profilerfiller.pop();
        profilerfiller.pop();
        this.sendDebugPackets();
    }

    protected void sendDebugPackets() {
        DebugPackets.sendGoalSelector(this.level(), this, this.goalSelector);
    }

    protected void customServerAiStep(ServerLevel pLevel) {
    }

    public int getMaxHeadXRot() {
        return 40;
    }

    public int getMaxHeadYRot() {
        return 75;
    }

    protected void clampHeadRotationToBody() {
        float f = (float)this.getMaxHeadYRot();
        float f1 = this.getYHeadRot();
        float f2 = Mth.wrapDegrees(this.yBodyRot - f1);
        float f3 = Mth.clamp(Mth.wrapDegrees(this.yBodyRot - f1), -f, f);
        float f4 = f1 + f2 - f3;
        this.setYHeadRot(f4);
    }

    public int getHeadRotSpeed() {
        return 10;
    }

    public void lookAt(Entity pEntity, float pMaxYRotIncrease, float pMaxXRotIncrease) {
        double d0 = pEntity.getX() - this.getX();
        double d1 = pEntity.getZ() - this.getZ();
        double d2;
        if (pEntity instanceof LivingEntity livingentity) {
            d2 = livingentity.getEyeY() - this.getEyeY();
        } else {
            d2 = (pEntity.getBoundingBox().minY + pEntity.getBoundingBox().maxY) / 2.0 - this.getEyeY();
        }

        double d3 = Math.sqrt(d0 * d0 + d1 * d1);
        float f = (float)(Mth.atan2(d1, d0) * 180.0 / (float) Math.PI) - 90.0F;
        float f1 = (float)(-(Mth.atan2(d2, d3) * 180.0 / (float) Math.PI));
        this.setXRot(this.rotlerp(this.getXRot(), f1, pMaxXRotIncrease));
        this.setYRot(this.rotlerp(this.getYRot(), f, pMaxYRotIncrease));
    }

    private float rotlerp(float pAngle, float pTargetAngle, float pMaxIncrease) {
        float f = Mth.wrapDegrees(pTargetAngle - pAngle);
        if (f > pMaxIncrease) {
            f = pMaxIncrease;
        }

        if (f < -pMaxIncrease) {
            f = -pMaxIncrease;
        }

        return pAngle + f;
    }

    public static boolean checkMobSpawnRules(
        EntityType<? extends Mob> pEntityType, LevelAccessor pLevel, EntitySpawnReason pSpawnReason, BlockPos pPos, RandomSource pRandom
    ) {
        BlockPos blockpos = pPos.below();
        return EntitySpawnReason.isSpawner(pSpawnReason) || pLevel.getBlockState(blockpos).isValidSpawn(pLevel, blockpos, pEntityType);
    }

    public boolean checkSpawnRules(LevelAccessor pLevel, EntitySpawnReason pSpawnReason) {
        return true;
    }

    public boolean checkSpawnObstruction(LevelReader pLevel) {
        return !pLevel.containsAnyLiquid(this.getBoundingBox()) && pLevel.isUnobstructed(this);
    }

    public int getMaxSpawnClusterSize() {
        return 4;
    }

    public boolean isMaxGroupSizeReached(int pSize) {
        return false;
    }

    @Override
    public int getMaxFallDistance() {
        if (this.getTarget() == null) {
            return this.getComfortableFallDistance(0.0F);
        } else {
            int i = (int)(this.getHealth() - this.getMaxHealth() * 0.33F);
            i -= (3 - this.level().getDifficulty().getId()) * 4;
            if (i < 0) {
                i = 0;
            }

            return this.getComfortableFallDistance((float)i);
        }
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return this.handItems;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return this.armorItems;
    }

    public ItemStack getBodyArmorItem() {
        return this.bodyArmorItem;
    }

    @Override
    public boolean canUseSlot(EquipmentSlot p_334488_) {
        return p_334488_ != EquipmentSlot.BODY;
    }

    public boolean isWearingBodyArmor() {
        return !this.getItemBySlot(EquipmentSlot.BODY).isEmpty();
    }

    public void setBodyArmorItem(ItemStack pStack) {
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, pStack);
    }

    @Override
    public Iterable<ItemStack> getArmorAndBodyArmorSlots() {
        return (Iterable<ItemStack>)(this.bodyArmorItem.isEmpty() ? this.armorItems : Iterables.concat(this.armorItems, List.of(this.bodyArmorItem)));
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot pSlot) {
        return switch (pSlot.getType()) {
            case HAND -> (ItemStack)this.handItems.get(pSlot.getIndex());
            case HUMANOID_ARMOR -> (ItemStack)this.armorItems.get(pSlot.getIndex());
            case ANIMAL_ARMOR -> this.bodyArmorItem;
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack) {
        this.verifyEquippedItem(pStack);
        switch (pSlot.getType()) {
            case HAND:
                this.onEquipItem(pSlot, this.handItems.set(pSlot.getIndex(), pStack), pStack);
                break;
            case HUMANOID_ARMOR:
                this.onEquipItem(pSlot, this.armorItems.set(pSlot.getIndex(), pStack), pStack);
                break;
            case ANIMAL_ARMOR:
                ItemStack itemstack = this.bodyArmorItem;
                this.bodyArmorItem = pStack;
                this.onEquipItem(pSlot, itemstack, pStack);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel p_345102_, DamageSource p_21385_, boolean p_21387_) {
        super.dropCustomDeathLoot(p_345102_, p_21385_, p_21387_);

        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            float f = this.getEquipmentDropChance(equipmentslot);
            if (f != 0.0F) {
                boolean flag = f > 1.0F;
                Entity entity = p_21385_.getEntity();
                if (entity instanceof LivingEntity) {
                    LivingEntity livingentity = (LivingEntity)entity;
                    if (this.level() instanceof ServerLevel serverlevel) {
                        f = EnchantmentHelper.processEquipmentDropChance(serverlevel, livingentity, p_21385_, f);
                    }
                }

                if (!itemstack.isEmpty()
                    && !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)
                    && (p_21387_ || flag)
                    && this.random.nextFloat() < f) {
                    if (!flag && itemstack.isDamageableItem()) {
                        itemstack.setDamageValue(itemstack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemstack.getMaxDamage() - 3, 1))));
                    }

                    this.spawnAtLocation(p_345102_, itemstack);
                    this.setItemSlot(equipmentslot, ItemStack.EMPTY);
                }
            }
        }
    }

    protected float getEquipmentDropChance(EquipmentSlot pSlot) {
        return switch (pSlot.getType()) {
            case HAND -> this.handDropChances[pSlot.getIndex()];
            case HUMANOID_ARMOR -> this.armorDropChances[pSlot.getIndex()];
            case ANIMAL_ARMOR -> this.bodyArmorDropChance;
        };
    }

    public void dropPreservedEquipment(ServerLevel pLevel) {
        this.dropPreservedEquipment(pLevel, goalIn -> true);
    }

    public Set<EquipmentSlot> dropPreservedEquipment(ServerLevel pLevel, Predicate<ItemStack> pFilter) {
        Set<EquipmentSlot> set = new HashSet<>();

        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            if (!itemstack.isEmpty()) {
                if (!pFilter.test(itemstack)) {
                    set.add(equipmentslot);
                } else {
                    double d0 = (double)this.getEquipmentDropChance(equipmentslot);
                    if (d0 > 1.0) {
                        this.setItemSlot(equipmentslot, ItemStack.EMPTY);
                        this.spawnAtLocation(pLevel, itemstack);
                    }
                }
            }
        }

        return set;
    }

    private LootParams createEquipmentParams(ServerLevel pLevel) {
        return new LootParams.Builder(pLevel)
            .withParameter(LootContextParams.ORIGIN, this.position())
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .create(LootContextParamSets.EQUIPMENT);
    }

    public void equip(EquipmentTable pEquipmentTable) {
        this.equip(pEquipmentTable.lootTable(), pEquipmentTable.slotDropChances());
    }

    public void equip(ResourceKey<LootTable> pEquipmentLootTable, Map<EquipmentSlot, Float> pSlotDropChances) {
        if (this.level() instanceof ServerLevel serverlevel) {
            this.equip(pEquipmentLootTable, this.createEquipmentParams(serverlevel), pSlotDropChances);
        }
    }

    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        if (pRandom.nextFloat() < 0.15F * pDifficulty.getSpecialMultiplier()) {
            int i = pRandom.nextInt(2);
            float f = this.level().getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F;
            if (pRandom.nextFloat() < 0.095F) {
                i++;
            }

            if (pRandom.nextFloat() < 0.095F) {
                i++;
            }

            if (pRandom.nextFloat() < 0.095F) {
                i++;
            }

            boolean flag = true;

            for (EquipmentSlot equipmentslot : EQUIPMENT_POPULATION_ORDER) {
                ItemStack itemstack = this.getItemBySlot(equipmentslot);
                if (!flag && pRandom.nextFloat() < f) {
                    break;
                }

                flag = false;
                if (itemstack.isEmpty()) {
                    Item item = getEquipmentForSlot(equipmentslot, i);
                    if (item != null) {
                        this.setItemSlot(equipmentslot, new ItemStack(item));
                    }
                }
            }
        }
    }

    @Nullable
    public static Item getEquipmentForSlot(EquipmentSlot pSlot, int pChance) {
        switch (pSlot) {
            case HEAD:
                if (pChance == 0) {
                    return Items.LEATHER_HELMET;
                } else if (pChance == 1) {
                    return Items.GOLDEN_HELMET;
                } else if (pChance == 2) {
                    return Items.CHAINMAIL_HELMET;
                } else if (pChance == 3) {
                    return Items.IRON_HELMET;
                } else if (pChance == 4) {
                    return Items.DIAMOND_HELMET;
                }
            case CHEST:
                if (pChance == 0) {
                    return Items.LEATHER_CHESTPLATE;
                } else if (pChance == 1) {
                    return Items.GOLDEN_CHESTPLATE;
                } else if (pChance == 2) {
                    return Items.CHAINMAIL_CHESTPLATE;
                } else if (pChance == 3) {
                    return Items.IRON_CHESTPLATE;
                } else if (pChance == 4) {
                    return Items.DIAMOND_CHESTPLATE;
                }
            case LEGS:
                if (pChance == 0) {
                    return Items.LEATHER_LEGGINGS;
                } else if (pChance == 1) {
                    return Items.GOLDEN_LEGGINGS;
                } else if (pChance == 2) {
                    return Items.CHAINMAIL_LEGGINGS;
                } else if (pChance == 3) {
                    return Items.IRON_LEGGINGS;
                } else if (pChance == 4) {
                    return Items.DIAMOND_LEGGINGS;
                }
            case FEET:
                if (pChance == 0) {
                    return Items.LEATHER_BOOTS;
                } else if (pChance == 1) {
                    return Items.GOLDEN_BOOTS;
                } else if (pChance == 2) {
                    return Items.CHAINMAIL_BOOTS;
                } else if (pChance == 3) {
                    return Items.IRON_BOOTS;
                } else if (pChance == 4) {
                    return Items.DIAMOND_BOOTS;
                }
            default:
                return null;
        }
    }

    protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor pLevel, RandomSource pRandom, DifficultyInstance pDifficulty) {
        this.enchantSpawnedWeapon(pLevel, pRandom, pDifficulty);

        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            if (equipmentslot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                this.enchantSpawnedArmor(pLevel, pRandom, equipmentslot, pDifficulty);
            }
        }
    }

    protected void enchantSpawnedWeapon(ServerLevelAccessor pLevel, RandomSource pRandom, DifficultyInstance pDifficulty) {
        this.enchantSpawnedEquipment(pLevel, EquipmentSlot.MAINHAND, pRandom, 0.25F, pDifficulty);
    }

    protected void enchantSpawnedArmor(ServerLevelAccessor pLevel, RandomSource pRandom, EquipmentSlot pSlot, DifficultyInstance pDifficulty) {
        this.enchantSpawnedEquipment(pLevel, pSlot, pRandom, 0.5F, pDifficulty);
    }

    private void enchantSpawnedEquipment(ServerLevelAccessor pLevel, EquipmentSlot pSlot, RandomSource pRandom, float pEnchantChance, DifficultyInstance pDifficulty) {
        ItemStack itemstack = this.getItemBySlot(pSlot);
        if (!itemstack.isEmpty() && pRandom.nextFloat() < pEnchantChance * pDifficulty.getSpecialMultiplier()) {
            EnchantmentHelper.enchantItemFromProvider(itemstack, pLevel.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, pDifficulty, pRandom);
            this.setItemSlot(pSlot, itemstack);
        }
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, EntitySpawnReason pSpawnReason, @Nullable SpawnGroupData pSpawnGroupData) {
        RandomSource randomsource = pLevel.getRandom();
        AttributeInstance attributeinstance = Objects.requireNonNull(this.getAttribute(Attributes.FOLLOW_RANGE));
        if (!attributeinstance.hasModifier(RANDOM_SPAWN_BONUS_ID)) {
            attributeinstance.addPermanentModifier(
                new AttributeModifier(RANDOM_SPAWN_BONUS_ID, randomsource.triangle(0.0, 0.11485000000000001), AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            );
        }

        this.setLeftHanded(randomsource.nextFloat() < 0.05F);
        this.spawnReason = pSpawnReason;
        return pSpawnGroupData;
    }

    public void setPersistenceRequired() {
        this.persistenceRequired = true;
    }

    @Override
    public void setDropChance(EquipmentSlot pSlot, float pChance) {
        switch (pSlot.getType()) {
            case HAND:
                this.handDropChances[pSlot.getIndex()] = pChance;
                break;
            case HUMANOID_ARMOR:
                this.armorDropChances[pSlot.getIndex()] = pChance;
                break;
            case ANIMAL_ARMOR:
                this.bodyArmorDropChance = pChance;
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return this.canPickUpLoot;
    }

    public void setCanPickUpLoot(boolean pCanPickUpLoot) {
        this.canPickUpLoot = pCanPickUpLoot;
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_367943_) {
        return this.canPickUpLoot();
    }

    public boolean isPersistenceRequired() {
        return this.persistenceRequired;
    }

    @Override
    public final InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        if (!this.isAlive()) {
            return InteractionResult.PASS;
        } else {
            InteractionResult interactionresult = this.checkAndHandleImportantInteractions(pPlayer, pHand);
            if (interactionresult.consumesAction()) {
                this.gameEvent(GameEvent.ENTITY_INTERACT, pPlayer);
                return interactionresult;
            } else {
                InteractionResult interactionresult1 = super.interact(pPlayer, pHand);
                if (interactionresult1 != InteractionResult.PASS) {
                    return interactionresult1;
                } else {
                    interactionresult = this.mobInteract(pPlayer, pHand);
                    if (interactionresult.consumesAction()) {
                        this.gameEvent(GameEvent.ENTITY_INTERACT, pPlayer);
                        return interactionresult;
                    } else {
                        return InteractionResult.PASS;
                    }
                }
            }
        }
    }

    private InteractionResult checkAndHandleImportantInteractions(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (itemstack.is(Items.NAME_TAG)) {
            InteractionResult interactionresult = itemstack.interactLivingEntity(pPlayer, this, pHand);
            if (interactionresult.consumesAction()) {
                return interactionresult;
            }
        }

        if (itemstack.getItem() instanceof SpawnEggItem) {
            if (this.level() instanceof ServerLevel) {
                SpawnEggItem spawneggitem = (SpawnEggItem)itemstack.getItem();
                Optional<Mob> optional = spawneggitem.spawnOffspringFromSpawnEgg(
                    pPlayer, this, (EntityType<? extends Mob>)this.getType(), (ServerLevel)this.level(), this.position(), itemstack
                );
                optional.ifPresent(mobIn -> this.onOffspringSpawnedFromEgg(pPlayer, mobIn));
                if (optional.isEmpty()) {
                    return InteractionResult.PASS;
                }
            }

            return InteractionResult.SUCCESS_SERVER;
        } else {
            return InteractionResult.PASS;
        }
    }

    protected void onOffspringSpawnedFromEgg(Player pPlayer, Mob pChild) {
    }

    protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        return InteractionResult.PASS;
    }

    public boolean isWithinRestriction() {
        return this.isWithinRestriction(this.blockPosition());
    }

    public boolean isWithinRestriction(BlockPos pPos) {
        return this.restrictRadius == -1.0F ? true : this.restrictCenter.distSqr(pPos) < (double)(this.restrictRadius * this.restrictRadius);
    }

    public void restrictTo(BlockPos pPos, int pDistance) {
        this.restrictCenter = pPos;
        this.restrictRadius = (float)pDistance;
    }

    public BlockPos getRestrictCenter() {
        return this.restrictCenter;
    }

    public float getRestrictRadius() {
        return this.restrictRadius;
    }

    public void clearRestriction() {
        this.restrictRadius = -1.0F;
    }

    public boolean hasRestriction() {
        return this.restrictRadius != -1.0F;
    }

    @Nullable
    public <T extends Mob> T convertTo(
        EntityType<T> pEntityType, ConversionParams pConversionParams, EntitySpawnReason pSpawnReason, ConversionParams.AfterConversion<T> pAfterConversion
    ) {
        if (this.isRemoved()) {
            return null;
        } else {
            T t = (T)pEntityType.create(this.level(), pSpawnReason);
            if (t == null) {
                return null;
            } else {
                pConversionParams.type().convert(this, t, pConversionParams);
                pAfterConversion.finalizeConversion(t);
                if (this.level() instanceof ServerLevel serverlevel) {
                    serverlevel.addFreshEntity(t);
                }

                if (pConversionParams.type().shouldDiscardAfterConversion()) {
                    this.discard();
                }

                return t;
            }
        }
    }

    @Nullable
    public <T extends Mob> T convertTo(EntityType<T> pEntityType, ConversionParams pCoversionParams, ConversionParams.AfterConversion<T> pAfterConversion) {
        return this.convertTo(pEntityType, pCoversionParams, EntitySpawnReason.CONVERSION, pAfterConversion);
    }

    @Nullable
    @Override
    public Leashable.LeashData getLeashData() {
        return this.leashData;
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData p_344337_) {
        this.leashData = p_344337_;
    }

    @Override
    public void onLeashRemoved() {
        if (this.getLeashData() == null) {
            this.clearRestriction();
        }
    }

    @Override
    public void leashTooFarBehaviour() {
        Leashable.super.leashTooFarBehaviour();
        this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
    }

    @Override
    public boolean canBeLeashed() {
        return !(this instanceof Enemy);
    }

    @Override
    public boolean startRiding(Entity pEntity, boolean pForce) {
        boolean flag = super.startRiding(pEntity, pForce);
        if (flag && this.isLeashed()) {
            this.dropLeash();
        }

        return flag;
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi() && !this.isNoAi();
    }

    public void setNoAi(boolean pNoAi) {
        byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, pNoAi ? (byte)(b0 | 1) : (byte)(b0 & -2));
    }

    public void setLeftHanded(boolean pLeftHanded) {
        byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, pLeftHanded ? (byte)(b0 | 2) : (byte)(b0 & -3));
    }

    public void setAggressive(boolean pAggressive) {
        byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, pAggressive ? (byte)(b0 | 4) : (byte)(b0 & -5));
    }

    public boolean isNoAi() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 1) != 0;
    }

    public boolean isLeftHanded() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 2) != 0;
    }

    public boolean isAggressive() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 4) != 0;
    }

    public void setBaby(boolean pBaby) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    public boolean isWithinMeleeAttackRange(LivingEntity pEntity) {
        return this.getAttackBoundingBox().intersects(pEntity.getHitbox());
    }

    protected AABB getAttackBoundingBox() {
        Entity entity = this.getVehicle();
        AABB aabb;
        if (entity != null) {
            AABB aabb1 = entity.getBoundingBox();
            AABB aabb2 = this.getBoundingBox();
            aabb = new AABB(
                Math.min(aabb2.minX, aabb1.minX),
                aabb2.minY,
                Math.min(aabb2.minZ, aabb1.minZ),
                Math.max(aabb2.maxX, aabb1.maxX),
                aabb2.maxY,
                Math.max(aabb2.maxZ, aabb1.maxZ)
            );
        } else {
            aabb = this.getBoundingBox();
        }

        return aabb.inflate(DEFAULT_ATTACK_REACH, 0.0, DEFAULT_ATTACK_REACH);
    }

    @Override
    public boolean doHurtTarget(ServerLevel p_365421_, Entity p_21372_) {
        float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack itemstack = this.getWeaponItem();
        DamageSource damagesource = Optional.ofNullable(itemstack.getItem().getDamageSource(this)).orElse(this.damageSources().mobAttack(this));
        f = EnchantmentHelper.modifyDamage(p_365421_, itemstack, p_21372_, damagesource, f);
        f += itemstack.getItem().getAttackDamageBonus(p_21372_, f, damagesource);
        boolean flag = p_21372_.hurtServer(p_365421_, damagesource, f);
        if (flag) {
            float f1 = this.getKnockback(p_21372_, damagesource);
            if (f1 > 0.0F && p_21372_ instanceof LivingEntity livingentity) {
                livingentity.knockback(
                    (double)(f1 * 0.5F),
                    (double)Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)),
                    (double)(-Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)))
                );
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }

            if (p_21372_ instanceof LivingEntity livingentity1) {
                itemstack.hurtEnemy(livingentity1, this);
            }

            EnchantmentHelper.doPostAttackEffects(p_365421_, p_21372_, damagesource);
            this.setLastHurtMob(p_21372_);
            this.playAttackSound();
        }

        return flag;
    }

    protected void playAttackSound() {
    }

    protected boolean isSunBurnTick() {
        if (this.level().isDay() && !this.level().isClientSide) {
            float f = this.getLightLevelDependentMagicValue();
            BlockPos blockpos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
            boolean flag = this.isInWaterRainOrBubble() || this.isInPowderSnow || this.wasInPowderSnow;
            if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !flag && this.level().canSeeSky(blockpos)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void jumpInLiquid(TagKey<Fluid> p_204045_) {
        this.jumpInLiquidInternal(() -> super.jumpInLiquid(p_204045_));
    }

    private void jumpInLiquidInternal(Runnable onSuper) {
        if (this.getNavigation().canFloat()) {
            onSuper.run();
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.3, 0.0));
        }
    }

    @Override
    public void jumpInFluid(FluidType type) {
        this.jumpInLiquidInternal(() -> IForgeLivingEntity.super.jumpInFluid(type));
    }

    public final EntitySpawnReason getSpawnReason() {
        return this.spawnReason;
    }

    public final boolean isSpawnCancelled() {
        return this.spawnCancelled;
    }

    public final void setSpawnCancelled(boolean cancel) {
        if (this.isAddedToWorld()) {
            throw new UnsupportedOperationException("Late invocations of Mob#setSpawnCancelled are not permitted.");
        } else {
            this.spawnCancelled = cancel;
        }
    }

    @VisibleForTesting
    public void removeFreeWill() {
        this.removeAllGoals(goalIn -> true);
        this.getBrain().removeAllBehaviors();
    }

    public void removeAllGoals(Predicate<Goal> pFilter) {
        this.goalSelector.removeAllGoals(pFilter);
    }

    @Override
    protected void removeAfterChangingDimensions() {
        super.removeAfterChangingDimensions();
        this.getAllSlots().forEach(itemStackIn -> {
            if (!itemStackIn.isEmpty()) {
                itemStackIn.setCount(0);
            }
        });
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        SpawnEggItem spawneggitem = SpawnEggItem.byId(this.getType());
        return spawneggitem == null ? null : new ItemStack(spawneggitem);
    }

    @Override
    protected void onAttributeUpdated(Holder<Attribute> p_365996_) {
        super.onAttributeUpdated(p_365996_);
        if (p_365996_.is(Attributes.FOLLOW_RANGE) || p_365996_.is(Attributes.TEMPT_RANGE)) {
            this.getNavigation().updatePathfinderMaxVisitedNodes();
        }
    }

    @VisibleForTesting
    public float[] getHandDropChances() {
        return this.handDropChances;
    }

    @VisibleForTesting
    public float[] getArmorDropChances() {
        return this.armorDropChances;
    }

    private boolean canSkipUpdate() {
        if (this.isBaby()) {
            return false;
        } else if (this.hurtTime > 0) {
            return false;
        } else if (this.tickCount < 20) {
            return false;
        } else {
            List list = this.getListPlayers(this.getCommandSenderWorld());
            if (list == null) {
                return false;
            } else if (list.size() != 1) {
                return false;
            } else {
                Entity entity = (Entity)list.get(0);
                double d0 = Math.max(Math.abs(this.getX() - entity.getX()) - 16.0, 0.0);
                double d1 = Math.max(Math.abs(this.getZ() - entity.getZ()) - 16.0, 0.0);
                double d2 = d0 * d0 + d1 * d1;
                return !this.shouldRenderAtSqrDistance(d2);
            }
        }
    }

    private List getListPlayers(Level entityWorld) {
        Level level = this.getCommandSenderWorld();
        if (level instanceof ClientLevel clientlevel) {
            return clientlevel.players();
        } else {
            return level instanceof ServerLevel serverlevel ? serverlevel.players() : null;
        }
    }

    private void onUpdateMinimal() {
        this.noActionTime++;
        if (this instanceof Monster) {
            float f = this.getLightLevelDependentMagicValue();
            boolean flag = this instanceof Raider;
            if (f > 0.5F || flag) {
                this.noActionTime += 2;
            }
        }
    }
}