package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterGuardian extends ModelAdapterLiving {
    public ModelAdapterGuardian() {
        super(EntityType.GUARDIAN, "guardian", ModelLayers.GUARDIAN);
    }

    public ModelAdapterGuardian(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new GuardianModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "head");
        map.put("eye", "eye");

        for (int i = 0; i < 12; i++) {
            map.put("spine" + (i + 1), "spike" + i);
        }

        for (int j = 0; j < 3; j++) {
            map.put("tail" + (j + 1), "tail" + j);
        }

        map.put("root", "root");
        return map;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new GuardianRenderer(context);
    }
}