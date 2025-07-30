package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.CaveSpiderRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterCaveSpider extends ModelAdapterSpider {
    public ModelAdapterCaveSpider() {
        super(EntityType.CAVE_SPIDER, "cave_spider", ModelLayers.CAVE_SPIDER);
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new CaveSpiderRenderer(context);
    }
}