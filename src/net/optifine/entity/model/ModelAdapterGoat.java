package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.GoatModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterGoat extends ModelAdapterQuadruped {
    public ModelAdapterGoat() {
        super(EntityType.GOAT, "goat", ModelLayers.GOAT);
    }

    protected ModelAdapterGoat(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterGoat modeladaptergoat = new ModelAdapterGoat(this.getEntityType(), "goat_baby", ModelLayers.GOAT_BABY);
        modeladaptergoat.setBaby(true);
        modeladaptergoat.setAlias(this.getName());
        return modeladaptergoat;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new GoatModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = super.makeMapParts();
        map.put("left_horn", "left_horn");
        map.put("right_horn", "right_horn");
        map.put("nose", "nose");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new GoatRenderer(context);
    }
}