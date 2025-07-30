package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.RabbitModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterRabbit extends ModelAdapterAgeable {
    public ModelAdapterRabbit() {
        super(EntityType.RABBIT, "rabbit", ModelLayers.RABBIT);
    }

    protected ModelAdapterRabbit(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterRabbit modeladapterrabbit = new ModelAdapterRabbit(this.getEntityType(), "rabbit_baby", ModelLayers.RABBIT_BABY);
        modeladapterrabbit.setBaby(true);
        modeladapterrabbit.setAlias(this.getName());
        return modeladapterrabbit;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new RabbitModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("left_foot", "left_hind_foot");
        map.put("right_foot", "right_hind_foot");
        map.put("left_thigh", "left_haunch");
        map.put("right_thigh", "right_haunch");
        map.put("body", "body");
        map.put("left_arm", "left_front_leg");
        map.put("right_arm", "right_front_leg");
        map.put("head", "head");
        map.put("right_ear", "right_ear");
        map.put("left_ear", "left_ear");
        map.put("tail", "tail");
        map.put("nose", "nose");
        map.put("root", "root");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new RabbitRenderer(context);
    }
}