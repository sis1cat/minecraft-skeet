package net.optifine;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.optifine.util.CompoundKey;

public class ItemOverrideCache {
    private ItemOverrideProperty[] itemOverrideProperties;
    private Map<CompoundKey, Integer> mapModelIndexes = new HashMap<>();
    public static final Integer INDEX_NONE = new Integer(-1);

    public ItemOverrideCache(ItemOverrideProperty[] itemOverrideProperties) {
        this.itemOverrideProperties = itemOverrideProperties;
    }

    public Integer getModelIndex(ItemStack stack, ClientLevel world, LivingEntity entity) {
        CompoundKey compoundkey = this.getValueKey(stack, world, entity);
        return compoundkey == null ? null : this.mapModelIndexes.get(compoundkey);
    }

    public void putModelIndex(ItemStack stack, ClientLevel world, LivingEntity entity, Integer index) {
        CompoundKey compoundkey = this.getValueKey(stack, world, entity);
        if (compoundkey != null) {
            this.mapModelIndexes.put(compoundkey, index);
        }
    }

    private CompoundKey getValueKey(ItemStack stack, ClientLevel world, LivingEntity entity) {
        Integer[] ainteger = new Integer[this.itemOverrideProperties.length];

        for (int i = 0; i < ainteger.length; i++) {
            Integer integer = this.itemOverrideProperties[i].getValueIndex(stack, world, entity);
            if (integer == null) {
                return null;
            }

            ainteger[i] = integer;
        }

        return new CompoundKey(ainteger);
    }

    @Override
    public String toString() {
        return "properties: " + this.itemOverrideProperties.length + ", modelIndexes: " + this.mapModelIndexes.size();
    }
}
