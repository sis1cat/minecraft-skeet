package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.Config;

public class ModelAdapterHead extends ModelAdapterBlockEntity {
    private ModelLayerLocation modelLayer;
    private SkullBlock.Types skullBlockType;
    private static Map<String, String> mapParts = makeMapParts();

    public ModelAdapterHead(String name, ModelLayerLocation modelLayer, SkullBlock.Types skullBlockType) {
        super(BlockEntityType.SKULL, name);
        this.modelLayer = modelLayer;
        this.skullBlockType = skullBlockType;
    }

    @Override
    public Model makeModel() {
        return new SkullModel(bakeModelLayer(this.modelLayer));
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
        map.put("head", "head");
        map.put("root", "root");
        return map;
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        BlockEntityRenderer blockentityrenderer = rendererCache.get(BlockEntityType.SKULL, index, () -> new SkullBlockRenderer(this.getContext()));
        if (!(blockentityrenderer instanceof SkullBlockRenderer)) {
            return null;
        } else {
            Map<SkullBlock.Type, SkullModelBase> map = RendererUtils.getSkullModelMap();
            if (map == null) {
                Config.warn("Field not found: SkullBlockRenderer.globalModels");
                return null;
            } else {
                map.put(this.skullBlockType, (SkullModelBase)modelBase);
                return blockentityrenderer;
            }
        }
    }
}