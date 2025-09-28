package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterBlaze extends ModelAdapterLiving {
    public ModelAdapterBlaze() {
        super(EntityType.BLAZE, "blaze", ModelLayers.BLAZE);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new BlazeModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("head", "head");

        for (int i = 0; i < 12; i++) {
            map.put("stick" + (i + 1), "part" + i);
        }

        map.put("root", "root");
        return map;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new BlazeRenderer(context);
    }
}