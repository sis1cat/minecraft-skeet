package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterHusk extends ModelAdapterZombie {
    public ModelAdapterHusk() {
        super(EntityType.HUSK, "husk", ModelLayers.HUSK);
    }

    protected ModelAdapterHusk(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterHusk modeladapterhusk = new ModelAdapterHusk(this.getEntityType(), "husk_baby", ModelLayers.HUSK_BABY);
        modeladapterhusk.setBaby(true);
        modeladapterhusk.setAlias(this.getName());
        return modeladapterhusk;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new HuskRenderer(context);
    }
}