package sisicat.main.functions.visual;

import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import sisicat.main.functions.Function;
import sisicat.main.utilities.ItemsRenderer;

public class CustomItemRenderer extends Function {

    public CustomItemRenderer(String name) {
        super(name);
    }

    @EventTarget
    void _event(IER_render ier_render) {

        ier_render.cancel();

        ier_render.p_115030_.pushPose();

        if(ier_render.p_365095_.entity.getY() == Minecraft.getInstance().player.getEyeY() - 0.3F)
            ier_render.p_115030_.scale(0, 0, 0);

        ItemEntityRenderer.renderMultipleFromCount(ier_render.p_115030_, ier_render.p_115031_, ier_render.p_115032_, ier_render.p_365095_, ier_render.instance.random);

        ier_render.p_115030_.popPose();

    }

    @EventTarget
    void _event(IER_renderMultipleFromCount ier_renderMultipleFromCount) {

        ier_renderMultipleFromCount.cancel();

        ier_renderMultipleFromCount.pRandom.setSeed(ier_renderMultipleFromCount.pRenderState.seed);

        Vec3 rotation = new Vec3(0, 0, 0);

        int itemsCount = ier_renderMultipleFromCount.pRenderState.count;
        ItemStackRenderState itemstackrenderstate = ier_renderMultipleFromCount.pRenderState.item;

        Item item = ((ItemEntity)ier_renderMultipleFromCount.pRenderState.entity).getItem().getItem();

        for (int index = 0; index < itemsCount; index++) {

            Vec3 rot = new Vec3(rotation.toVector3f());

            ier_renderMultipleFromCount.pPoseStack.pushPose();

            float[] modelProperties = ItemsRenderer.getModelProperties(item instanceof BlockItem ? (BlockItem) item : null, ier_renderMultipleFromCount.pRenderState.item, index * 0.3851f);

            ier_renderMultipleFromCount.pPoseStack.rotateDegYn(
                    ier_renderMultipleFromCount.pRenderState.entity.getYRot() + ier_renderMultipleFromCount.pRandom.nextFloat() * 22.5f + index * 5f
            );

            ier_renderMultipleFromCount.pPoseStack.translate(
                    (ier_renderMultipleFromCount.pRandom.nextFloat() * 2f - 1f) * 0.04f,
                    modelProperties[4] + 0.0005f * ier_renderMultipleFromCount.pRandom.nextFloat() + ier_renderMultipleFromCount.pRenderState.entity.getId() % 50 * 0.0001f,
                    (ier_renderMultipleFromCount.pRandom.nextFloat() * 2f - 1f) * 0.04f
            );

            ItemsRenderer.renderItem90(
                    itemstackrenderstate,
                    ier_renderMultipleFromCount.pPoseStack,
                    ier_renderMultipleFromCount.pBufferSource,
                    ier_renderMultipleFromCount.pPackedLight,
                    OverlayTexture.NO_OVERLAY,
                    rot,
                    item instanceof BlockItem ? (BlockItem) item : null
            );

            ier_renderMultipleFromCount.pPoseStack.popPose();

        }

    }

    public static class IER_render extends EventCancellable {

        public IER_render(ItemEntityRenderer instance, ItemEntityRenderState p_365095_, PoseStack p_115030_, MultiBufferSource p_115031_, int p_115032_) {

            this.instance = instance;
            this.p_365095_ = p_365095_;
            this.p_115030_ = p_115030_;
            this.p_115031_ = p_115031_;
            this.p_115032_ = p_115032_;

        }

        public ItemEntityRenderer instance;
        public ItemEntityRenderState p_365095_;
        public PoseStack p_115030_;
        public MultiBufferSource p_115031_;
        public int p_115032_;

    }

    public static class IER_renderMultipleFromCount extends EventCancellable {

        public IER_renderMultipleFromCount(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, ItemClusterRenderState pRenderState, RandomSource pRandom) {

            this.pPoseStack = pPoseStack;
            this.pBufferSource = pBufferSource;
            this.pPackedLight = pPackedLight;
            this.pRenderState = pRenderState;
            this.pRandom = pRandom;

        }

        public PoseStack pPoseStack;
        public MultiBufferSource pBufferSource;
        public int pPackedLight;
        public ItemClusterRenderState pRenderState;
        public RandomSource pRandom;

    }

}
