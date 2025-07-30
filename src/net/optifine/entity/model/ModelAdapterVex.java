package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.VexModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterVex extends ModelAdapterLiving {
    public ModelAdapterVex() {
        super(EntityType.VEX, "vex", ModelLayers.VEX);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new VexModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("head", "head");
        map.put("right_arm", "right_arm");
        map.put("left_arm", "left_arm");
        map.put("right_wing", "right_wing");
        map.put("left_wing", "left_wing");
        map.put("root", "root");
        return map;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new VexRenderer(context);
    }
}