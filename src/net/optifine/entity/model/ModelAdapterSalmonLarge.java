package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorField;

public class ModelAdapterSalmonLarge extends ModelAdapterSalmon {
    public ModelAdapterSalmonLarge() {
        super(EntityType.SALMON, "salmon_large", ModelLayers.SALMON_LARGE);
        this.setAlias("salmon");
    }

    @Override
    protected ReflectorField getModelField() {
        return Reflector.SalmonRenderer_modelLarge;
    }
}