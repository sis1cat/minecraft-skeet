package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.OcelotRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterOcelot extends ModelAdapterAgeable {
    public ModelAdapterOcelot() {
        super(EntityType.OCELOT, "ocelot", ModelLayers.OCELOT);
    }

    protected ModelAdapterOcelot(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterOcelot modeladapterocelot = new ModelAdapterOcelot(this.getEntityType(), "ocelot_baby", ModelLayers.OCELOT_BABY);
        modeladapterocelot.setBaby(true);
        modeladapterocelot.setAlias(this.getName());
        return modeladapterocelot;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new OcelotModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("head", "head");
        map.put("tail", "tail1");
        map.put("tail2", "tail2");
        map.put("back_left_leg", "left_hind_leg");
        map.put("back_right_leg", "right_hind_leg");
        map.put("front_left_leg", "left_front_leg");
        map.put("front_right_leg", "right_front_leg");
        map.put("root", "root");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new OcelotRenderer(context);
    }
}