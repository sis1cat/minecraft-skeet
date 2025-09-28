package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.entity.model.ModelAdapter;
import net.optifine.util.ArrayUtils;

public class WolfCollarLayer extends RenderLayer<WolfRenderState, WolfModel> {
    private static final ResourceLocation WOLF_COLLAR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_collar.png");
    public WolfModel adultModel = new WolfModel(ModelAdapter.bakeModelLayer(ModelLayers.WOLF));
    public WolfModel babyModel = new WolfModel(ModelAdapter.bakeModelLayer(ModelLayers.WOLF_BABY));

    public WolfCollarLayer(RenderLayerParent<WolfRenderState, WolfModel> p_117707_) {
        super(p_117707_);
    }

    public void render(PoseStack p_117709_, MultiBufferSource p_117710_, int p_117711_, WolfRenderState p_365773_, float p_117713_, float p_117714_) {
        DyeColor dyecolor = p_365773_.collarColor;
        if (dyecolor != null && !p_365773_.isInvisible) {
            int i = dyecolor.getTextureDiffuseColor();
            if (Config.isCustomColors()) {
                i = CustomColors.getWolfCollarColors(dyecolor, i);
            }

            WolfModel wolfmodel = this.getEntityModel(p_365773_);
            ResourceLocation resourcelocation = ArrayUtils.firstNonNull(wolfmodel.locationTextureCustom, WOLF_COLLAR_LOCATION);
            VertexConsumer vertexconsumer = p_117710_.getBuffer(RenderType.entityCutoutNoCull(resourcelocation));
            wolfmodel.renderToBuffer(p_117709_, vertexconsumer, p_117711_, OverlayTexture.NO_OVERLAY, i);
        }
    }

    public WolfModel getEntityModel(WolfRenderState stateIn) {
        WolfModel wolfmodel = stateIn.isBaby ? this.babyModel : this.adultModel;
        if (wolfmodel != null) {
            wolfmodel.setupAnim(stateIn);
            return wolfmodel;
        } else {
            return (WolfModel)super.getParentModel();
        }
    }
}