package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterZombie extends ModelAdapterAgeableHumanoid {
    public ModelAdapterZombie() {
        super(EntityType.ZOMBIE, "zombie", ModelLayers.ZOMBIE);
    }

    protected ModelAdapterZombie(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterZombie modeladapterzombie = new ModelAdapterZombie(this.getEntityType(), "zombie_baby", ModelLayers.ZOMBIE_BABY);
        modeladapterzombie.setBaby(true);
        modeladapterzombie.setAlias(this.getName());
        return modeladapterzombie;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new ZombieModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new ZombieRenderer(context);
    }
}