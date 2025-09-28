package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPiglin extends ModelAdapterAgeablePlayer {
    public ModelAdapterPiglin() {
        super(EntityType.PIGLIN, "piglin", ModelLayers.PIGLIN);
    }

    protected ModelAdapterPiglin(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterPiglin modeladapterpiglin = new ModelAdapterPiglin(this.getEntityType(), "piglin_baby", ModelLayers.PIGLIN_BABY);
        modeladapterpiglin.setBaby(true);
        modeladapterpiglin.setAlias(this.getName());
        return modeladapterpiglin;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new PiglinModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = super.makeMapParts();
        map.put("left_ear", "left_ear");
        map.put("right_ear", "right_ear");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new PiglinRenderer(
            context, ModelLayers.PIGLIN, ModelLayers.PIGLIN_BABY, ModelLayers.PIGLIN_INNER_ARMOR, ModelLayers.PIGLIN_OUTER_ARMOR, ModelLayers.PIGLIN_BABY_INNER_ARMOR, ModelLayers.PIGLIN_BABY_OUTER_ARMOR
        );
    }
}