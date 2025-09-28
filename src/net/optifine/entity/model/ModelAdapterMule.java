package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.DonkeyRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterMule extends ModelAdapterDonkey {
    public ModelAdapterMule() {
        super(EntityType.MULE, "mule", ModelLayers.MULE);
    }

    protected ModelAdapterMule(EntityType type, String name, ModelLayerLocation modelLayer) {
        super(type, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterMule modeladaptermule = new ModelAdapterMule(this.getEntityType(), "mule_baby", ModelLayers.MULE_BABY);
        modeladaptermule.setBaby(true);
        modeladaptermule.setAlias(this.getName());
        return modeladaptermule;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new DonkeyRenderer(context, ModelLayers.MULE, ModelLayers.MULE_BABY, true);
    }
}