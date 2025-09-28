package net.optifine.entity.model;

import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MushroomCowRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterMooshroom extends ModelAdapterQuadruped {
    public ModelAdapterMooshroom() {
        this(EntityType.MOOSHROOM, "mooshroom", ModelLayers.MOOSHROOM);
    }

    protected ModelAdapterMooshroom(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterMooshroom modeladaptermooshroom = new ModelAdapterMooshroom(this.getEntityType(), "mooshroom_baby", ModelLayers.MOOSHROOM_BABY);
        modeladaptermooshroom.setBaby(true);
        modeladaptermooshroom.setAlias(this.getName());
        return modeladaptermooshroom;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new CowModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new MushroomCowRenderer(context);
    }
}