package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HoglinRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterHoglin extends ModelAdapterAgeable {
    public ModelAdapterHoglin() {
        this(EntityType.HOGLIN, "hoglin", ModelLayers.HOGLIN);
    }

    protected ModelAdapterHoglin(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterAgeable modeladapterageable = new ModelAdapterHoglin(this.getEntityType(), "hoglin_baby", ModelLayers.HOGLIN_BABY);
        modeladapterageable.setBaby(true);
        modeladapterageable.setAlias(this.getName());
        return modeladapterageable;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new HoglinModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("mane", "mane");
        map.put("head", "head");
        map.put("right_ear", "right_ear");
        map.put("left_ear", "left_ear");
        map.put("front_right_leg", "right_front_leg");
        map.put("front_left_leg", "left_front_leg");
        map.put("back_right_leg", "right_hind_leg");
        map.put("back_left_leg", "left_hind_leg");
        map.put("root", "root");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new HoglinRenderer(context);
    }
}