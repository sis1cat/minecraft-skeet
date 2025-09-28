package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterSheep extends ModelAdapterQuadruped {
    public ModelAdapterSheep() {
        super(EntityType.SHEEP, "sheep", ModelLayers.SHEEP);
    }

    protected ModelAdapterSheep(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterSheep modeladaptersheep = new ModelAdapterSheep(this.getEntityType(), "sheep_baby", ModelLayers.SHEEP_BABY);
        modeladaptersheep.setBaby(true);
        modeladaptersheep.setAlias(this.getName());
        return modeladaptersheep;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new SheepModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new SheepRenderer(context);
    }
}