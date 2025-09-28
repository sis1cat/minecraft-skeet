package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterVillager extends ModelAdapterAgeable {
    public ModelAdapterVillager() {
        super(EntityType.VILLAGER, "villager", ModelLayers.VILLAGER);
    }

    protected ModelAdapterVillager(EntityType type, String name, ModelLayerLocation modelLayer) {
        super(type, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterVillager modeladaptervillager = new ModelAdapterVillager(this.getEntityType(), "villager_baby", ModelLayers.VILLAGER_BABY);
        modeladaptervillager.setBaby(true);
        modeladaptervillager.setAlias(this.getName());
        return modeladaptervillager;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new VillagerModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        return makeStaticMapParts();
    }

    public static Map<String, String> makeStaticMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("head", "head");
        map.put("headwear", "hat");
        map.put("headwear2", "hat_rim");
        map.put("body", "body");
        map.put("bodywear", "jacket");
        map.put("arms", "arms");
        map.put("right_leg", "right_leg");
        map.put("left_leg", "left_leg");
        map.put("nose", "nose");
        map.put("root", "root");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new VillagerRenderer(context);
    }
}