package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SnowGolemRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterSnowman extends ModelAdapterLiving {
    public ModelAdapterSnowman() {
        super(EntityType.SNOW_GOLEM, "snow_golem", ModelLayers.SNOW_GOLEM);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new SnowGolemModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "upper_body");
        map.put("body_bottom", "lower_body");
        map.put("head", "head");
        map.put("right_hand", "right_arm");
        map.put("left_hand", "left_arm");
        map.put("root", "root");
        return map;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new SnowGolemRenderer(context);
    }
}