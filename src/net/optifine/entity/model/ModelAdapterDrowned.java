package net.optifine.entity.model;

import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterDrowned extends ModelAdapterZombie {
    public ModelAdapterDrowned() {
        super(EntityType.DROWNED, "drowned", ModelLayers.DROWNED);
    }

    public ModelAdapterDrowned(EntityType type, String name, ModelLayerLocation modelLayer) {
        super(type, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterDrowned modeladapterdrowned = new ModelAdapterDrowned(this.getEntityType(), "drowned_baby", ModelLayers.DROWNED_BABY);
        modeladapterdrowned.setBaby(true);
        modeladapterdrowned.setAlias(this.getName());
        return modeladapterdrowned;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new DrownedModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new DrownedRenderer(context);
    }
}