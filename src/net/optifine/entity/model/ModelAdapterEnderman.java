package net.optifine.entity.model;

import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterEnderman extends ModelAdapterHumanoid {
    public ModelAdapterEnderman() {
        super(EntityType.ENDERMAN, "enderman", ModelLayers.ENDERMAN);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new EndermanModel(root);
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new EndermanRenderer(context);
    }
}