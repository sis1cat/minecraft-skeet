package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.ChestBoat;

public enum ChestBoatType {
    OAK("oak", EntityType.OAK_CHEST_BOAT, ModelLayers.OAK_CHEST_BOAT),
    SPRUCE("spruce", EntityType.SPRUCE_CHEST_BOAT, ModelLayers.SPRUCE_CHEST_BOAT),
    BIRCH("birch", EntityType.BIRCH_CHEST_BOAT, ModelLayers.BIRCH_CHEST_BOAT),
    JUNGLE("jungle", EntityType.JUNGLE_CHEST_BOAT, ModelLayers.JUNGLE_CHEST_BOAT),
    ACACIA("acacia", EntityType.ACACIA_CHEST_BOAT, ModelLayers.ACACIA_CHEST_BOAT),
    CHERRY("cherry", EntityType.CHERRY_CHEST_BOAT, ModelLayers.CHERRY_CHEST_BOAT),
    DARK_OAK("dark_oak", EntityType.DARK_OAK_CHEST_BOAT, ModelLayers.DARK_OAK_CHEST_BOAT),
    PALE_OAK("pale_oak", EntityType.PALE_OAK_CHEST_BOAT, ModelLayers.PALE_OAK_CHEST_BOAT),
    MANGROVE("mangrove", EntityType.MANGROVE_CHEST_BOAT, ModelLayers.MANGROVE_CHEST_BOAT);

    private String name;
    private EntityType<ChestBoat> entityType;
    private ModelLayerLocation modelLayer;

    private ChestBoatType(String name, EntityType<ChestBoat> entityType, ModelLayerLocation modelLayer) {
        this.name = name;
        this.entityType = entityType;
        this.modelLayer = modelLayer;
    }

    public String getName() {
        return this.name;
    }

    public EntityType<ChestBoat> getEntityType() {
        return this.entityType;
    }

    public ModelLayerLocation getModelLayer() {
        return this.modelLayer;
    }
}