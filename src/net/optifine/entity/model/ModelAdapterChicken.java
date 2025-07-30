package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterChicken extends ModelAdapterAgeable {
    public ModelAdapterChicken() {
        super(EntityType.CHICKEN, "chicken", ModelLayers.CHICKEN);
    }

    protected ModelAdapterChicken(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterChicken modeladapterchicken = new ModelAdapterChicken(this.getEntityType(), "chicken_baby", ModelLayers.CHICKEN_BABY);
        modeladapterchicken.setBaby(true);
        modeladapterchicken.setAlias(this.getName());
        return modeladapterchicken;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new ChickenModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("head", "head");
        map.put("body", "body");
        map.put("right_leg", "right_leg");
        map.put("left_leg", "left_leg");
        map.put("right_wing", "right_wing");
        map.put("left_wing", "left_wing");
        map.put("bill", "beak");
        map.put("chin", "red_thing");
        map.put("root", "root");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new ChickenRenderer(context);
    }
}