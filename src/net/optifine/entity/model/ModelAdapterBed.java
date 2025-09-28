package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModelAdapterBed extends ModelAdapterBlockEntity {
    public ModelAdapterBed() {
        super(BlockEntityType.BED, "bed");
    }

    @Override
    public Model makeModel() {
        return new BedModel();
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart) {
        if (!(model instanceof BedModel bedmodel)) {
            return null;
        } else if (modelPart.equals("head")) {
            return bedmodel.headPiece;
        } else if (modelPart.equals("foot")) {
            return bedmodel.footPiece;
        } else {
            ModelPart[] amodelpart = bedmodel.legs;
            if (amodelpart != null) {
                if (modelPart.equals("leg1")) {
                    return amodelpart[0];
                }

                if (modelPart.equals("leg2")) {
                    return amodelpart[1];
                }

                if (modelPart.equals("leg3")) {
                    return amodelpart[2];
                }

                if (modelPart.equals("leg4")) {
                    return amodelpart[3];
                }
            }

            return null;
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"head", "foot", "leg1", "leg2", "leg3", "leg4"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        BlockEntityRenderer blockentityrenderer = rendererCache.get(BlockEntityType.BED, index, () -> new BedRenderer(this.getContext()));
        BedModel bedmodel = (BedModel)modelBase;
        return bedmodel.updateRenderer(blockentityrenderer);
    }
}