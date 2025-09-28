package net.optifine.entity.model;

import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterEvoker extends ModelAdapterIllager {
    public ModelAdapterEvoker() {
        super(EntityType.EVOKER, "evoker", ModelLayers.EVOKER);
        this.setAlias("evocation_illager");
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new IllagerModel(root);
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new EvokerRenderer(context);
    }
}