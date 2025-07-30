package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.GhastModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GhastRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterGhast extends ModelAdapterLiving {
    public ModelAdapterGhast() {
        super(EntityType.GHAST, "ghast", ModelLayers.GHAST);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new GhastModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");

        for (int i = 0; i < 9; i++) {
            map.put("tentacle" + (i + 1), "tentacle" + i);
        }

        map.put("root", "root");
        return map;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new GhastRenderer(context);
    }
}