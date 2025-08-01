package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.Config;

public class ModelAdapterConduit extends ModelAdapterBlockEntity {
    public ModelAdapterConduit() {
        super(BlockEntityType.CONDUIT, "conduit");
    }

    @Override
    public Model makeModel() {
        return new ConduitModel();
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart) {
        if (!(model instanceof ConduitModel conduitmodel)) {
            return null;
        } else if (modelPart.equals("eye")) {
            return conduitmodel.eye;
        } else if (modelPart.equals("wind")) {
            return conduitmodel.wind;
        } else if (modelPart.equals("base")) {
            return conduitmodel.base;
        } else {
            return modelPart.equals("cage") ? conduitmodel.cage : null;
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"eye", "wind", "base", "cage"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        BlockEntityRenderer blockentityrenderer = rendererCache.get(BlockEntityType.CONDUIT, index, () -> new ConduitRenderer(this.getContext()));
        if (!(blockentityrenderer instanceof ConduitRenderer)) {
            return null;
        } else if (!(modelBase instanceof ConduitModel conduitmodel)) {
            Config.warn("Not a conduit model: " + modelBase);
            return null;
        } else {
            return conduitmodel.updateRenderer(blockentityrenderer);
        }
    }
}