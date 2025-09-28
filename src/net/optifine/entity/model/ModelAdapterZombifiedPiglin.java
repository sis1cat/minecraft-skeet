package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.ZombifiedPiglinModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombifiedPiglinRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterZombifiedPiglin extends ModelAdapterPiglin {
    public ModelAdapterZombifiedPiglin() {
        super(EntityType.ZOMBIFIED_PIGLIN, "zombified_piglin", ModelLayers.ZOMBIFIED_PIGLIN);
    }

    protected ModelAdapterZombifiedPiglin(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterZombifiedPiglin modeladapterzombifiedpiglin = new ModelAdapterZombifiedPiglin(
            this.getEntityType(), "zombified_piglin_baby", ModelLayers.ZOMBIFIED_PIGLIN_BABY
        );
        modeladapterzombifiedpiglin.setBaby(true);
        modeladapterzombifiedpiglin.setAlias(this.getName());
        return modeladapterzombifiedpiglin;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new ZombifiedPiglinModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new ZombifiedPiglinRenderer(
            context, ModelLayers.ZOMBIFIED_PIGLIN, ModelLayers.ZOMBIFIED_PIGLIN_BABY, ModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR, ModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR, ModelLayers.ZOMBIFIED_PIGLIN_BABY_INNER_ARMOR, ModelLayers.ZOMBIFIED_PIGLIN_BABY_OUTER_ARMOR
        );
    }
}