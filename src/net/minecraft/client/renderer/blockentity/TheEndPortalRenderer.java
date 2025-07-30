package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.optifine.Config;
import net.optifine.shaders.ShadersRender;
import org.joml.Matrix4f;

public class TheEndPortalRenderer<T extends TheEndPortalBlockEntity> implements BlockEntityRenderer<T> {
    public static final ResourceLocation END_SKY_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
    public static final ResourceLocation END_PORTAL_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");

    public TheEndPortalRenderer(BlockEntityRendererProvider.Context pContext) {
    }

    public void render(T p_112650_, float p_112651_, PoseStack p_112652_, MultiBufferSource p_112653_, int p_112654_, int p_112655_) {
        if (!Config.isShaders() || !ShadersRender.renderEndPortal(p_112650_, p_112651_, this.getOffsetUp(), p_112652_, p_112653_, p_112654_, p_112655_)) {
            Matrix4f matrix4f = p_112652_.last().pose();
            this.renderCube(p_112650_, matrix4f, p_112653_.getBuffer(this.renderType()));
        }
    }

    private void renderCube(T pBlockEntity, Matrix4f pPose, VertexConsumer pConsumer) {
        float f = this.getOffsetDown();
        float f1 = this.getOffsetUp();
        this.renderFace(pBlockEntity, pPose, pConsumer, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, Direction.SOUTH);
        this.renderFace(pBlockEntity, pPose, pConsumer, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Direction.NORTH);
        this.renderFace(pBlockEntity, pPose, pConsumer, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.EAST);
        this.renderFace(pBlockEntity, pPose, pConsumer, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.WEST);
        this.renderFace(pBlockEntity, pPose, pConsumer, 0.0F, 1.0F, f, f, 0.0F, 0.0F, 1.0F, 1.0F, Direction.DOWN);
        this.renderFace(pBlockEntity, pPose, pConsumer, 0.0F, 1.0F, f1, f1, 1.0F, 1.0F, 0.0F, 0.0F, Direction.UP);
    }

    private void renderFace(
        T pBlockEntity,
        Matrix4f pPose,
        VertexConsumer pConsumer,
        float pX0,
        float pX1,
        float pY0,
        float pY1,
        float pZ0,
        float pZ1,
        float pZ2,
        float pZ3,
        Direction pDirection
    ) {
        if (pBlockEntity.shouldRenderFace(pDirection)) {
            pConsumer.addVertex(pPose, pX0, pY0, pZ0);
            pConsumer.addVertex(pPose, pX1, pY0, pZ1);
            pConsumer.addVertex(pPose, pX1, pY1, pZ2);
            pConsumer.addVertex(pPose, pX0, pY1, pZ3);
        }
    }

    protected float getOffsetUp() {
        return 0.75F;
    }

    protected float getOffsetDown() {
        return 0.375F;
    }

    protected RenderType renderType() {
        return RenderType.endPortal();
    }
}