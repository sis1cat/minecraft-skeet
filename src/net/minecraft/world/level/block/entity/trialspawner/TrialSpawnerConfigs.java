package net.minecraft.world.level.block.entity.trialspawner;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class TrialSpawnerConfigs {
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_BREEZE = TrialSpawnerConfigs.Keys.of("trial_chamber/breeze");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_MELEE_HUSK = TrialSpawnerConfigs.Keys.of("trial_chamber/melee/husk");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_MELEE_SPIDER = TrialSpawnerConfigs.Keys.of("trial_chamber/melee/spider");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_MELEE_ZOMBIE = TrialSpawnerConfigs.Keys.of("trial_chamber/melee/zombie");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_RANGED_POISON_SKELETON = TrialSpawnerConfigs.Keys.of("trial_chamber/ranged/poison_skeleton");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_RANGED_SKELETON = TrialSpawnerConfigs.Keys.of("trial_chamber/ranged/skeleton");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_RANGED_STRAY = TrialSpawnerConfigs.Keys.of("trial_chamber/ranged/stray");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SLOW_RANGED_POISON_SKELETON = TrialSpawnerConfigs.Keys.of("trial_chamber/slow_ranged/poison_skeleton");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SLOW_RANGED_SKELETON = TrialSpawnerConfigs.Keys.of("trial_chamber/slow_ranged/skeleton");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SLOW_RANGED_STRAY = TrialSpawnerConfigs.Keys.of("trial_chamber/slow_ranged/stray");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SMALL_MELEE_BABY_ZOMBIE = TrialSpawnerConfigs.Keys.of("trial_chamber/small_melee/baby_zombie");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SMALL_MELEE_CAVE_SPIDER = TrialSpawnerConfigs.Keys.of("trial_chamber/small_melee/cave_spider");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SMALL_MELEE_SILVERFISH = TrialSpawnerConfigs.Keys.of("trial_chamber/small_melee/silverfish");
    private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SMALL_MELEE_SLIME = TrialSpawnerConfigs.Keys.of("trial_chamber/small_melee/slime");

    public static void bootstrap(BootstrapContext<TrialSpawnerConfig> pContext) {
        register(
            pContext,
            TRIAL_CHAMBER_BREEZE,
            TrialSpawnerConfig.builder()
                .simultaneousMobs(1.0F)
                .simultaneousMobsAddedPerPlayer(0.5F)
                .ticksBetweenSpawn(20)
                .totalMobs(2.0F)
                .totalMobsAddedPerPlayer(1.0F)
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.BREEZE)))
                .build(),
            TrialSpawnerConfig.builder()
                .simultaneousMobsAddedPerPlayer(0.5F)
                .ticksBetweenSpawn(20)
                .totalMobs(4.0F)
                .totalMobsAddedPerPlayer(1.0F)
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.BREEZE)))
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_MELEE_HUSK,
            trialChamberBase().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.HUSK))).build(),
            trialChamberBase()
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnDataWithEquipment(EntityType.HUSK, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE)))
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_MELEE_SPIDER,
            trialChamberBase().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.SPIDER))).build(),
            trialChamberMeleeOminous()
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.SPIDER)))
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_MELEE_ZOMBIE,
            trialChamberBase().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.ZOMBIE))).build(),
            trialChamberBase()
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnDataWithEquipment(EntityType.ZOMBIE, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE)))
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_RANGED_POISON_SKELETON,
            trialChamberBase().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.BOGGED))).build(),
            trialChamberBase()
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnDataWithEquipment(EntityType.BOGGED, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_RANGED_SKELETON,
            trialChamberBase().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.SKELETON))).build(),
            trialChamberBase()
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnDataWithEquipment(EntityType.SKELETON, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_RANGED_STRAY,
            trialChamberBase().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.STRAY))).build(),
            trialChamberBase()
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnDataWithEquipment(EntityType.STRAY, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_SLOW_RANGED_POISON_SKELETON,
            trialChamberSlowRanged().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.BOGGED))).build(),
            trialChamberSlowRanged()
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnDataWithEquipment(EntityType.BOGGED, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_SLOW_RANGED_SKELETON,
            trialChamberSlowRanged().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.SKELETON))).build(),
            trialChamberSlowRanged()
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnDataWithEquipment(EntityType.SKELETON, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_SLOW_RANGED_STRAY,
            trialChamberSlowRanged().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.STRAY))).build(),
            trialChamberSlowRanged()
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnDataWithEquipment(EntityType.STRAY, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_SMALL_MELEE_BABY_ZOMBIE,
            TrialSpawnerConfig.builder()
                .simultaneousMobsAddedPerPlayer(0.5F)
                .ticksBetweenSpawn(20)
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(customSpawnDataWithEquipment(EntityType.ZOMBIE, p_368102_ -> p_368102_.putBoolean("IsBaby", true), null)))
                .build(),
            TrialSpawnerConfig.builder()
                .simultaneousMobsAddedPerPlayer(0.5F)
                .ticksBetweenSpawn(20)
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(
                    SimpleWeightedRandomList.single(
                        customSpawnDataWithEquipment(EntityType.ZOMBIE, p_361540_ -> p_361540_.putBoolean("IsBaby", true), BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE)
                    )
                )
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_SMALL_MELEE_CAVE_SPIDER,
            trialChamberBase().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.CAVE_SPIDER))).build(),
            trialChamberMeleeOminous()
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.CAVE_SPIDER)))
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_SMALL_MELEE_SILVERFISH,
            trialChamberBase().spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.SILVERFISH))).build(),
            trialChamberMeleeOminous()
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(SimpleWeightedRandomList.single(spawnData(EntityType.SILVERFISH)))
                .build()
        );
        register(
            pContext,
            TRIAL_CHAMBER_SMALL_MELEE_SLIME,
            trialChamberBase()
                .spawnPotentialsDefinition(
                    SimpleWeightedRandomList.<SpawnData>builder()
                        .add(customSpawnData(EntityType.SLIME, p_360912_ -> p_360912_.putByte("Size", (byte)1)), 3)
                        .add(customSpawnData(EntityType.SLIME, p_362344_ -> p_362344_.putByte("Size", (byte)2)), 1)
                        .build()
                )
                .build(),
            trialChamberMeleeOminous()
                .lootTablesToEject(
                    SimpleWeightedRandomList.<ResourceKey<LootTable>>builder()
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                        .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                        .build()
                )
                .spawnPotentialsDefinition(
                    SimpleWeightedRandomList.<SpawnData>builder()
                        .add(customSpawnData(EntityType.SLIME, p_363382_ -> p_363382_.putByte("Size", (byte)1)), 3)
                        .add(customSpawnData(EntityType.SLIME, p_367157_ -> p_367157_.putByte("Size", (byte)2)), 1)
                        .build()
                )
                .build()
        );
    }

    private static <T extends Entity> SpawnData spawnData(EntityType<T> pEntityType) {
        return customSpawnDataWithEquipment(pEntityType, p_368946_ -> {
        }, null);
    }

    private static <T extends Entity> SpawnData customSpawnData(EntityType<T> pEntityType, Consumer<CompoundTag> pTagConsumer) {
        return customSpawnDataWithEquipment(pEntityType, pTagConsumer, null);
    }

    private static <T extends Entity> SpawnData spawnDataWithEquipment(EntityType<T> pEntityType, ResourceKey<LootTable> pLootTableKey) {
        return customSpawnDataWithEquipment(pEntityType, p_364921_ -> {
        }, pLootTableKey);
    }

    private static <T extends Entity> SpawnData customSpawnDataWithEquipment(EntityType<T> pEntityType, Consumer<CompoundTag> pTagConsumer, @Nullable ResourceKey<LootTable> pLootTableKey) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(pEntityType).toString());
        pTagConsumer.accept(compoundtag);
        Optional<EquipmentTable> optional = Optional.ofNullable(pLootTableKey).map(p_367063_ -> new EquipmentTable((ResourceKey<LootTable>)p_367063_, 0.0F));
        return new SpawnData(compoundtag, Optional.empty(), optional);
    }

    private static void register(
        BootstrapContext<TrialSpawnerConfig> pContext, TrialSpawnerConfigs.Keys pKeys, TrialSpawnerConfig pNormal, TrialSpawnerConfig pOminous
    ) {
        pContext.register(pKeys.normal, pNormal);
        pContext.register(pKeys.ominous, pOminous);
    }

    static ResourceKey<TrialSpawnerConfig> registryKey(String pName) {
        return ResourceKey.create(Registries.TRIAL_SPAWNER_CONFIG, ResourceLocation.withDefaultNamespace(pName));
    }

    private static TrialSpawnerConfig.Builder trialChamberMeleeOminous() {
        return TrialSpawnerConfig.builder().simultaneousMobs(4.0F).simultaneousMobsAddedPerPlayer(0.5F).ticksBetweenSpawn(20).totalMobs(12.0F);
    }

    private static TrialSpawnerConfig.Builder trialChamberSlowRanged() {
        return TrialSpawnerConfig.builder().simultaneousMobs(4.0F).simultaneousMobsAddedPerPlayer(2.0F).ticksBetweenSpawn(160);
    }

    private static TrialSpawnerConfig.Builder trialChamberBase() {
        return TrialSpawnerConfig.builder().simultaneousMobs(3.0F).simultaneousMobsAddedPerPlayer(0.5F).ticksBetweenSpawn(20);
    }

    static record Keys(ResourceKey<TrialSpawnerConfig> normal, ResourceKey<TrialSpawnerConfig> ominous) {
        public static TrialSpawnerConfigs.Keys of(String pKey) {
            return new TrialSpawnerConfigs.Keys(TrialSpawnerConfigs.registryKey(pKey + "/normal"), TrialSpawnerConfigs.registryKey(pKey + "/ominous"));
        }
    }
}