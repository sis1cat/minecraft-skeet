package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.TadpoleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.TadpoleRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterTadpole extends ModelAdapterLiving {
    public ModelAdapterTadpole() {
        super(EntityType.TADPOLE, "tadpole", ModelLayers.TADPOLE);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new TadpoleModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("tail", "tail");
        map.put("root", "root");
        return map;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new TadpoleRenderer(context);
    }
}