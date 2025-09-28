package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterSlimeOuter extends ModelAdapterSlime {
    public ModelAdapterSlimeOuter() {
        super(EntityType.SLIME, "slime_outer", ModelLayers.SLIME_OUTER);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "cube");
        map.put("root", "root");
        return map;
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase) {
        SlimeRenderer slimerenderer = (SlimeRenderer)renderer;
        SlimeOuterLayer slimeouterlayer = new SlimeOuterLayer(slimerenderer, this.getContext().getModelSet());
        slimeouterlayer.model = (SlimeModel)modelBase;
        slimerenderer.replaceLayer(SlimeOuterLayer.class, slimeouterlayer);
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation) {
        SlimeRenderer slimerenderer = (SlimeRenderer)er;

        for (SlimeOuterLayer slimeouterlayer : slimerenderer.getLayers(SlimeOuterLayer.class)) {
            slimeouterlayer.customTextureLocation = textureLocation;
        }

        return true;
    }
}