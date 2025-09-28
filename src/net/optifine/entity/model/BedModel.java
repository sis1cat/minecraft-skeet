package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class BedModel extends Model {
    public ModelPart headPiece;
    public ModelPart footPiece;
    public ModelPart[] legs = new ModelPart[4];

    public BedModel() {
        super(ModelPart.makeRoot(), RenderType::entityCutoutNoCull);
        BlockEntityRenderDispatcher blockentityrenderdispatcher = Config.getMinecraft().getBlockEntityRenderDispatcher();
        BedRenderer bedrenderer = new BedRenderer(blockentityrenderdispatcher.getContext());
        Model model = (Model)Reflector.TileEntityBedRenderer_headModel.getValue(bedrenderer);
        if (model != null) {
            ModelPart modelpart = model.root();
            this.headPiece = modelpart.getChild("main");
            this.legs[0] = modelpart.getChild("left_leg");
            this.legs[1] = modelpart.getChild("right_leg");
            this.root().addChildModel("head", modelpart);
        }

        Model model1 = (Model)Reflector.TileEntityBedRenderer_footModel.getValue(bedrenderer);
        if (model1 != null) {
            ModelPart modelpart1 = model1.root();
            this.footPiece = modelpart1.getChild("main");
            this.legs[2] = modelpart1.getChild("left_leg");
            this.legs[3] = modelpart1.getChild("right_leg");
            this.root().addChildModel("foot", modelpart1);
        }
    }

    public BlockEntityRenderer updateRenderer(BlockEntityRenderer renderer) {
        if (!Reflector.TileEntityBedRenderer_headModel.exists()) {
            Config.warn("Field not found: TileEntityBedRenderer.head");
            return null;
        } else if (!Reflector.TileEntityBedRenderer_footModel.exists()) {
            Config.warn("Field not found: TileEntityBedRenderer.footModel");
            return null;
        } else {
            Model model = (Model)Reflector.TileEntityBedRenderer_headModel.getValue(renderer);
            if (model != null) {
                ModelPart modelpart = model.root();
                modelpart.addChildModel("main", this.headPiece);
                modelpart.addChildModel("left_leg", this.legs[0]);
                modelpart.addChildModel("right_leg", this.legs[1]);
            }

            Model model1 = (Model)Reflector.TileEntityBedRenderer_footModel.getValue(renderer);
            if (model1 != null) {
                ModelPart modelpart1 = model1.root();
                modelpart1.addChildModel("main", this.footPiece);
                modelpart1.addChildModel("left_leg", this.legs[2]);
                modelpart1.addChildModel("right_leg", this.legs[3]);
            }

            return renderer;
        }
    }

    @Override
    public boolean isRenderRoot() {
        return false;
    }
}