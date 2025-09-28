package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.PandaModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PandaRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPanda extends ModelAdapterQuadruped {
    public ModelAdapterPanda() {
        super(EntityType.PANDA, "panda", ModelLayers.PANDA);
    }

    protected ModelAdapterPanda(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterPanda modeladapterpanda = new ModelAdapterPanda(this.getEntityType(), "panda_baby", ModelLayers.PANDA_BABY);
        modeladapterpanda.setBaby(true);
        modeladapterpanda.setAlias(this.getName());
        return modeladapterpanda;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new PandaModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new PandaRenderer(context);
    }
}