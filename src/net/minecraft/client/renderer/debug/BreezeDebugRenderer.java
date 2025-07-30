package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.BreezeDebugPayload;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class BreezeDebugRenderer {
    private static final int JUMP_TARGET_LINE_COLOR = ARGB.color(255, 255, 100, 255);
    private static final int TARGET_LINE_COLOR = ARGB.color(255, 100, 255, 255);
    private static final int INNER_CIRCLE_COLOR = ARGB.color(255, 0, 255, 0);
    private static final int MIDDLE_CIRCLE_COLOR = ARGB.color(255, 255, 165, 0);
    private static final int OUTER_CIRCLE_COLOR = ARGB.color(255, 255, 0, 0);
    private static final int CIRCLE_VERTICES = 20;
    private static final float SEGMENT_SIZE_RADIANS = (float) (Math.PI / 10);
    private final Minecraft minecraft;
    private final Map<Integer, BreezeDebugPayload.BreezeInfo> perEntity = new HashMap<>();

    public BreezeDebugRenderer(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    public void render(PoseStack pPoseStack, MultiBufferSource pBuffer, double pXOffset, double pYOffset, double pZOffset) {
        LocalPlayer localplayer = this.minecraft.player;
        localplayer.level()
            .getEntities(EntityType.BREEZE, localplayer.getBoundingBox().inflate(100.0), p_312249_ -> true)
            .forEach(
                p_357966_ -> {
                    Optional<BreezeDebugPayload.BreezeInfo> optional = Optional.ofNullable(this.perEntity.get(p_357966_.getId()));
                    optional.map(BreezeDebugPayload.BreezeInfo::attackTarget)
                        .map(p_374653_ -> localplayer.level().getEntity(p_374653_))
                        .map(p_357967_ -> p_357967_.getPosition(this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)))
                        .ifPresent(
                            p_310972_ -> {
                                drawLine(pPoseStack, pBuffer, pXOffset, pYOffset, pZOffset, p_357966_.position(), p_310972_, TARGET_LINE_COLOR);
                                Vec3 vec3 = p_310972_.add(0.0, 0.01F, 0.0);
                                drawCircle(
                                    pPoseStack.last().pose(),
                                    pXOffset,
                                    pYOffset,
                                    pZOffset,
                                    pBuffer.getBuffer(RenderType.debugLineStrip(2.0)),
                                    vec3,
                                    4.0F,
                                    INNER_CIRCLE_COLOR
                                );
                                drawCircle(
                                    pPoseStack.last().pose(),
                                    pXOffset,
                                    pYOffset,
                                    pZOffset,
                                    pBuffer.getBuffer(RenderType.debugLineStrip(2.0)),
                                    vec3,
                                    8.0F,
                                    MIDDLE_CIRCLE_COLOR
                                );
                                drawCircle(
                                    pPoseStack.last().pose(),
                                    pXOffset,
                                    pYOffset,
                                    pZOffset,
                                    pBuffer.getBuffer(RenderType.debugLineStrip(2.0)),
                                    vec3,
                                    24.0F,
                                    OUTER_CIRCLE_COLOR
                                );
                            }
                        );
                    optional.map(BreezeDebugPayload.BreezeInfo::jumpTarget)
                        .ifPresent(
                            p_374651_ -> {
                                drawLine(pPoseStack, pBuffer, pXOffset, pYOffset, pZOffset, p_357966_.position(), p_374651_.getCenter(), JUMP_TARGET_LINE_COLOR);
                                DebugRenderer.renderFilledBox(
                                    pPoseStack,
                                    pBuffer,
                                    AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(p_374651_)).move(-pXOffset, -pYOffset, -pZOffset),
                                    1.0F,
                                    0.0F,
                                    0.0F,
                                    1.0F
                                );
                            }
                        );
                }
            );
    }

    private static void drawLine(
        PoseStack pPoseStack, MultiBufferSource pBuffer, double pXOffset, double pYOffset, double pZOffset, Vec3 pFromPos, Vec3 pToPos, int pColor
    ) {
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.debugLineStrip(2.0));
        vertexconsumer.addVertex(
                pPoseStack.last(), (float)(pFromPos.x - pXOffset), (float)(pFromPos.y - pYOffset), (float)(pFromPos.z - pZOffset)
            )
            .setColor(pColor);
        vertexconsumer.addVertex(
                pPoseStack.last(), (float)(pToPos.x - pXOffset), (float)(pToPos.y - pYOffset), (float)(pToPos.z - pZOffset)
            )
            .setColor(pColor);
    }

    private static void drawCircle(
        Matrix4f pPose, double pXOffset, double pYOffset, double pZOffset, VertexConsumer pConsumer, Vec3 pPos, float pRadius, int pColor
    ) {
        for (int i = 0; i < 20; i++) {
            drawCircleVertex(i, pPose, pXOffset, pYOffset, pZOffset, pConsumer, pPos, pRadius, pColor);
        }

        drawCircleVertex(0, pPose, pXOffset, pYOffset, pZOffset, pConsumer, pPos, pRadius, pColor);
    }

    private static void drawCircleVertex(
        int pIndex,
        Matrix4f pPose,
        double pXOffset,
        double pYOffset,
        double pZOffset,
        VertexConsumer pConsumer,
        Vec3 pCircleCenter,
        float pRadius,
        int pColor
    ) {
        float f = (float)pIndex * (float) (Math.PI / 10);
        Vec3 vec3 = pCircleCenter.add((double)pRadius * Math.cos((double)f), 0.0, (double)pRadius * Math.sin((double)f));
        pConsumer.addVertex(pPose, (float)(vec3.x - pXOffset), (float)(vec3.y - pYOffset), (float)(vec3.z - pZOffset))
            .setColor(pColor);
    }

    public void clear() {
        this.perEntity.clear();
    }

    public void add(BreezeDebugPayload.BreezeInfo pBreeze) {
        this.perEntity.put(pBreeze.id(), pBreeze);
    }
}