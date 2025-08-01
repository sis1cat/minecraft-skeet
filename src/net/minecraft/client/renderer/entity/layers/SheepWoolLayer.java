package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.optifine.Config;
import net.optifine.CustomColors;

public class SheepWoolLayer extends RenderLayer<SheepRenderState, SheepModel> {
    private static final ResourceLocation SHEEP_FUR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/sheep/sheep_fur.png");
    public EntityModel<SheepRenderState> adultModel;
    public EntityModel<SheepRenderState> babyModel;

    public SheepWoolLayer(RenderLayerParent<SheepRenderState, SheepModel> pRenderer, EntityModelSet pModelSet) {
        super(pRenderer);
        this.adultModel = new SheepFurModel(pModelSet.bakeLayer(ModelLayers.SHEEP_WOOL));
        this.babyModel = new SheepFurModel(pModelSet.bakeLayer(ModelLayers.SHEEP_BABY_WOOL));
    }

    public void render(PoseStack p_362211_, MultiBufferSource p_366726_, int p_362383_, SheepRenderState p_366463_, float p_364799_, float p_361838_) {
        if (!p_366463_.isSheared) {
            EntityModel<SheepRenderState> entitymodel = p_366463_.isBaby ? this.babyModel : this.adultModel;
            if (p_366463_.isInvisible) {
                if (p_366463_.appearsGlowing) {
                    entitymodel.setupAnim(p_366463_);
                    VertexConsumer vertexconsumer = p_366726_.getBuffer(RenderType.outline(SHEEP_FUR_LOCATION));
                    entitymodel.renderToBuffer(p_362211_, vertexconsumer, p_362383_, LivingEntityRenderer.getOverlayCoords(p_366463_, 0.0F), -16777216);
                }
            } else {
                int i2;
                if (p_366463_.customName != null && "jeb_".equals(p_366463_.customName.getString())) {
                    int i = 25;
                    int j = Mth.floor(p_366463_.ageInTicks);
                    int k = j / 25 + p_366463_.id;
                    int l = DyeColor.values().length;
                    int i1 = k % l;
                    int j1 = (k + 1) % l;
                    float f = ((float)(j % 25) + Mth.frac(p_366463_.ageInTicks)) / 25.0F;
                    int k1 = Sheep.getColor(DyeColor.byId(i1));
                    int l1 = Sheep.getColor(DyeColor.byId(j1));
                    if (Config.isCustomColors()) {
                        k1 = CustomColors.getSheepColors(DyeColor.byId(i1), k1);
                        l1 = CustomColors.getSheepColors(DyeColor.byId(j1), l1);
                    }

                    i2 = ARGB.lerp(f, k1, l1);
                } else {
                    i2 = Sheep.getColor(p_366463_.woolColor);
                    if (Config.isCustomColors()) {
                        i2 = CustomColors.getSheepColors(p_366463_.woolColor, i2);
                    }
                }

                coloredCutoutModelCopyLayerRender(entitymodel, SHEEP_FUR_LOCATION, p_362211_, p_366726_, p_362383_, p_366463_, i2);
            }
        }
    }
}