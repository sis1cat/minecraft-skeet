package net.optifine.entity.model;

import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllusionerRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterIllusioner extends ModelAdapterIllager {
    public ModelAdapterIllusioner() {
        super(EntityType.ILLUSIONER, "illusioner", ModelLayers.ILLUSIONER);
        this.setAlias("illusion_illager");
    }

    @Override
    protected Model makeModel(ModelPart root) {
        IllagerModel illagermodel = new IllagerModel(root);
        illagermodel.getHat().visible = true;
        return illagermodel;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new IllusionerRenderer(context);
    }
}