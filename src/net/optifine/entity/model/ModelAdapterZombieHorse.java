package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.UndeadHorseRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterZombieHorse extends ModelAdapterHorse {
    public ModelAdapterZombieHorse() {
        this(EntityType.ZOMBIE_HORSE, "zombie_horse", ModelLayers.ZOMBIE_HORSE);
    }

    protected ModelAdapterZombieHorse(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterZombieHorse modeladapterzombiehorse = new ModelAdapterZombieHorse(this.getEntityType(), "zombie_horse_baby", ModelLayers.ZOMBIE_HORSE_BABY);
        modeladapterzombiehorse.setBaby(true);
        modeladapterzombiehorse.setAlias(this.getName());
        return modeladapterzombiehorse;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new UndeadHorseRenderer(context, ModelLayers.ZOMBIE_HORSE, ModelLayers.ZOMBIE_HORSE_BABY, false);
    }
}