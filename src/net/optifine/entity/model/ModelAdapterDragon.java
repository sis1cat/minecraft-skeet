package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.dragon.EnderDragonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.Reflector;

public class ModelAdapterDragon extends ModelAdapterEntity {
    public ModelAdapterDragon() {
        super(EntityType.ENDER_DRAGON, "dragon", ModelLayers.ENDER_DRAGON);
    }

    @Override
    public Model makeModel(ModelPart root) {
        return new EnderDragonModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("head", "head");
        map.put("jaw", "jaw");
        map.put("body", "body");

        for (int i = 0; i < 5; i++) {
            map.put("neck" + (i + 1), "neck" + i);
        }

        for (int j = 0; j < 12; j++) {
            map.put("tail" + (j + 1), "tail" + j);
        }

        map.put("left_wing", "left_wing");
        map.put("left_wing_tip", "left_wing_tip");
        map.put("front_left_leg", "left_front_leg");
        map.put("front_left_shin", "left_front_leg_tip");
        map.put("front_left_foot", "left_front_foot");
        map.put("back_left_leg", "left_hind_leg");
        map.put("back_left_shin", "left_hind_leg_tip");
        map.put("back_left_foot", "left_hind_foot");
        map.put("right_wing", "right_wing");
        map.put("right_wing_tip", "right_wing_tip");
        map.put("front_right_leg", "right_front_leg");
        map.put("front_right_shin", "right_front_leg_tip");
        map.put("front_right_foot", "right_front_foot");
        map.put("back_right_leg", "right_hind_leg");
        map.put("back_right_shin", "right_hind_leg_tip");
        map.put("back_right_foot", "right_hind_foot");
        map.put("root", "root");
        return map;
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        EnderDragonRenderer enderdragonrenderer = new EnderDragonRenderer(this.getContext());
        this.setModel(enderdragonrenderer, Reflector.EnderDragonRenderer_model, "EnderDragonRenderer.model", modelBase);
        return enderdragonrenderer;
    }
}