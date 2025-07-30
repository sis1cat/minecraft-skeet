package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpawnerRenderer implements BlockEntityRenderer<SpawnerBlockEntity> {
    private final EntityRenderDispatcher entityRenderer;

    public SpawnerRenderer(BlockEntityRendererProvider.Context pContext) {
        this.entityRenderer = pContext.getEntityRenderer();
    }

    public void render(SpawnerBlockEntity p_112563_, float p_112564_, PoseStack p_112565_, MultiBufferSource p_112566_, int p_112567_, int p_112568_) {
        Level level = p_112563_.getLevel();
        if (level != null) {
            BaseSpawner basespawner = p_112563_.getSpawner();
            Entity entity = basespawner.getOrCreateDisplayEntity(level, p_112563_.getBlockPos());
            if (entity != null) {
                renderEntityInSpawner(p_112564_, p_112565_, p_112566_, p_112567_, entity, this.entityRenderer, basespawner.getoSpin(), basespawner.getSpin());
            }
        }
    }

    public static void renderEntityInSpawner(
        float pPartialTick,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        Entity pEntity,
        EntityRenderDispatcher pEntityRenderer,
        double pOSpin,
        double pSpin
    ) {
        pPoseStack.pushPose();
        pPoseStack.translate(0.5F, 0.0F, 0.5F);
        float f = 0.53125F;
        float f1 = Math.max(pEntity.getBbWidth(), pEntity.getBbHeight());
        if ((double)f1 > 1.0) {
            f /= f1;
        }

        pPoseStack.translate(0.0F, 0.4F, 0.0F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees((float)Mth.lerp((double)pPartialTick, pOSpin, pSpin) * 10.0F));
        pPoseStack.translate(0.0F, -0.2F, 0.0F);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
        pPoseStack.scale(f, f, f);
        pEntityRenderer.render(pEntity, 0.0, 0.0, 0.0, pPartialTick, pPoseStack, pBuffer, pPackedLight);
        pPoseStack.popPose();
    }
}