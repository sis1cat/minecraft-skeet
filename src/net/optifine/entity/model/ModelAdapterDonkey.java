package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.DonkeyModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.DonkeyRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterDonkey extends ModelAdapterHorse {
    public ModelAdapterDonkey() {
        super(EntityType.DONKEY, "donkey", ModelLayers.DONKEY);
    }

    protected ModelAdapterDonkey(EntityType type, String name, ModelLayerLocation modelLayer) {
        super(type, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterDonkey modeladapterdonkey = new ModelAdapterDonkey(this.getEntityType(), "donkey_baby", ModelLayers.DONKEY_BABY);
        modeladapterdonkey.setBaby(true);
        modeladapterdonkey.setAlias(this.getName());
        return modeladapterdonkey;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new DonkeyModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = super.makeMapParts();
        map.put("left_chest", "left_chest");
        map.put("right_chest", "right_chest");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new DonkeyRenderer(context, ModelLayers.DONKEY, ModelLayers.DONKEY_BABY, false);
    }
}