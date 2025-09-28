package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterMinecartChest extends ModelAdapterMinecart {
    public ModelAdapterMinecartChest() {
        super(EntityType.CHEST_MINECART, "chest_minecart", ModelLayers.CHEST_MINECART);
        this.setAlias("minecart");
    }
}