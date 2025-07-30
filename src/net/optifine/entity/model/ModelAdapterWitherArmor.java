package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterWitherArmor extends ModelAdapterWither {
    public ModelAdapterWitherArmor() {
        super(EntityType.WITHER, "wither_armor", ModelLayers.WITHER_ARMOR);
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase) {
        WitherBossRenderer witherbossrenderer = (WitherBossRenderer)renderer;
        WitherArmorLayer witherarmorlayer = new WitherArmorLayer(witherbossrenderer, this.getContext().getModelSet());
        witherarmorlayer.model = (WitherBossModel)modelBase;
        renderer.replaceLayer(WitherArmorLayer.class, witherarmorlayer);
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation) {
        WitherBossRenderer witherbossrenderer = (WitherBossRenderer)er;

        for (WitherArmorLayer witherarmorlayer : witherbossrenderer.getLayers(WitherArmorLayer.class)) {
            witherarmorlayer.customTextureLocation = textureLocation;
        }

        return true;
    }
}