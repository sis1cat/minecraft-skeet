package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterWitch extends ModelAdapterLiving {
    public ModelAdapterWitch() {
        super(EntityType.WITCH, "witch", ModelLayers.WITCH);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new WitchModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = ModelAdapterVillager.makeStaticMapParts();
        map.put("mole", "mole");
        return map;
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new WitchRenderer(context);
    }
}