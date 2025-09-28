package net.optifine.entity.model;

import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterCow extends ModelAdapterQuadruped {
    public ModelAdapterCow() {
        super(EntityType.COW, "cow", ModelLayers.COW);
    }

    protected ModelAdapterCow(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterCow modeladaptercow = new ModelAdapterCow(this.getEntityType(), "cow_baby", ModelLayers.COW_BABY);
        modeladaptercow.setBaby(true);
        modeladaptercow.setAlias(this.getName());
        return modeladaptercow;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new CowModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new CowRenderer(context);
    }
}