package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.optifine.Config;

public class MushroomCowMushroomLayer extends RenderLayer<MushroomCowRenderState, CowModel> {
    private final BlockRenderDispatcher blockRenderer;
    private ModelPart modelRendererMushroom;
    private static final ResourceLocation LOCATION_MUSHROOM_RED = new ResourceLocation("textures/entity/cow/red_mushroom.png");
    private static final ResourceLocation LOCATION_MUSHROOM_BROWN = new ResourceLocation("textures/entity/cow/brown_mushroom.png");
    private static boolean hasTextureMushroomRed = false;
    private static boolean hasTextureMushroomBrown = false;

    public MushroomCowMushroomLayer(RenderLayerParent<MushroomCowRenderState, CowModel> pRenderer, BlockRenderDispatcher pBlockRenderer) {
        super(pRenderer);
        this.blockRenderer = pBlockRenderer;
        this.modelRendererMushroom = new ModelPart(new ArrayList<>(), new HashMap<>());
        this.modelRendererMushroom.setTextureSize(16, 16);
        this.modelRendererMushroom.x = 8.0F;
        this.modelRendererMushroom.z = 8.0F;
        this.modelRendererMushroom.yRot = (float) (Math.PI / 4);
        float[][] afloat = new float[][]{null, null, {16.0F, 16.0F, 0.0F, 0.0F}, {16.0F, 16.0F, 0.0F, 0.0F}, null, null};
        this.modelRendererMushroom.addBox(afloat, -10.0F, 0.0F, 0.0F, 20.0F, 16.0F, 0.0F, 0.0F);
        float[][] afloat1 = new float[][]{null, null, null, null, {16.0F, 16.0F, 0.0F, 0.0F}, {16.0F, 16.0F, 0.0F, 0.0F}};
        this.modelRendererMushroom.addBox(afloat1, 0.0F, 0.0F, -10.0F, 0.0F, 16.0F, 20.0F, 0.0F);
    }

    public void render(PoseStack p_117256_, MultiBufferSource p_117257_, int p_117258_, MushroomCowRenderState p_367819_, float p_117260_, float p_117261_) {
        if (!p_367819_.isBaby) {
            boolean flag = p_367819_.appearsGlowing && p_367819_.isInvisible;
            if (!p_367819_.isInvisible || flag) {
                BlockState blockstate = p_367819_.variant.getBlockState();
                ResourceLocation resourcelocation = this.getCustomMushroom(blockstate);
                VertexConsumer vertexconsumer = null;
                if (resourcelocation != null) {
                    vertexconsumer = p_117257_.getBuffer(RenderType.entityCutout(resourcelocation));
                }

                int i = LivingEntityRenderer.getOverlayCoords(p_367819_, 0.0F);
                BakedModel bakedmodel = this.blockRenderer.getBlockModel(blockstate);
                p_117256_.pushPose();
                p_117256_.translate(0.2F, -0.35F, 0.5F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(-48.0F));
                p_117256_.scale(-1.0F, -1.0F, 1.0F);
                p_117256_.translate(-0.5F, -0.5F, -0.5F);
                if (resourcelocation != null) {
                    this.modelRendererMushroom.render(p_117256_, vertexconsumer, p_117258_, i);
                } else {
                    this.renderMushroomBlock(p_117256_, p_117257_, p_117258_, flag, blockstate, i, bakedmodel);
                }

                p_117256_.popPose();
                p_117256_.pushPose();
                p_117256_.translate(0.2F, -0.35F, 0.5F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(42.0F));
                p_117256_.translate(0.1F, 0.0F, -0.6F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(-48.0F));
                p_117256_.scale(-1.0F, -1.0F, 1.0F);
                p_117256_.translate(-0.5F, -0.5F, -0.5F);
                if (resourcelocation != null) {
                    this.modelRendererMushroom.render(p_117256_, vertexconsumer, p_117258_, i);
                } else {
                    this.renderMushroomBlock(p_117256_, p_117257_, p_117258_, flag, blockstate, i, bakedmodel);
                }

                p_117256_.popPose();
                p_117256_.pushPose();
                this.getParentModel().getHead().translateAndRotate(p_117256_);
                p_117256_.translate(0.0F, -0.7F, -0.2F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(-78.0F));
                p_117256_.scale(-1.0F, -1.0F, 1.0F);
                p_117256_.translate(-0.5F, -0.5F, -0.5F);
                if (resourcelocation != null) {
                    this.modelRendererMushroom.render(p_117256_, vertexconsumer, p_117258_, i);
                } else {
                    this.renderMushroomBlock(p_117256_, p_117257_, p_117258_, flag, blockstate, i, bakedmodel);
                }

                p_117256_.popPose();
            }
        }
    }

    private void renderMushroomBlock(
        PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, boolean pOutlineOnly, BlockState pState, int pPackedOverlay, BakedModel pModel
    ) {
        if (pOutlineOnly) {
            this.blockRenderer
                .getModelRenderer()
                .renderModel(
                    pPoseStack.last(),
                    pBuffer.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS)),
                    pState,
                    pModel,
                    0.0F,
                    0.0F,
                    0.0F,
                    pPackedLight,
                    pPackedOverlay
                );
        } else {
            this.blockRenderer.renderSingleBlock(pState, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
        }
    }

    private ResourceLocation getCustomMushroom(BlockState iblockstate) {
        Block block = iblockstate.getBlock();
        if (block == Blocks.RED_MUSHROOM && hasTextureMushroomRed) {
            return LOCATION_MUSHROOM_RED;
        } else {
            return block == Blocks.BROWN_MUSHROOM && hasTextureMushroomBrown ? LOCATION_MUSHROOM_BROWN : null;
        }
    }

    public static void update() {
        hasTextureMushroomRed = Config.hasResource(LOCATION_MUSHROOM_RED);
        hasTextureMushroomBrown = Config.hasResource(LOCATION_MUSHROOM_BROWN);
    }
}