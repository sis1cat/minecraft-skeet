package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPiglinBrute extends ModelAdapterPiglin {
    public ModelAdapterPiglinBrute() {
        super(EntityType.PIGLIN_BRUTE, "piglin_brute", ModelLayers.PIGLIN_BRUTE);
    }

    @Override
    public ModelAdapter makeBaby() {
        return null;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new PiglinRenderer(
            context, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_INNER_ARMOR, ModelLayers.PIGLIN_BRUTE_OUTER_ARMOR, ModelLayers.PIGLIN_BRUTE_INNER_ARMOR, ModelLayers.PIGLIN_BRUTE_OUTER_ARMOR
        );
    }
}