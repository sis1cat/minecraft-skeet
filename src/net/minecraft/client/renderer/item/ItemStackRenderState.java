package net.minecraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.Config;
import net.optifine.CustomItems;
import net.optifine.reflect.Reflector;

public class ItemStackRenderState {
    ItemDisplayContext displayContext = ItemDisplayContext.NONE;
    boolean isLeftHand;
    private int activeLayerCount;
    private ItemStackRenderState.LayerRenderState[] layers = new ItemStackRenderState.LayerRenderState[]{new ItemStackRenderState.LayerRenderState()};

    public void ensureCapacity(int pExpectedSize) {
        int i = this.layers.length;
        int j = this.activeLayerCount + pExpectedSize;
        if (j > i) {
            this.layers = Arrays.copyOf(this.layers, j);

            for (int k = i; k < j; k++) {
                this.layers[k] = new ItemStackRenderState.LayerRenderState();
            }
        }
    }

    public ItemStackRenderState.LayerRenderState newLayer() {
        this.ensureCapacity(1);
        return this.layers[this.activeLayerCount++];
    }

    public void clear() {
        this.displayContext = ItemDisplayContext.NONE;
        this.isLeftHand = false;

        for (int i = 0; i < this.activeLayerCount; i++) {
            this.layers[i].clear();
        }

        this.activeLayerCount = 0;
    }

    private ItemStackRenderState.LayerRenderState firstLayer() {
        return this.layers[0];
    }

    public boolean isEmpty() {
        return this.activeLayerCount == 0;
    }

    public boolean isGui3d() {
        return this.firstLayer().isGui3d();
    }

    public boolean usesBlockLight() {
        return this.firstLayer().usesBlockLight();
    }

    @Nullable
    public TextureAtlasSprite pickParticleIcon(RandomSource pRandom) {
        if (this.activeLayerCount == 0) {
            return null;
        } else {
            BakedModel bakedmodel = this.layers[pRandom.nextInt(this.activeLayerCount)].renderModel;
            if (Reflector.ForgeHooksClient.exists()) {
                return bakedmodel == null ? null : bakedmodel.getParticleIcon(ModelData.EMPTY);
            } else {
                return bakedmodel == null ? null : bakedmodel.getParticleIcon();
            }
        }
    }

    public ItemTransform transform() {
        return this.firstLayer().transform();
    }

    public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        for (int i = 0; i < this.activeLayerCount; i++) {
            this.layers[i].render(pPoseStack, pBufferSource, pPackedLight, pPackedOverlay);
        }
    }

    public static enum FoilType {
        NONE,
        STANDARD,
        SPECIAL;
    }

    public class LayerRenderState {
        @Nullable
        BakedModel model;
        @Nullable
        private RenderType renderType;
        private ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.NONE;
        private int[] tintLayers = new int[0];
        @Nullable
        private SpecialModelRenderer<Object> specialRenderer;
        @Nullable
        private Object argumentForSpecialRendering;
        private ItemStack itemStack;
        private BakedModel renderModel;

        public void clear() {
            this.model = null;
            this.renderType = null;
            this.foilType = ItemStackRenderState.FoilType.NONE;
            this.specialRenderer = null;
            this.argumentForSpecialRendering = null;
            Arrays.fill(this.tintLayers, -1);
            this.renderModel = null;
        }

        public void setupBlockModel(BakedModel pModel, RenderType pRenderType) {
            this.model = pModel;
            this.renderType = pRenderType;
            this.renderModel = this.model;
            if (Config.isCustomItems()) {
                this.model = this.getCustomItemModel(this.model, true);
                this.renderModel = this.getCustomItemModel(this.model, false);
            }
        }

        public <T> void setupSpecialModel(SpecialModelRenderer<T> pSpecialRenderer, @Nullable T pArgumentForSpecialRendering, BakedModel pModel) {
            this.model = pModel;
            this.specialRenderer = eraseSpecialRenderer(pSpecialRenderer);
            this.argumentForSpecialRendering = pArgumentForSpecialRendering;
            this.renderModel = this.model;
            if (Config.isCustomItems()) {
                this.model = this.getCustomItemModel(this.model, true);
                this.renderModel = this.getCustomItemModel(this.model, false);
                if (this.renderModel != pModel && this.itemStack != null) {
                    this.renderType = ItemBlockRenderTypes.getRenderType(this.itemStack);
                }
            }
        }

        private static SpecialModelRenderer<Object> eraseSpecialRenderer(SpecialModelRenderer<?> pSpecialRenderer) {
            return (SpecialModelRenderer<Object>)pSpecialRenderer;
        }

        public void setFoilType(ItemStackRenderState.FoilType pFoilType) {
            this.foilType = pFoilType;
        }

        public int[] prepareTintLayers(int pCount) {
            if (pCount > this.tintLayers.length) {
                this.tintLayers = new int[pCount];
                Arrays.fill(this.tintLayers, -1);
            }

            return this.tintLayers;
        }

        ItemTransform transform() {
            return this.model != null ? this.model.getTransforms().getTransform(ItemStackRenderState.this.displayContext) : ItemTransform.NO_TRANSFORM;
        }

        void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
            pPoseStack.pushPose();
            if (Reflector.ForgeHooksClient.exists() && this.model != null) {
                this.model.applyTransform(ItemStackRenderState.this.displayContext, pPoseStack, ItemStackRenderState.this.isLeftHand);
            } else {
                this.transform().apply(ItemStackRenderState.this.isLeftHand, pPoseStack);
            }

            pPoseStack.translate(-0.5F, -0.5F, -0.5F);
            if (this.specialRenderer != null && this.renderType == null) {
                this.specialRenderer
                    .render(
                        this.argumentForSpecialRendering,
                        ItemStackRenderState.this.displayContext,
                        pPoseStack,
                        pBufferSource,
                        pPackedLight,
                        pPackedOverlay,
                        this.foilType != ItemStackRenderState.FoilType.NONE
                    );
            } else if (this.renderModel != null) {
                ItemRenderer.renderItem(
                    ItemStackRenderState.this.displayContext,
                    pPoseStack,
                    pBufferSource,
                    pPackedLight,
                    pPackedOverlay,
                    this.tintLayers,
                    this.renderModel,
                    this.renderType,
                    this.foilType
                );
            }

            pPoseStack.popPose();
        }

        boolean isGui3d() {
            return this.renderModel != null && this.renderModel.isGui3d();
        }

        boolean usesBlockLight() {
            return this.model != null && this.model.usesBlockLight();
        }

        private BakedModel getCustomItemModel(BakedModel modelIn, boolean fullModelIn) {
            if (modelIn == null) {
                return null;
            } else {
                ResourceLocation resourcelocation = modelIn instanceof SimpleBakedModel simplebakedmodel ? simplebakedmodel.getModelLocation() : null;
                return CustomItems.getCustomItemModel(this.itemStack, modelIn, resourcelocation, fullModelIn);
            }
        }

        public ItemStack getItemStack() {
            return this.itemStack;
        }

        public void setItemStack(ItemStack itemStack) {
            this.itemStack = itemStack;
        }
    }
}