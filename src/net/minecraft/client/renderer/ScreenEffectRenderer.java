package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.optifine.Config;
import net.optifine.SmartAnimations;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

public class ScreenEffectRenderer {
    private static final ResourceLocation UNDERWATER_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");

    public static void renderScreenEffect(Minecraft pMinecraft, PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        Player player = pMinecraft.player;
        if (!player.noPhysics) {
            BlockState blockstate = getViewBlockingState(player);
            if (Reflector.ForgeHooksClient_renderBlockOverlay.exists() && Reflector.ForgeBlockModelShapes_getTexture3.exists()) {
                Pair<BlockState, BlockPos> pair = getOverlayBlock(player);
                if (pair != null) {
                    Object object = Reflector.getFieldValue(Reflector.RenderBlockScreenEffectEvent_OverlayType_BLOCK);
                    if (!Reflector.ForgeHooksClient_renderBlockOverlay.callBoolean(player, pPoseStack, object, pair.getLeft(), pair.getRight())) {
                        TextureAtlasSprite textureatlassprite = (TextureAtlasSprite)Reflector.call(
                            pMinecraft.getBlockRenderer().getBlockModelShaper(), Reflector.ForgeBlockModelShapes_getTexture3, pair.getLeft(), pMinecraft.level, pair.getRight()
                        );
                        renderTex(textureatlassprite, pPoseStack, pBufferSource);
                    }
                }
            } else if (blockstate != null) {
                renderTex(pMinecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(blockstate), pPoseStack, pBufferSource);
            }
        }

        if (!pMinecraft.player.isSpectator()) {
            if (pMinecraft.player.isEyeInFluid(FluidTags.WATER)) {
                if (!Reflector.ForgeHooksClient_renderWaterOverlay.callBoolean(player, pPoseStack)) {
                    renderWater(pMinecraft, pPoseStack, pBufferSource);
                }
            } else if (Reflector.IForgeEntity_getEyeInFluidType.exists()) {
                FluidType fluidtype = (FluidType)Reflector.call(player, Reflector.IForgeEntity_getEyeInFluidType);
                if (!fluidtype.isAir()) {
                    IClientFluidTypeExtensions.of(fluidtype).renderOverlay(pMinecraft, pPoseStack, pBufferSource);
                }
            }

            if (pMinecraft.player.isOnFire() && !Reflector.ForgeHooksClient_renderFireOverlay.callBoolean(player, pPoseStack)) {
                renderFire(pPoseStack, pBufferSource);
            }
        }
    }

    @Nullable
    private static BlockState getViewBlockingState(Player pPlayer) {
        Pair<BlockState, BlockPos> pair = getOverlayBlock(pPlayer);
        return pair == null ? null : pair.getLeft();
    }

    private static Pair<BlockState, BlockPos> getOverlayBlock(Player playerIn) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 8; i++) {
            double d0 = playerIn.getX() + (double)(((float)((i >> 0) % 2) - 0.5F) * playerIn.getBbWidth() * 0.8F);
            double d1 = playerIn.getEyeY() + (double)(((float)((i >> 1) % 2) - 0.5F) * 0.1F * playerIn.getScale());
            double d2 = playerIn.getZ() + (double)(((float)((i >> 2) % 2) - 0.5F) * playerIn.getBbWidth() * 0.8F);
            blockpos$mutableblockpos.set(d0, d1, d2);
            BlockState blockstate = playerIn.level().getBlockState(blockpos$mutableblockpos);
            if (blockstate.getRenderShape() != RenderShape.INVISIBLE && blockstate.isViewBlocking(playerIn.level(), blockpos$mutableblockpos)) {
                return Pair.of(blockstate, blockpos$mutableblockpos.immutable());
            }
        }

        return null;
    }

    private static void renderTex(TextureAtlasSprite pTexture, PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        if (SmartAnimations.isActive()) {
            SmartAnimations.spriteRendered(pTexture);
        }

        float f = 0.1F;
        int i = ARGB.colorFromFloat(1.0F, 0.1F, 0.1F, 0.1F);
        float f1 = -1.0F;
        float f2 = 1.0F;
        float f3 = -1.0F;
        float f4 = 1.0F;
        float f5 = -0.5F;
        float f6 = pTexture.getU0();
        float f7 = pTexture.getU1();
        float f8 = pTexture.getV0();
        float f9 = pTexture.getV1();
        Matrix4f matrix4f = pPoseStack.last().pose();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.blockScreenEffect(pTexture.atlasLocation()));
        vertexconsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(f7, f9).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(f6, f9).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(f6, f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(f7, f8).setColor(i);
    }

    private static void renderWater(Minecraft pMinecraft, PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        renderFluid(pMinecraft, pPoseStack, pBufferSource, UNDERWATER_LOCATION);
    }

    public static void renderFluid(Minecraft minecraftIn, PoseStack matrixStackIn, MultiBufferSource buffersIn, ResourceLocation textureIn) {
        if (!Config.isShaders() || Shaders.isUnderwaterOverlay()) {
            if (SmartAnimations.isActive()) {
                SmartAnimations.textureRendered(minecraftIn.getTextureManager().getTexture(UNDERWATER_LOCATION).getId());
            }

            BlockPos blockpos = BlockPos.containing(minecraftIn.player.getX(), minecraftIn.player.getEyeY(), minecraftIn.player.getZ());
            float f = LightTexture.getBrightness(minecraftIn.player.level().dimensionType(), minecraftIn.player.level().getMaxLocalRawBrightness(blockpos));
            int i = ARGB.colorFromFloat(0.1F, f, f, f);
            float f1 = 4.0F;
            float f2 = -1.0F;
            float f3 = 1.0F;
            float f4 = -1.0F;
            float f5 = 1.0F;
            float f6 = -0.5F;
            float f7 = -minecraftIn.player.getYRot() / 64.0F;
            float f8 = minecraftIn.player.getXRot() / 64.0F;
            Matrix4f matrix4f = matrixStackIn.last().pose();
            VertexConsumer vertexconsumer = buffersIn.getBuffer(RenderType.blockScreenEffect(textureIn));
            vertexconsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(4.0F + f7, 4.0F + f8).setColor(i);
            vertexconsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(0.0F + f7, 4.0F + f8).setColor(i);
            vertexconsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(0.0F + f7, 0.0F + f8).setColor(i);
            vertexconsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(4.0F + f7, 0.0F + f8).setColor(i);
        }
    }

    private static void renderFire(PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        TextureAtlasSprite textureatlassprite = ModelBakery.FIRE_1.sprite();
        if (SmartAnimations.isActive()) {
            SmartAnimations.spriteRendered(textureatlassprite);
        }

        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.fireScreenEffect(textureatlassprite.atlasLocation()));
        float f = textureatlassprite.getU0();
        float f1 = textureatlassprite.getU1();
        float f2 = (f + f1) / 2.0F;
        float f3 = textureatlassprite.getV0();
        float f4 = textureatlassprite.getV1();
        float f5 = (f3 + f4) / 2.0F;
        float f6 = textureatlassprite.uvShrinkRatio();
        float f7 = Mth.lerp(f6, f, f2);
        float f8 = Mth.lerp(f6, f1, f2);
        float f9 = Mth.lerp(f6, f3, f5);
        float f10 = Mth.lerp(f6, f4, f5);
        float f11 = 1.0F;

        for (int i = 0; i < 2; i++) {
            pPoseStack.pushPose();
            float f12 = -0.5F;
            float f13 = 0.5F;
            float f14 = -0.5F;
            float f15 = 0.5F;
            float f16 = -0.5F;
            pPoseStack.translate((float)(-(i * 2 - 1)) * 0.24F, -0.3F, 0.0F);
            pPoseStack.mulPose(Axis.YP.rotationDegrees((float)(i * 2 - 1) * 10.0F));
            Matrix4f matrix4f = pPoseStack.last().pose();
            vertexconsumer.addVertex(matrix4f, -0.5F, -0.5F, -0.5F).setUv(f8, f10).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, 0.5F, -0.5F, -0.5F).setUv(f7, f10).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, 0.5F, 0.5F, -0.5F).setUv(f7, f9).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, -0.5F, 0.5F, -0.5F).setUv(f8, f9).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            pPoseStack.popPose();
        }
    }
}