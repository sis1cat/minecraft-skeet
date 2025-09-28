package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.MatrixUtil;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.optifine.EmissiveTextures;
import net.optifine.reflect.Reflector;
import net.optifine.render.VertexBuilderWrapper;
import net.optifine.shaders.Shaders;

public class ItemRenderer {
    public static final ResourceLocation ENCHANTED_GLINT_ENTITY = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_entity.png");
    public static final ResourceLocation ENCHANTED_GLINT_ITEM = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    public static final int GUI_SLOT_CENTER_X = 8;
    public static final int GUI_SLOT_CENTER_Y = 8;
    public static final int ITEM_DECORATION_BLIT_OFFSET = 200;
    public static final float COMPASS_FOIL_UI_SCALE = 0.5F;
    public static final float COMPASS_FOIL_FIRST_PERSON_SCALE = 0.75F;
    public static final float COMPASS_FOIL_TEXTURE_SCALE = 0.0078125F;
    public static final int NO_TINT = -1;
    private final ItemModelResolver resolver;
    private final ItemStackRenderState scratchItemStackRenderState = new ItemStackRenderState();
    private static boolean renderItemGui = false;

    public ItemRenderer(ItemModelResolver pResolver) {
        this.resolver = pResolver;
    }

    public static void renderModelLists(BakedModel pModel, int[] pTintLayers, int pPackedLight, int pPackedOverlay, PoseStack pPoseStack, VertexConsumer pBuffer) {
        RandomSource randomsource = RandomSource.create();
        long i = 42L;

        for (Direction direction : Direction.VALUES) {
            randomsource.setSeed(42L);
            renderQuadList(pPoseStack, pBuffer, pModel.getQuads(null, direction, randomsource), pTintLayers, pPackedLight, pPackedOverlay);
        }

        randomsource.setSeed(42L);
        renderQuadList(pPoseStack, pBuffer, pModel.getQuads(null, null, randomsource), pTintLayers, pPackedLight, pPackedOverlay);
    }

    public static void renderItem(
        ItemDisplayContext pDisplayContext,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        int pPackedOverlay,
        int[] pTintLayers,
        BakedModel pModel,
        RenderType pRenderType,
        ItemStackRenderState.FoilType pFoilType
    ) {
        VertexConsumer vertexconsumer;
        if (pFoilType == ItemStackRenderState.FoilType.SPECIAL) {
            PoseStack.Pose posestack$pose = pPoseStack.last().copy();
            if (pDisplayContext == ItemDisplayContext.GUI) {
                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.5F);
            } else if (pDisplayContext.firstPerson()) {
                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.75F);
            }

            vertexconsumer = getCompassFoilBuffer(pBufferSource, pRenderType, posestack$pose);
        } else {
            vertexconsumer = getFoilBuffer(pBufferSource, pRenderType, true, pFoilType != ItemStackRenderState.FoilType.NONE);
        }

        if (EmissiveTextures.isActive()) {
            EmissiveTextures.beginRender();
        }

        renderModelLists(pModel, pTintLayers, pPackedLight, pPackedOverlay, pPoseStack, vertexconsumer);
        if (EmissiveTextures.isActive()) {
            if (EmissiveTextures.hasEmissive()) {
                EmissiveTextures.beginRenderEmissive();
                VertexConsumer vertexconsumer1 = vertexconsumer instanceof VertexBuilderWrapper
                    ? ((VertexBuilderWrapper)vertexconsumer).getVertexBuilder()
                    : vertexconsumer;
                renderModelLists(pModel, pTintLayers, LightTexture.MAX_BRIGHTNESS, pPackedOverlay, pPoseStack, vertexconsumer1);
                EmissiveTextures.endRenderEmissive();
            }

            EmissiveTextures.endRender();
        }
    }

    public static VertexConsumer getArmorFoilBuffer(MultiBufferSource pBufferSource, RenderType pRenderType, boolean pHasFoil) {
        if (Shaders.isShadowPass) {
            pHasFoil = false;
        }

        if (EmissiveTextures.isRenderEmissive()) {
            pHasFoil = false;
        }

        return pHasFoil ? VertexMultiConsumer.create(pBufferSource.getBuffer(RenderType.armorEntityGlint()), pBufferSource.getBuffer(pRenderType)) : pBufferSource.getBuffer(pRenderType);
    }

    private static VertexConsumer getCompassFoilBuffer(MultiBufferSource pBufferSource, RenderType pRenderType, PoseStack.Pose pPose) {
        return VertexMultiConsumer.create(
            new SheetedDecalTextureGenerator(pBufferSource.getBuffer(RenderType.glint()), pPose, 0.0078125F), pBufferSource.getBuffer(pRenderType)
        );
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource pBufferSource, RenderType pRenderType, boolean pIsItem, boolean pGlint) {
        if (Shaders.isShadowPass) {
            pGlint = false;
        }

        if (EmissiveTextures.isRenderEmissive()) {
            pGlint = false;
        }

        if (!pGlint) {
            return pBufferSource.getBuffer(pRenderType);
        } else {
            return Minecraft.useShaderTransparency() && pRenderType == Sheets.translucentItemSheet()
                ? VertexMultiConsumer.create(pBufferSource.getBuffer(RenderType.glintTranslucent()), pBufferSource.getBuffer(pRenderType))
                : VertexMultiConsumer.create(pBufferSource.getBuffer(pIsItem ? RenderType.glint() : RenderType.entityGlint()), pBufferSource.getBuffer(pRenderType));
        }
    }

    private static int getLayerColorSafe(int[] pTintLayers, int pIndex) {
        return pIndex >= pTintLayers.length ? -1 : pTintLayers[pIndex];
    }

    private static void renderQuadList(PoseStack pPoseStack, VertexConsumer pBuffer, List<BakedQuad> pQuads, int[] pTintLayers, int pPackedLight, int pPackedOverlay) {
        PoseStack.Pose posestack$pose = pPoseStack.last();
        boolean flag = EmissiveTextures.isActive();
        int i = pQuads.size();

        for (int j = 0; j < i; j++) {
            BakedQuad bakedquad = pQuads.get(j);
            if (flag) {
                bakedquad = EmissiveTextures.getEmissiveQuad(bakedquad);
                if (bakedquad == null) {
                    continue;
                }
            }

            float f;
            float f1;
            float f2;
            float f3;
            if (bakedquad.isTinted()) {
                int k = getLayerColorSafe(pTintLayers, bakedquad.getTintIndex());
                f = (float)ARGB.alpha(k) / 255.0F;
                f1 = (float)ARGB.red(k) / 255.0F;
                f2 = (float)ARGB.green(k) / 255.0F;
                f3 = (float)ARGB.blue(k) / 255.0F;
            } else {
                f = 1.0F;
                f1 = 1.0F;
                f2 = 1.0F;
                f3 = 1.0F;
            }

            if (Reflector.ForgeHooksClient.exists()) {
                pBuffer.putBulkData(posestack$pose, bakedquad, f1, f2, f3, f, pPackedLight, pPackedOverlay, true);
            } else {
                pBuffer.putBulkData(posestack$pose, bakedquad, f1, f2, f3, f, pPackedLight, pPackedOverlay);
            }
        }
    }

    public void renderStatic(
        ItemStack pStack,
        ItemDisplayContext pDisplayContext,
        int pCombinedLight,
        int pCombinedOverlay,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        @Nullable Level pLevel,
        int pSeed
    ) {
        this.renderStatic(null, pStack, pDisplayContext, false, pPoseStack, pBufferSource, pLevel, pCombinedLight, pCombinedOverlay, pSeed);
    }

    public void renderStatic(
        @Nullable LivingEntity pEntity,
        ItemStack pItemStack,
        ItemDisplayContext pDiplayContext,
        boolean pLeftHand,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        @Nullable Level pLevel,
        int pCombinedLight,
        int pCombinedOverlay,
        int pSeed
    ) {
        this.resolver.updateForTopItem(this.scratchItemStackRenderState, pItemStack, pDiplayContext, pLeftHand, pLevel, pEntity, pSeed);
        this.scratchItemStackRenderState.render(pPoseStack, pBufferSource, pCombinedLight, pCombinedOverlay);
    }

    public static boolean isRenderItemGui() {
        return renderItemGui;
    }

    public static void setRenderItemGui(boolean renderItemGui) {
        ItemRenderer.renderItemGui = renderItemGui;
    }
}