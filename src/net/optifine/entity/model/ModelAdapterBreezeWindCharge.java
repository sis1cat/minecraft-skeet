package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterBreezeWindCharge extends ModelAdapterWindCharge {
    public ModelAdapterBreezeWindCharge() {
        super(EntityType.BREEZE_WIND_CHARGE, "breeze_wind_charge", ModelLayers.WIND_CHARGE);
        this.setAlias("wind_charge");
    }
}