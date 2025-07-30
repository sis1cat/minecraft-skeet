package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterMinecartMobSpawner extends ModelAdapterMinecart {
    public ModelAdapterMinecartMobSpawner() {
        super(EntityType.SPAWNER_MINECART, "spawner_minecart", ModelLayers.SPAWNER_MINECART);
        this.setAlias("minecart");
    }
}