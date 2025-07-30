package net.minecraft.util.datafix.fixes;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class TrialSpawnerConfigInRegistryFix extends NamedEntityFix {
    private static final Logger LOGGER = LogUtils.getLogger();

    public TrialSpawnerConfigInRegistryFix(Schema pOutputSchema) {
        super(pOutputSchema, false, "TrialSpawnerConfigInRegistryFix", References.BLOCK_ENTITY, "minecraft:trial_spawner");
    }

    public Dynamic<?> fixTag(Dynamic<Tag> pTag) {
        Optional<Dynamic<Tag>> optional = pTag.get("normal_config").result();
        if (optional.isEmpty()) {
            return pTag;
        } else {
            Optional<Dynamic<Tag>> optional1 = pTag.get("ominous_config").result();
            if (optional1.isEmpty()) {
                return pTag;
            } else {
                ResourceLocation resourcelocation = TrialSpawnerConfigInRegistryFix.VanillaTrialChambers.CONFIGS_TO_KEY
                    .get(Pair.of(optional.get(), optional1.get()));
                return resourcelocation == null
                    ? pTag
                    : pTag.set("normal_config", pTag.createString(resourcelocation.withSuffix("/normal").toString()))
                        .set("ominous_config", pTag.createString(resourcelocation.withSuffix("/ominous").toString()));
            }
        }
    }

    @Override
    protected Typed<?> fix(Typed<?> p_362424_) {
        return p_362424_.update(DSL.remainderFinder(), p_361190_ -> {
            DynamicOps<?> dynamicops = p_361190_.getOps();
            Dynamic<?> dynamic = this.fixTag(p_361190_.convert(NbtOps.INSTANCE));
            return dynamic.convert(dynamicops);
        });
    }

    static final class VanillaTrialChambers {
        public static final Map<Pair<Dynamic<Tag>, Dynamic<Tag>>, ResourceLocation> CONFIGS_TO_KEY = new HashMap<>();

        private VanillaTrialChambers() {
        }

        private static void register(ResourceLocation pName, String pNormal, String pOminous) {
            try {
                CompoundTag compoundtag = parse(pNormal);
                CompoundTag compoundtag1 = parse(pOminous);
                CompoundTag compoundtag2 = compoundtag.copy().merge(compoundtag1);
                CompoundTag compoundtag3 = removeDefaults(compoundtag2.copy());
                Dynamic<Tag> dynamic = asDynamic(compoundtag);
                CONFIGS_TO_KEY.put(Pair.of(dynamic, asDynamic(compoundtag1)), pName);
                CONFIGS_TO_KEY.put(Pair.of(dynamic, asDynamic(compoundtag2)), pName);
                CONFIGS_TO_KEY.put(Pair.of(dynamic, asDynamic(compoundtag3)), pName);
            } catch (RuntimeException runtimeexception) {
                throw new IllegalStateException("Failed to parse NBT for " + pName, runtimeexception);
            }
        }

        private static Dynamic<Tag> asDynamic(CompoundTag pTag) {
            return new Dynamic<>(NbtOps.INSTANCE, pTag);
        }

        private static CompoundTag parse(String pConfig) {
            try {
                return TagParser.parseTag(pConfig);
            } catch (CommandSyntaxException commandsyntaxexception) {
                throw new IllegalArgumentException("Failed to parse Trial Spawner NBT config: " + pConfig, commandsyntaxexception);
            }
        }

        private static CompoundTag removeDefaults(CompoundTag pTag) {
            if (pTag.getInt("spawn_range") == 4) {
                pTag.remove("spawn_range");
            }

            if (pTag.getFloat("total_mobs") == 6.0F) {
                pTag.remove("total_mobs");
            }

            if (pTag.getFloat("simultaneous_mobs") == 2.0F) {
                pTag.remove("simultaneous_mobs");
            }

            if (pTag.getFloat("total_mobs_added_per_player") == 2.0F) {
                pTag.remove("total_mobs_added_per_player");
            }

            if (pTag.getFloat("simultaneous_mobs_added_per_player") == 1.0F) {
                pTag.remove("simultaneous_mobs_added_per_player");
            }

            if (pTag.getInt("ticks_between_spawn") == 40) {
                pTag.remove("ticks_between_spawn");
            }

            return pTag;
        }

        static {
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/breeze"),
                "{simultaneous_mobs: 1.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:breeze\"}}, weight: 1}], ticks_between_spawn: 20, total_mobs: 2.0f, total_mobs_added_per_player: 1.0f}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 2.0f, total_mobs: 4.0f}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/melee/husk"),
                "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:husk\"}}, weight: 1}], ticks_between_spawn: 20}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:husk\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_melee\", slot_drop_chances: 0.0f}}, weight: 1}]}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/melee/spider"),
                "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:spider\"}}, weight: 1}], ticks_between_spawn: 20}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],simultaneous_mobs: 4.0f, total_mobs: 12.0f}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/melee/zombie"),
                "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:zombie\"}}, weight: 1}], ticks_between_spawn: 20}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],spawn_potentials: [{data: {entity: {id: \"minecraft:zombie\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_melee\", slot_drop_chances: 0.0f}}, weight: 1}]}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/ranged/poison_skeleton"),
                "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}}, weight: 1}], ticks_between_spawn: 20}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/ranged/skeleton"),
                "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}}, weight: 1}], ticks_between_spawn: 20}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/ranged/stray"),
                "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}}, weight: 1}], ticks_between_spawn: 20}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/slow_ranged/poison_skeleton"),
                "{simultaneous_mobs: 4.0f, simultaneous_mobs_added_per_player: 2.0f, spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}}, weight: 1}], ticks_between_spawn: 160}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/slow_ranged/skeleton"),
                "{simultaneous_mobs: 4.0f, simultaneous_mobs_added_per_player: 2.0f, spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}}, weight: 1}], ticks_between_spawn: 160}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/slow_ranged/stray"),
                "{simultaneous_mobs: 4.0f, simultaneous_mobs_added_per_player: 2.0f, spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}}, weight: 1}], ticks_between_spawn: 160}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/small_melee/baby_zombie"),
                "{simultaneous_mobs: 2.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {IsBaby: 1b, id: \"minecraft:zombie\"}}, weight: 1}], ticks_between_spawn: 20}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {IsBaby: 1b, id: \"minecraft:zombie\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_melee\", slot_drop_chances: 0.0f}}, weight: 1}]}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/small_melee/cave_spider"),
                "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:cave_spider\"}}, weight: 1}], ticks_between_spawn: 20}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 4.0f, total_mobs: 12.0f}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/small_melee/silverfish"),
                "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:silverfish\"}}, weight: 1}], ticks_between_spawn: 20}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 4.0f, total_mobs: 12.0f}"
            );
            register(
                ResourceLocation.withDefaultNamespace("trial_chamber/small_melee/slime"),
                "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {Size: 1, id: \"minecraft:slime\"}}, weight: 3}, {data: {entity: {Size: 2, id: \"minecraft:slime\"}}, weight: 1}], ticks_between_spawn: 20}",
                "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 4.0f, total_mobs: 12.0f}"
            );
        }
    }
}