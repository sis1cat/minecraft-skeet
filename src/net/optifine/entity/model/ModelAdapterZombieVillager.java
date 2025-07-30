package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterZombieVillager extends ModelAdapterAgeableHumanoid {
    public ModelAdapterZombieVillager() {
        super(EntityType.ZOMBIE_VILLAGER, "zombie_villager", ModelLayers.ZOMBIE_VILLAGER);
    }

    protected ModelAdapterZombieVillager(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterZombieVillager modeladapterzombievillager = new ModelAdapterZombieVillager(
            this.getEntityType(), "zombie_villager_baby", ModelLayers.ZOMBIE_VILLAGER_BABY
        );
        modeladapterzombievillager.setBaby(true);
        modeladapterzombievillager.setAlias(this.getName());
        return modeladapterzombievillager;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new ZombieVillagerModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new ZombieVillagerRenderer(context);
    }
}