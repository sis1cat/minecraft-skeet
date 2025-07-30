package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class LightSectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final Duration REFRESH_INTERVAL = Duration.ofMillis(500L);
    private static final int RADIUS = 10;
    private static final Vector4f LIGHT_AND_BLOCKS_COLOR = new Vector4f(1.0F, 1.0F, 0.0F, 0.25F);
    private static final Vector4f LIGHT_ONLY_COLOR = new Vector4f(0.25F, 0.125F, 0.0F, 0.125F);
    private final Minecraft minecraft;
    private final LightLayer lightLayer;
    private Instant lastUpdateTime = Instant.now();
    @Nullable
    private LightSectionDebugRenderer.SectionData data;

    public LightSectionDebugRenderer(Minecraft pMinecraft, LightLayer pLightLayer) {
        this.minecraft = pMinecraft;
        this.lightLayer = pLightLayer;
    }

    @Override
    public void render(PoseStack p_281418_, MultiBufferSource p_282487_, double p_282164_, double p_282565_, double p_281615_) {
        Instant instant = Instant.now();
        if (this.data == null || Duration.between(this.lastUpdateTime, instant).compareTo(REFRESH_INTERVAL) > 0) {
            this.lastUpdateTime = instant;
            this.data = new LightSectionDebugRenderer.SectionData(
                this.minecraft.level.getLightEngine(), SectionPos.of(this.minecraft.player.blockPosition()), 10, this.lightLayer
            );
        }

        renderEdges(p_281418_, this.data.lightAndBlocksShape, this.data.minPos, p_282487_, p_282164_, p_282565_, p_281615_, LIGHT_AND_BLOCKS_COLOR);
        renderEdges(p_281418_, this.data.lightShape, this.data.minPos, p_282487_, p_282164_, p_282565_, p_281615_, LIGHT_ONLY_COLOR);
        VertexConsumer vertexconsumer = p_282487_.getBuffer(RenderType.debugSectionQuads());
        renderFaces(p_281418_, this.data.lightAndBlocksShape, this.data.minPos, vertexconsumer, p_282164_, p_282565_, p_281615_, LIGHT_AND_BLOCKS_COLOR);
        renderFaces(p_281418_, this.data.lightShape, this.data.minPos, vertexconsumer, p_282164_, p_282565_, p_281615_, LIGHT_ONLY_COLOR);
    }

    private static void renderFaces(
        PoseStack pPoseStack,
        DiscreteVoxelShape pShape,
        SectionPos pPos,
        VertexConsumer pBuffer,
        double pCamX,
        double pCamY,
        double pCamZ,
        Vector4f pColor
    ) {
        pShape.forAllFaces((p_282087_, p_283360_, p_282854_, p_282233_) -> {
            int i = p_283360_ + pPos.getX();
            int j = p_282854_ + pPos.getY();
            int k = p_282233_ + pPos.getZ();
            renderFace(pPoseStack, pBuffer, p_282087_, pCamX, pCamY, pCamZ, i, j, k, pColor);
        });
    }

    private static void renderEdges(
        PoseStack pPoseStack,
        DiscreteVoxelShape pShape,
        SectionPos pPos,
        MultiBufferSource pBufferSource,
        double pCamX,
        double pCamY,
        double pCamZ,
        Vector4f pColor
    ) {
        pShape.forAllEdges((p_283441_, p_283631_, p_282083_, p_281900_, p_281481_, p_283547_) -> {
            int i = p_283441_ + pPos.getX();
            int j = p_283631_ + pPos.getY();
            int k = p_282083_ + pPos.getZ();
            int l = p_281900_ + pPos.getX();
            int i1 = p_281481_ + pPos.getY();
            int j1 = p_283547_ + pPos.getZ();
            VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.debugLineStrip(1.0));
            renderEdge(pPoseStack, vertexconsumer, pCamX, pCamY, pCamZ, i, j, k, l, i1, j1, pColor);
        }, true);
    }

    private static void renderFace(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        Direction pFace,
        double pCamX,
        double pCamY,
        double pCamZ,
        int pBlockX,
        int pBlockY,
        int pBlockZ,
        Vector4f pColor
    ) {
        float f = (float)((double)SectionPos.sectionToBlockCoord(pBlockX) - pCamX);
        float f1 = (float)((double)SectionPos.sectionToBlockCoord(pBlockY) - pCamY);
        float f2 = (float)((double)SectionPos.sectionToBlockCoord(pBlockZ) - pCamZ);
        ShapeRenderer.renderFace(
            pPoseStack, pBuffer, pFace, f, f1, f2, f + 16.0F, f1 + 16.0F, f2 + 16.0F, pColor.x(), pColor.y(), pColor.z(), pColor.w()
        );
    }

    private static void renderEdge(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        double pCamX,
        double pCamY,
        double pCamZ,
        int pX1,
        int pY1,
        int pZ1,
        int pX2,
        int pY2,
        int pZ2,
        Vector4f pColor
    ) {
        float f = (float)((double)SectionPos.sectionToBlockCoord(pX1) - pCamX);
        float f1 = (float)((double)SectionPos.sectionToBlockCoord(pY1) - pCamY);
        float f2 = (float)((double)SectionPos.sectionToBlockCoord(pZ1) - pCamZ);
        float f3 = (float)((double)SectionPos.sectionToBlockCoord(pX2) - pCamX);
        float f4 = (float)((double)SectionPos.sectionToBlockCoord(pY2) - pCamY);
        float f5 = (float)((double)SectionPos.sectionToBlockCoord(pZ2) - pCamZ);
        Matrix4f matrix4f = pPoseStack.last().pose();
        pBuffer.addVertex(matrix4f, f, f1, f2).setColor(pColor.x(), pColor.y(), pColor.z(), 1.0F);
        pBuffer.addVertex(matrix4f, f3, f4, f5).setColor(pColor.x(), pColor.y(), pColor.z(), 1.0F);
    }

    @OnlyIn(Dist.CLIENT)
    static final class SectionData {
        final DiscreteVoxelShape lightAndBlocksShape;
        final DiscreteVoxelShape lightShape;
        final SectionPos minPos;

        SectionData(LevelLightEngine pLevelLightEngine, SectionPos pPos, int pRadius, LightLayer pLightLayer) {
            int i = pRadius * 2 + 1;
            this.lightAndBlocksShape = new BitSetDiscreteVoxelShape(i, i, i);
            this.lightShape = new BitSetDiscreteVoxelShape(i, i, i);

            for (int j = 0; j < i; j++) {
                for (int k = 0; k < i; k++) {
                    for (int l = 0; l < i; l++) {
                        SectionPos sectionpos = SectionPos.of(
                            pPos.x() + l - pRadius, pPos.y() + k - pRadius, pPos.z() + j - pRadius
                        );
                        LayerLightSectionStorage.SectionType layerlightsectionstorage$sectiontype = pLevelLightEngine.getDebugSectionType(pLightLayer, sectionpos);
                        if (layerlightsectionstorage$sectiontype == LayerLightSectionStorage.SectionType.LIGHT_AND_DATA) {
                            this.lightAndBlocksShape.fill(l, k, j);
                            this.lightShape.fill(l, k, j);
                        } else if (layerlightsectionstorage$sectiontype == LayerLightSectionStorage.SectionType.LIGHT_ONLY) {
                            this.lightShape.fill(l, k, j);
                        }
                    }
                }
            }

            this.minPos = SectionPos.of(pPos.x() - pRadius, pPos.y() - pRadius, pPos.z() - pRadius);
        }
    }
}