package net.optifine.entity.model;

import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.PillagerRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPillager extends ModelAdapterIllager {
    public ModelAdapterPillager() {
        super(EntityType.PILLAGER, "pillager", ModelLayers.PILLAGER);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new IllagerModel(root);
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new PillagerRenderer(context);
    }
}