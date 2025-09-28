package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterWolf extends ModelAdapterAgeable {
    public ModelAdapterWolf() {
        this(EntityType.WOLF, "wolf", ModelLayers.WOLF);
    }

    protected ModelAdapterWolf(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterAgeable modeladapterageable = new ModelAdapterWolf(EntityType.WOLF, "wolf_baby", ModelLayers.WOLF_BABY);
        modeladapterageable.setBaby(true);
        modeladapterageable.setAlias("wolf");
        return modeladapterageable;
    }

    @Override
    public Model makeModel(ModelPart root) {
        return new WolfModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("head", "head");
        map.put("body", "body");
        map.put("leg1", "right_hind_leg");
        map.put("leg2", "left_hind_leg");
        map.put("leg3", "right_front_leg");
        map.put("leg4", "left_front_leg");
        map.put("tail", "tail");
        map.put("mane", "upper_body");
        map.put("root", "root");
        return map;
    }

    @Override
    public AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new WolfRenderer(context);
    }
}