package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;

public enum BoatType {
    OAK("oak", EntityType.OAK_BOAT, ModelLayers.OAK_BOAT),
    SPRUCE("spruce", EntityType.SPRUCE_BOAT, ModelLayers.SPRUCE_BOAT),
    BIRCH("birch", EntityType.BIRCH_BOAT, ModelLayers.BIRCH_BOAT),
    JUNGLE("jungle", EntityType.JUNGLE_BOAT, ModelLayers.JUNGLE_BOAT),
    ACACIA("acacia", EntityType.ACACIA_BOAT, ModelLayers.ACACIA_BOAT),
    CHERRY("cherry", EntityType.CHERRY_BOAT, ModelLayers.CHERRY_BOAT),
    DARK_OAK("dark_oak", EntityType.DARK_OAK_BOAT, ModelLayers.DARK_OAK_BOAT),
    PALE_OAK("pale_oak", EntityType.PALE_OAK_BOAT, ModelLayers.PALE_OAK_BOAT),
    MANGROVE("mangrove", EntityType.MANGROVE_BOAT, ModelLayers.MANGROVE_BOAT);

    private String name;
    private EntityType<Boat> entityType;
    private ModelLayerLocation modelLayer;

    private BoatType(String name, EntityType<Boat> entityType, ModelLayerLocation modelLayer) {
        this.name = name;
        this.entityType = entityType;
        this.modelLayer = modelLayer;
    }

    public String getName() {
        return this.name;
    }

    public EntityType<Boat> getEntityType() {
        return this.entityType;
    }

    public ModelLayerLocation getModelLayer() {
        return this.modelLayer;
    }
}