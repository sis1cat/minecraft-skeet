package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterSilverfish extends ModelAdapterLiving {
    public ModelAdapterSilverfish() {
        super(EntityType.SILVERFISH, "silverfish", ModelLayers.SILVERFISH);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new SilverfishModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();

        for (int i = 0; i < 7; i++) {
            map.put("body" + (i + 1), "segment" + i);
        }

        for (int j = 0; j < 3; j++) {
            map.put("wing" + (j + 1), "layer" + j);
        }

        map.put("root", "root");
        return map;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new SilverfishRenderer(context);
    }
}