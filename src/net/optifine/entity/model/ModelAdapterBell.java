package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.BellModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.reflect.Reflector;

public class ModelAdapterBell extends ModelAdapterBlockEntity {
    private static Map<String, String> mapParts = makeMapParts();

    public ModelAdapterBell() {
        super(BlockEntityType.BELL, "bell");
    }

    @Override
    public Model makeModel() {
        return new BellModel(bakeModelLayer(ModelLayers.BELL));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart) {
        return this.getModelRenderer(model.root(), mapParts.get(modelPart));
    }

    @Override
    public String[] getModelRendererNames() {
        return toArray(mapParts.keySet());
    }

    private static Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "bell_body");
        map.put("base", "bell_base");
        map.put("root", "root");
        return map;
    }

    @Override
    public IEntityRenderer makeEntityRender(Model model, RendererCache rendererCache, int index) {
        BlockEntityRenderer blockentityrenderer = rendererCache.get(BlockEntityType.BELL, index, () -> new BellRenderer(this.getContext()));
        BellModel bellmodel = (BellModel)model;
        Reflector.setFieldValue(blockentityrenderer, Reflector.BellRenderer_model, bellmodel);
        return blockentityrenderer;
    }
}