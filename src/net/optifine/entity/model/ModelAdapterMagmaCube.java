package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MagmaCubeRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterMagmaCube extends ModelAdapterLiving {
    public ModelAdapterMagmaCube() {
        super(EntityType.MAGMA_CUBE, "magma_cube", ModelLayers.MAGMA_CUBE);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new LavaSlimeModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("core", "inside_cube");

        for (int i = 0; i < 8; i++) {
            map.put("segment" + (i + 1), "cube" + i);
        }

        map.put("root", "root");
        return map;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new MagmaCubeRenderer(context);
    }
}