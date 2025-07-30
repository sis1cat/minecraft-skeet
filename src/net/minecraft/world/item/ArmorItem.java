package net.minecraft.world.item;

import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public class ArmorItem extends Item {
    private ArmorMaterial armorMaterial;
    private ArmorType armorType;
    private Item.Properties properties;

    public ArmorItem(ArmorMaterial pMaterial, ArmorType pArmorType, Item.Properties pProperties) {
        super(pMaterial.humanoidProperties(pProperties, pArmorType));
        this.armorMaterial = pMaterial;
        this.armorType = pArmorType;
        this.properties = pProperties;
    }

    public ArmorMaterial getArmorMaterial() {
        return this.armorMaterial;
    }

    public ArmorType getArmorType() {
        return this.armorType;
    }

    public Item.Properties getProperties() {
        return this.properties;
    }
}