package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.AxolotlRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterAxolotl extends ModelAdapterAgeable {
    public ModelAdapterAxolotl() {
        super(EntityType.AXOLOTL, "axolotl", ModelLayers.AXOLOTL);
    }

    protected ModelAdapterAxolotl(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterAxolotl modeladapteraxolotl = new ModelAdapterAxolotl(this.getEntityType(), "axolotl_baby", ModelLayers.AXOLOTL_BABY);
        modeladapteraxolotl.setBaby(true);
        modeladapteraxolotl.setAlias(this.getName());
        return modeladapteraxolotl;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new AxolotlModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("tail", "tail");
        map.put("leg2", "left_hind_leg");
        map.put("leg1", "right_hind_leg");
        map.put("leg4", "left_front_leg");
        map.put("leg3", "right_front_leg");
        map.put("body", "body");
        map.put("head", "head");
        map.put("top_gills", "top_gills");
        map.put("left_gills", "left_gills");
        map.put("right_gills", "right_gills");
        map.put("root", "root");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new AxolotlRenderer(context);
    }
}