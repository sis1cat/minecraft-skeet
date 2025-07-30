package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.CamelRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterCamel extends ModelAdapterAgeable {
    public ModelAdapterCamel() {
        super(EntityType.CAMEL, "camel", ModelLayers.CAMEL);
    }

    protected ModelAdapterCamel(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterCamel modeladaptercamel = new ModelAdapterCamel(this.getEntityType(), "camel_baby", ModelLayers.CAMEL_BABY);
        modeladaptercamel.setBaby(true);
        modeladaptercamel.setAlias(this.getName());
        return modeladaptercamel;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new CamelModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("hump", "hump");
        map.put("tail", "tail");
        map.put("head", "head");
        map.put("left_ear", "left_ear");
        map.put("right_ear", "right_ear");
        map.put("back_left_leg", "left_hind_leg");
        map.put("back_right_leg", "right_hind_leg");
        map.put("front_left_leg", "left_front_leg");
        map.put("front_right_leg", "right_front_leg");
        map.put("saddle", "saddle");
        map.put("reins", "reins");
        map.put("bridle", "bridle");
        map.put("root", "root");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new CamelRenderer(context);
    }
}