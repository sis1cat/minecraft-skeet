package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPig extends ModelAdapterQuadruped {
    public ModelAdapterPig() {
        super(EntityType.PIG, "pig", ModelLayers.PIG);
    }

    protected ModelAdapterPig(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterPig modeladapterpig = new ModelAdapterPig(this.getEntityType(), "pig_baby", ModelLayers.PIG_BABY);
        modeladapterpig.setBaby(true);
        modeladapterpig.setAlias(this.getName());
        return modeladapterpig;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new PigModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new PigRenderer(context);
    }
}