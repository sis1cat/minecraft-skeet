package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterMinecartFurnace extends ModelAdapterMinecart {
    public ModelAdapterMinecartFurnace() {
        super(EntityType.FURNACE_MINECART, "furnace_minecart", ModelLayers.FURNACE_MINECART);
        this.setAlias("minecart");
    }
}