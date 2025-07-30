package net.minecraft.world.entity;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public record EquipmentTable(ResourceKey<LootTable> lootTable, Map<EquipmentSlot, Float> slotDropChances) {
    public static final Codec<Map<EquipmentSlot, Float>> DROP_CHANCES_CODEC = Codec.either(Codec.FLOAT, Codec.unboundedMap(EquipmentSlot.CODEC, Codec.FLOAT))
        .xmap(p_330726_ -> p_330726_.map(EquipmentTable::createForAllSlots, Function.identity()), p_329105_ -> {
            boolean flag = p_329105_.values().stream().distinct().count() == 1L;
            boolean flag1 = p_329105_.keySet().containsAll(EquipmentSlot.VALUES);
            return flag && flag1 ? Either.left(p_329105_.values().stream().findFirst().orElse(0.0F)) : Either.right((Map<EquipmentSlot, Float>)p_329105_);
        });
    public static final Codec<EquipmentTable> CODEC = RecordCodecBuilder.create(
        p_328347_ -> p_328347_.group(
                    ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(EquipmentTable::lootTable),
                    DROP_CHANCES_CODEC.optionalFieldOf("slot_drop_chances", Map.of()).forGetter(EquipmentTable::slotDropChances)
                )
                .apply(p_328347_, EquipmentTable::new)
    );

    public EquipmentTable(ResourceKey<LootTable> pLootTable, float pDropChance) {
        this(pLootTable, createForAllSlots(pDropChance));
    }

    private static Map<EquipmentSlot, Float> createForAllSlots(float pDropChance) {
        return createForAllSlots(List.of(EquipmentSlot.values()), pDropChance);
    }

    private static Map<EquipmentSlot, Float> createForAllSlots(List<EquipmentSlot> pEquipmentSlots, float pDropChance) {
        Map<EquipmentSlot, Float> map = Maps.newHashMap();

        for (EquipmentSlot equipmentslot : pEquipmentSlots) {
            map.put(equipmentslot, pDropChance);
        }

        return map;
    }
}