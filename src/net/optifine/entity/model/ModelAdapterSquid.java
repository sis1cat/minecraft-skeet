package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SquidRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterSquid extends ModelAdapterAgeable {
    public ModelAdapterSquid() {
        super(EntityType.SQUID, "squid", ModelLayers.SQUID);
    }

    protected ModelAdapterSquid(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterSquid modeladaptersquid = new ModelAdapterSquid(this.getEntityType(), "squid_baby", ModelLayers.SQUID_BABY);
        modeladaptersquid.setBaby(true);
        modeladaptersquid.setAlias(this.getName());
        return modeladaptersquid;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new SquidModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");

        for (int i = 0; i < 8; i++) {
            map.put("tentacle" + (i + 1), "tentacle" + i);
        }

        map.put("root", "root");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new SquidRenderer(context, new SquidModel(bakeModelLayer(ModelLayers.SQUID)), new SquidModel(bakeModelLayer(ModelLayers.SQUID_BABY)));
    }
}