package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.ArmadilloModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.ArmadilloRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterArmadillo extends ModelAdapterAgeable {
    public ModelAdapterArmadillo() {
        super(EntityType.ARMADILLO, "armadillo", ModelLayers.ARMADILLO);
    }

    protected ModelAdapterArmadillo(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterArmadillo modeladapterarmadillo = new ModelAdapterArmadillo(this.getEntityType(), "armadillo_baby", ModelLayers.ARMADILLO_BABY);
        modeladapterarmadillo.setBaby(true);
        modeladapterarmadillo.setAlias(this.getName());
        return modeladapterarmadillo;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new ArmadilloModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("head", "head");
        map.put("left_ear", "left_ear");
        map.put("right_ear", "right_ear");
        map.put("left_ear_cube", "left_ear_cube");
        map.put("right_ear_cube", "right_ear_cube");
        map.put("back_left_leg", "left_hind_leg");
        map.put("back_right_leg", "right_hind_leg");
        map.put("front_left_leg", "left_front_leg");
        map.put("front_right_leg", "right_front_leg");
        map.put("cube", "cube");
        map.put("tail", "tail");
        map.put("root", "root");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new ArmadilloRenderer(context);
    }
}