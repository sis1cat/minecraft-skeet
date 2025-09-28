package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterMinecartHopper extends ModelAdapterMinecart {
    public ModelAdapterMinecartHopper() {
        super(EntityType.HOPPER_MINECART, "hopper_minecart", ModelLayers.HOPPER_MINECART);
        this.setAlias("minecart");
    }
}