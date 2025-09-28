package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.CodModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.CodRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterCod extends ModelAdapterLiving {
    public ModelAdapterCod() {
        super(EntityType.COD, "cod", ModelLayers.COD);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new CodModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("fin_back", "top_fin");
        map.put("head", "head");
        map.put("nose", "nose");
        map.put("fin_right", "right_fin");
        map.put("fin_left", "left_fin");
        map.put("tail", "tail_fin");
        map.put("root", "root");
        return map;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new CodRenderer(context);
    }
}