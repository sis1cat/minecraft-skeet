package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.DolphinModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.DolphinRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterDolphin extends ModelAdapterAgeable {
    public ModelAdapterDolphin() {
        super(EntityType.DOLPHIN, "dolphin", ModelLayers.DOLPHIN);
    }

    protected ModelAdapterDolphin(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterDolphin modeladapterdolphin = new ModelAdapterDolphin(this.getEntityType(), "dolphin_baby", ModelLayers.DOLPHIN_BABY);
        modeladapterdolphin.setBaby(true);
        modeladapterdolphin.setAlias(this.getName());
        return modeladapterdolphin;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new DolphinModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("back_fin", "back_fin");
        map.put("left_fin", "left_fin");
        map.put("right_fin", "right_fin");
        map.put("tail", "tail");
        map.put("tail_fin", "tail_fin");
        map.put("head", "head");
        map.put("root", "root");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new DolphinRenderer(context);
    }
}