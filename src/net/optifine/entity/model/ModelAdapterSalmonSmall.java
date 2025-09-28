package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorField;

public class ModelAdapterSalmonSmall extends ModelAdapterSalmon {
    public ModelAdapterSalmonSmall() {
        super(EntityType.SALMON, "salmon_small", ModelLayers.SALMON_SMALL);
        this.setAlias("salmon");
    }

    @Override
    protected ReflectorField getModelField() {
        return Reflector.SalmonRenderer_modelSmall;
    }
}