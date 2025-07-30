package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.EndCrystalModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterEnderCrystal extends ModelAdapterEntity {
    public ModelAdapterEnderCrystal() {
        this("end_crystal");
    }

    protected ModelAdapterEnderCrystal(String name) {
        super(EntityType.END_CRYSTAL, name, ModelLayers.END_CRYSTAL);
    }

    @Override
    public Model makeModel(ModelPart root) {
        return new EndCrystalModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("outer_glass", "outer_glass");
        map.put("inner_glass", "inner_glass");
        map.put("cube", "cube");
        map.put("base", "base");
        map.put("root", "root");
        return map;
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        EntityRenderer entityrenderer = rendererCache.get(EntityType.END_CRYSTAL, index, () -> new EndCrystalRenderer(this.getContext()));
        if (!(entityrenderer instanceof EndCrystalRenderer endcrystalrenderer)) {
            Config.warn("Not an instance of RenderEnderCrystal: " + entityrenderer);
            return null;
        } else if (!(modelBase instanceof EndCrystalModel endcrystalmodel)) {
            Config.warn("Not a EnderCrystalModel model: " + modelBase);
            return null;
        } else if (!Reflector.RenderEnderCrystal_model.exists()) {
            Config.warn("Field not found: RenderEnderCrystal.model");
            return null;
        } else {
            Reflector.RenderEnderCrystal_model.setValue(endcrystalrenderer, modelBase);
            return endcrystalrenderer;
        }
    }
}