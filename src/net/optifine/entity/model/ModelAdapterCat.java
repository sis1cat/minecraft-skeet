package net.optifine.entity.model;

import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterCat extends ModelAdapterOcelot {
    public ModelAdapterCat() {
        super(EntityType.CAT, "cat", ModelLayers.CAT);
    }

    protected ModelAdapterCat(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterCat modeladaptercat = new ModelAdapterCat(this.getEntityType(), "cat_baby", ModelLayers.CAT_BABY);
        modeladaptercat.setBaby(true);
        modeladaptercat.setAlias(this.getName());
        return modeladaptercat;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new CatModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new CatRenderer(context);
    }
}