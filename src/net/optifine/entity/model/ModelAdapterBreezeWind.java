package net.optifine.entity.model;

import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterBreezeWind extends ModelAdapterBreeze {
    public ModelAdapterBreezeWind() {
        super(EntityType.BREEZE, "breeze_wind", ModelLayers.BREEZE_WIND);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new BreezeModel(root);
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase) {
        BreezeRenderer breezerenderer = (BreezeRenderer)renderer;
        ResourceLocation resourcelocation = modelBase.locationTextureCustom != null
            ? modelBase.locationTextureCustom
            : new ResourceLocation("textures/entity/breeze/breeze_wind.png");
        BreezeWindLayer breezewindlayer = new BreezeWindLayer(this.getContext(), breezerenderer);
        breezewindlayer.setModel((BreezeModel)modelBase);
        breezewindlayer.setTextureLocation(resourcelocation);
        renderer.replaceLayer(BreezeWindLayer.class, breezewindlayer);
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation) {
        return true;
    }
}