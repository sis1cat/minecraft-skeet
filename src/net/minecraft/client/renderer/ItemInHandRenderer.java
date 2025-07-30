package net.minecraft.client.renderer;

import com.darkmagician6.eventapi.EventManager;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.optifine.Config;
import net.optifine.CustomItems;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import org.joml.Matrix4f;
import sisicat.events.AttackAnimationEvent;

public class ItemInHandRenderer {
    private static final RenderType MAP_BACKGROUND = RenderType.entityCutout(ResourceLocation.withDefaultNamespace("textures/map/map_background.png"));
    private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderType.entityCutout(ResourceLocation.withDefaultNamespace("textures/map/map_background_checkerboard.png"));
    private static final float ITEM_SWING_X_POS_SCALE = -0.4F;
    private static final float ITEM_SWING_Y_POS_SCALE = 0.2F;
    private static final float ITEM_SWING_Z_POS_SCALE = -0.2F;
    private static final float ITEM_HEIGHT_SCALE = -0.6F;
    private static final float ITEM_POS_X = 0.56F;
    private static final float ITEM_POS_Y = -0.52F;
    private static final float ITEM_POS_Z = -0.72F;
    private static final float ITEM_PRESWING_ROT_Y = 45.0F;
    private static final float ITEM_SWING_X_ROT_AMOUNT = -80.0F;
    private static final float ITEM_SWING_Y_ROT_AMOUNT = -20.0F;
    private static final float ITEM_SWING_Z_ROT_AMOUNT = -20.0F;
    private static final float EAT_JIGGLE_X_ROT_AMOUNT = 10.0F;
    private static final float EAT_JIGGLE_Y_ROT_AMOUNT = 90.0F;
    private static final float EAT_JIGGLE_Z_ROT_AMOUNT = 30.0F;
    private static final float EAT_JIGGLE_X_POS_SCALE = 0.6F;
    private static final float EAT_JIGGLE_Y_POS_SCALE = -0.5F;
    private static final float EAT_JIGGLE_Z_POS_SCALE = 0.0F;
    private static final double EAT_JIGGLE_EXPONENT = 27.0;
    private static final float EAT_EXTRA_JIGGLE_CUTOFF = 0.8F;
    private static final float EAT_EXTRA_JIGGLE_SCALE = 0.1F;
    private static final float ARM_SWING_X_POS_SCALE = -0.3F;
    private static final float ARM_SWING_Y_POS_SCALE = 0.4F;
    private static final float ARM_SWING_Z_POS_SCALE = -0.4F;
    private static final float ARM_SWING_Y_ROT_AMOUNT = 70.0F;
    private static final float ARM_SWING_Z_ROT_AMOUNT = -20.0F;
    private static final float ARM_HEIGHT_SCALE = -0.6F;
    private static final float ARM_POS_SCALE = 0.8F;
    private static final float ARM_POS_X = 0.8F;
    private static final float ARM_POS_Y = -0.75F;
    private static final float ARM_POS_Z = -0.9F;
    private static final float ARM_PRESWING_ROT_Y = 45.0F;
    private static final float ARM_PREROTATION_X_OFFSET = -1.0F;
    private static final float ARM_PREROTATION_Y_OFFSET = 3.6F;
    private static final float ARM_PREROTATION_Z_OFFSET = 3.5F;
    private static final float ARM_POSTROTATION_X_OFFSET = 5.6F;
    private static final int ARM_ROT_X = 200;
    private static final int ARM_ROT_Y = -135;
    private static final int ARM_ROT_Z = 120;
    private static final float MAP_SWING_X_POS_SCALE = -0.4F;
    private static final float MAP_SWING_Z_POS_SCALE = -0.2F;
    private static final float MAP_HANDS_POS_X = 0.0F;
    private static final float MAP_HANDS_POS_Y = 0.04F;
    private static final float MAP_HANDS_POS_Z = -0.72F;
    private static final float MAP_HANDS_HEIGHT_SCALE = -1.2F;
    private static final float MAP_HANDS_TILT_SCALE = -0.5F;
    private static final float MAP_PLAYER_PITCH_SCALE = 45.0F;
    private static final float MAP_HANDS_Z_ROT_AMOUNT = -85.0F;
    private static final float MAPHAND_X_ROT_AMOUNT = 45.0F;
    private static final float MAPHAND_Y_ROT_AMOUNT = 92.0F;
    private static final float MAPHAND_Z_ROT_AMOUNT = -41.0F;
    private static final float MAP_HAND_X_POS = 0.3F;
    private static final float MAP_HAND_Y_POS = -1.1F;
    private static final float MAP_HAND_Z_POS = 0.45F;
    private static final float MAP_SWING_X_ROT_AMOUNT = 20.0F;
    private static final float MAP_PRE_ROT_SCALE = 0.38F;
    private static final float MAP_GLOBAL_X_POS = -0.5F;
    private static final float MAP_GLOBAL_Y_POS = -0.5F;
    private static final float MAP_GLOBAL_Z_POS = 0.0F;
    private static final float MAP_FINAL_SCALE = 0.0078125F;
    private static final int MAP_BORDER = 7;
    private static final int MAP_HEIGHT = 128;
    private static final int MAP_WIDTH = 128;
    private static final float BOW_CHARGE_X_POS_SCALE = 0.0F;
    private static final float BOW_CHARGE_Y_POS_SCALE = 0.0F;
    private static final float BOW_CHARGE_Z_POS_SCALE = 0.04F;
    private static final float BOW_CHARGE_SHAKE_X_SCALE = 0.0F;
    private static final float BOW_CHARGE_SHAKE_Y_SCALE = 0.004F;
    private static final float BOW_CHARGE_SHAKE_Z_SCALE = 0.0F;
    private static final float BOW_CHARGE_Z_SCALE = 0.2F;
    private static final float BOW_MIN_SHAKE_CHARGE = 0.1F;
    private final Minecraft minecraft;
    private final MapRenderState mapRenderState = new MapRenderState();
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;
    private float mainHandHeight;
    private float oMainHandHeight;
    private float offHandHeight;
    private float oOffHandHeight;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final ItemRenderer itemRenderer;
    private final ItemModelResolver itemModelResolver;
    private static boolean renderItemHand = false;

    public ItemInHandRenderer(Minecraft pMinecraft, EntityRenderDispatcher pEntityRenderDispatcher, ItemRenderer pItemRenderer, ItemModelResolver pItemModelResolver) {
        this.minecraft = pMinecraft;
        this.entityRenderDispatcher = pEntityRenderDispatcher;
        this.itemRenderer = pItemRenderer;
        this.itemModelResolver = pItemModelResolver;
    }

    public void renderItem(
        LivingEntity pEntity,
        ItemStack pItemStack,
        ItemDisplayContext pDisplayContext,
        boolean pLeftHand,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pSeed
    ) {
        boolean flag = pEntity.getMainArm() == HumanoidArm.LEFT ? !pLeftHand : pLeftHand;
        CustomItems.setRenderOffHand(flag);
        renderItemHand = true;
        if (!pItemStack.isEmpty()) {
            this.itemRenderer
                .renderStatic(
                    pEntity,
                    pItemStack,
                    pDisplayContext,
                    pLeftHand,
                    pPoseStack,
                    pBuffer,
                    pEntity.level(),
                    pSeed,
                    OverlayTexture.NO_OVERLAY,
                    pEntity.getId() + pDisplayContext.ordinal()
                );
        }

        renderItemHand = false;
        CustomItems.setRenderOffHand(false);
    }

    private float calculateMapTilt(float pPitch) {
        float f = 1.0F - pPitch / 45.0F + 0.1F;
        f = Mth.clamp(f, 0.0F, 1.0F);
        return -Mth.cos(f * (float) Math.PI) * 0.5F + 0.5F;
    }

    private void renderMapHand(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, HumanoidArm pSide) {
        PlayerRenderer playerrenderer = (PlayerRenderer)this.entityRenderDispatcher.<AbstractClientPlayer>getRenderer(this.minecraft.player);
        pPoseStack.pushPose();
        float f = pSide == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        pPoseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        pPoseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * -41.0F));
        pPoseStack.translate(f * 0.3F, -1.1F, 0.45F);
        ResourceLocation resourcelocation = this.minecraft.player.getSkin().texture();
        if (pSide == HumanoidArm.RIGHT) {
            playerrenderer.renderRightHand(pPoseStack, pBufferSource, pPackedLight, resourcelocation, this.minecraft.player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
        } else {
            playerrenderer.renderLeftHand(pPoseStack, pBufferSource, pPackedLight, resourcelocation, this.minecraft.player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
        }

        pPoseStack.popPose();
    }

    private void renderOneHandedMap(
        PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, float pEquippedProgress, HumanoidArm pHand, float pSwingProgress, ItemStack pStack
    ) {
        float f = pHand == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        pPoseStack.translate(f * 0.125F, -0.125F, 0.0F);
        if (!this.minecraft.player.isInvisible()) {
            pPoseStack.pushPose();
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * 10.0F));
            this.renderPlayerArm(pPoseStack, pBuffer, pPackedLight, pEquippedProgress, pSwingProgress, pHand);
            pPoseStack.popPose();
        }

        pPoseStack.pushPose();
        pPoseStack.translate(f * 0.51F, -0.08F + pEquippedProgress * -1.2F, -0.75F);
        float f1 = Mth.sqrt(pSwingProgress);
        float f2 = Mth.sin(f1 * (float) Math.PI);
        float f3 = -0.5F * f2;
        float f4 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f5 = -0.3F * Mth.sin(pSwingProgress * (float) Math.PI);
        pPoseStack.translate(f * f3, f4 - 0.3F * f2, f5);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(f2 * -45.0F));
        pPoseStack.mulPose(Axis.YP.rotationDegrees(f * f2 * -30.0F));
        this.renderMap(pPoseStack, pBuffer, pPackedLight, pStack);
        pPoseStack.popPose();
    }

    private void renderTwoHandedMap(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, float pPitch, float pEquippedProgress, float pSwingProgress) {
        float f = Mth.sqrt(pSwingProgress);
        float f1 = -0.2F * Mth.sin(pSwingProgress * (float) Math.PI);
        float f2 = -0.4F * Mth.sin(f * (float) Math.PI);
        pPoseStack.translate(0.0F, -f1 / 2.0F, f2);
        float f3 = this.calculateMapTilt(pPitch);
        pPoseStack.translate(0.0F, 0.04F + pEquippedProgress * -1.2F + f3 * -0.5F, -0.72F);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(f3 * -85.0F));
        if (!this.minecraft.player.isInvisible()) {
            pPoseStack.pushPose();
            pPoseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            this.renderMapHand(pPoseStack, pBuffer, pPackedLight, HumanoidArm.RIGHT);
            this.renderMapHand(pPoseStack, pBuffer, pPackedLight, HumanoidArm.LEFT);
            pPoseStack.popPose();
        }

        float f4 = Mth.sin(f * (float) Math.PI);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(f4 * 20.0F));
        pPoseStack.scale(2.0F, 2.0F, 2.0F);
        this.renderMap(pPoseStack, pBuffer, pPackedLight, this.mainHandItem);
    }

    private void renderMap(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, ItemStack pStack) {
        pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pPoseStack.scale(0.38F, 0.38F, 0.38F);
        pPoseStack.translate(-0.5F, -0.5F, 0.0F);
        pPoseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
        MapId mapid = pStack.get(DataComponents.MAP_ID);
        MapItemSavedData mapitemsaveddata = MapItem.getSavedData(mapid, this.minecraft.level);
        VertexConsumer vertexconsumer = pBuffer.getBuffer(mapitemsaveddata == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD);
        Matrix4f matrix4f = pPoseStack.last().pose();
        vertexconsumer.addVertex(matrix4f, -7.0F, 135.0F, 0.0F)
            .setColor(-1)
            .setUv(0.0F, 1.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal(0.0F, 1.0F, 0.0F);
        vertexconsumer.addVertex(matrix4f, 135.0F, 135.0F, 0.0F)
            .setColor(-1)
            .setUv(1.0F, 1.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal(0.0F, 1.0F, 0.0F);
        vertexconsumer.addVertex(matrix4f, 135.0F, -7.0F, 0.0F)
            .setColor(-1)
            .setUv(1.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal(0.0F, 1.0F, 0.0F);
        vertexconsumer.addVertex(matrix4f, -7.0F, -7.0F, 0.0F)
            .setColor(-1)
            .setUv(0.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal(0.0F, 1.0F, 0.0F);
        if (mapitemsaveddata != null) {
            MapRenderer maprenderer = this.minecraft.getMapRenderer();
            maprenderer.extractRenderState(mapid, mapitemsaveddata, this.mapRenderState);
            maprenderer.render(this.mapRenderState, pPoseStack, pBuffer, false, pPackedLight);
        }
    }

    private void renderPlayerArm(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, float pEquippedProgress, float pSwingProgress, HumanoidArm pSide) {
        boolean flag = pSide != HumanoidArm.LEFT;
        float f = flag ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(pSwingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(pSwingProgress * (float) Math.PI);
        pPoseStack.translate(f * (f2 + 0.64000005F), f3 + -0.6F + pEquippedProgress * -0.6F, f4 + -0.71999997F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        float f5 = Mth.sin(pSwingProgress * pSwingProgress * (float) Math.PI);
        float f6 = Mth.sin(f1 * (float) Math.PI);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));
        AbstractClientPlayer abstractclientplayer = this.minecraft.player;
        pPoseStack.translate(f * -1.0F, 3.6F, 3.5F);
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        pPoseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        pPoseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        pPoseStack.translate(f * 5.6F, 0.0F, 0.0F);
        PlayerRenderer playerrenderer = (PlayerRenderer)this.entityRenderDispatcher.<AbstractClientPlayer>getRenderer(abstractclientplayer);
        ResourceLocation resourcelocation = abstractclientplayer.getSkin().texture();
        if (flag) {
            playerrenderer.renderRightHand(pPoseStack, pBuffer, pPackedLight, resourcelocation, abstractclientplayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
        } else {
            playerrenderer.renderLeftHand(pPoseStack, pBuffer, pPackedLight, resourcelocation, abstractclientplayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
        }
    }

    private void applyEatTransform(PoseStack pPoseStack, float pPartialTick, HumanoidArm pArm, ItemStack pStack, Player pPlayer) {
        float f = (float)pPlayer.getUseItemRemainingTicks() - pPartialTick + 1.0F;
        float f1 = f / (float)pStack.getUseDuration(pPlayer);
        if (f1 < 0.8F) {
            float f2 = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            pPoseStack.translate(0.0F, f2, 0.0F);
        }

        float f3 = 1.0F - (float)Math.pow((double)f1, 27.0);
        int i = pArm == HumanoidArm.RIGHT ? 1 : -1;
        pPoseStack.translate(f3 * 0.6F * (float)i, f3 * -0.5F, f3 * 0.0F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees((float)i * f3 * 90.0F));
        pPoseStack.mulPose(Axis.XP.rotationDegrees(f3 * 10.0F));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees((float)i * f3 * 30.0F));
    }

    private void applyBrushTransform(PoseStack pPoseStack, float pPartialTick, HumanoidArm pArm, ItemStack pStack, Player pPlayer, float pEquippedProgress) {
        this.applyItemArmTransform(pPoseStack, pArm, pEquippedProgress);
        float f = (float)(pPlayer.getUseItemRemainingTicks() % 10);
        float f1 = f - pPartialTick + 1.0F;
        float f2 = 1.0F - f1 / 10.0F;
        float f3 = -90.0F;
        float f4 = 60.0F;
        float f5 = 150.0F;
        float f6 = -15.0F;
        int i = 2;
        float f7 = -15.0F + 75.0F * Mth.cos(f2 * 2.0F * (float) Math.PI);
        if (pArm != HumanoidArm.RIGHT) {
            pPoseStack.translate(0.1, 0.83, 0.35);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pPoseStack.mulPose(Axis.XP.rotationDegrees(f7));
            pPoseStack.translate(-0.3, 0.22, 0.35);
        } else {
            pPoseStack.translate(-0.25, 0.22, 0.35);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
            pPoseStack.mulPose(Axis.XP.rotationDegrees(f7));
        }
    }

    private void applyItemArmAttackTransform(PoseStack pPoseStack, HumanoidArm pHand, float pSwingProgress) {

        AttackAnimationEvent attackAnimationEvent = new AttackAnimationEvent(
                -80F, new Vec3(1, 1, 1), new Vec3(45F, 0, 0), new Vec3(0, 0, 0), 0
        );
        if(pHand == HumanoidArm.RIGHT)
            EventManager.call(attackAnimationEvent);

        int i = pHand == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(pSwingProgress * pSwingProgress * (float) Math.PI);
        pPoseStack.mulPose(Axis.YP.rotationDegrees((float)i * ((float) attackAnimationEvent.staticXRots.x + f * -20.0F * (float) attackAnimationEvent.animationXRots.x)));
        float f1 = Mth.sin(Mth.sqrt(pSwingProgress) * (float) Math.PI);
        pPoseStack.mulPose(Axis.ZP.rotationDegrees((float)i * f1 * -20.0F * (float) attackAnimationEvent.animationXRots.y + (float) attackAnimationEvent.staticXRots.y));
        pPoseStack.mulPose(Axis.XP.rotationDegrees(f1 * attackAnimationEvent.amplitude + (float) attackAnimationEvent.staticXRots.z));
        pPoseStack.mulPose(Axis.YP.rotationDegrees((float)i * -45.0F * (float) attackAnimationEvent.animationXRots.z));
    }

    private void applyItemArmTransform(PoseStack pPoseStack, HumanoidArm pHand, float pEquippedProg) {
        int i = pHand == HumanoidArm.RIGHT ? 1 : -1;
        pPoseStack.translate((float)i * 0.56F, -0.52F + pEquippedProg * -0.6F, -0.72F);
    }

    public void renderHandsWithItems(float pPartialTicks, PoseStack pPoseStack, MultiBufferSource.BufferSource pBuffer, LocalPlayer pPlayerEntity, int pCombinedLight) {
        float f = pPlayerEntity.getAttackAnim(pPartialTicks);
        InteractionHand interactionhand = MoreObjects.firstNonNull(pPlayerEntity.swingingArm, InteractionHand.MAIN_HAND);
        float f1 = pPlayerEntity.getXRot(pPartialTicks);
        ItemInHandRenderer.HandRenderSelection iteminhandrenderer$handrenderselection = evaluateWhichHandsToRender(pPlayerEntity);
        float f2 = Mth.lerp(pPartialTicks, pPlayerEntity.xBobO, pPlayerEntity.xBob);
        float f3 = Mth.lerp(pPartialTicks, pPlayerEntity.yBobO, pPlayerEntity.yBob);
        pPoseStack.mulPose(Axis.XP.rotationDegrees((pPlayerEntity.getViewXRot(pPartialTicks) - f2) * 0.1F));
        pPoseStack.mulPose(Axis.YP.rotationDegrees((pPlayerEntity.getViewYRot(pPartialTicks) - f3) * 0.1F));
        if (iteminhandrenderer$handrenderselection.renderMainHand) {
            float f4 = interactionhand == InteractionHand.MAIN_HAND ? f : 0.0F;
            float f5 = 1.0F - Mth.lerp(pPartialTicks, this.oMainHandHeight, this.mainHandHeight);
            if (!Reflector.ForgeHooksClient_renderSpecificFirstPersonHand.exists()
                || !Reflector.callBoolean(
                    Reflector.ForgeHooksClient_renderSpecificFirstPersonHand,
                    InteractionHand.MAIN_HAND,
                    pPoseStack,
                    pBuffer,
                    pCombinedLight,
                    pPartialTicks,
                    f1,
                    f4,
                    f5,
                    this.mainHandItem
                )) {
                this.renderArmWithItem(pPlayerEntity, pPartialTicks, f1, InteractionHand.MAIN_HAND, f4, this.mainHandItem, f5, pPoseStack, pBuffer, pCombinedLight);
            }
        }

        if (iteminhandrenderer$handrenderselection.renderOffHand) {
            float f6 = interactionhand == InteractionHand.OFF_HAND ? f : 0.0F;
            float f7 = 1.0F - Mth.lerp(pPartialTicks, this.oOffHandHeight, this.offHandHeight);
            if (!Reflector.ForgeHooksClient_renderSpecificFirstPersonHand.exists()
                || !Reflector.callBoolean(
                    Reflector.ForgeHooksClient_renderSpecificFirstPersonHand,
                    InteractionHand.OFF_HAND,
                    pPoseStack,
                    pBuffer,
                    pCombinedLight,
                    pPartialTicks,
                    f1,
                    f6,
                    f7,
                    this.offHandItem
                )) {
                this.renderArmWithItem(pPlayerEntity, pPartialTicks, f1, InteractionHand.OFF_HAND, f6, this.offHandItem, f7, pPoseStack, pBuffer, pCombinedLight);
            }
        }

        pBuffer.endBatch();
    }

    @VisibleForTesting
    static ItemInHandRenderer.HandRenderSelection evaluateWhichHandsToRender(LocalPlayer pPlayer) {
        ItemStack itemstack = pPlayer.getMainHandItem();
        ItemStack itemstack1 = pPlayer.getOffhandItem();
        boolean flag = itemstack.is(Items.BOW) || itemstack1.is(Items.BOW);
        boolean flag1 = itemstack.is(Items.CROSSBOW) || itemstack1.is(Items.CROSSBOW);
        if (!flag && !flag1) {
            return ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
        } else if (pPlayer.isUsingItem()) {
            return selectionUsingItemWhileHoldingBowLike(pPlayer);
        } else {
            return isChargedCrossbow(itemstack)
                ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
                : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
        }
    }

    private static ItemInHandRenderer.HandRenderSelection selectionUsingItemWhileHoldingBowLike(LocalPlayer pPlayer) {
        ItemStack itemstack = pPlayer.getUseItem();
        InteractionHand interactionhand = pPlayer.getUsedItemHand();
        if (!itemstack.is(Items.BOW) && !itemstack.is(Items.CROSSBOW)) {
            return interactionhand == InteractionHand.MAIN_HAND && isChargedCrossbow(pPlayer.getOffhandItem())
                ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
                : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
        } else {
            return ItemInHandRenderer.HandRenderSelection.onlyForHand(interactionhand);
        }
    }

    private static boolean isChargedCrossbow(ItemStack pStack) {
        return pStack.is(Items.CROSSBOW) && CrossbowItem.isCharged(pStack);
    }

    private void renderArmWithItem(
        AbstractClientPlayer pPlayer,
        float pPartialTicks,
        float pPitch,
        InteractionHand pHand,
        float pSwingProgress,
        ItemStack pStack,
        float pEquippedProgress,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pCombinedLight
    ) {
        if (!Config.isShaders() || !Shaders.isSkipRenderHand(pHand)) {
            if (!pPlayer.isScoping()) {
                boolean flag = pHand == InteractionHand.MAIN_HAND;
                HumanoidArm humanoidarm = flag ? pPlayer.getMainArm() : pPlayer.getMainArm().getOpposite();
                pPoseStack.pushPose();
                if (pStack.isEmpty()) {
                    if (flag && !pPlayer.isInvisible()) {
                        this.renderPlayerArm(pPoseStack, pBuffer, pCombinedLight, pEquippedProgress, pSwingProgress, humanoidarm);
                    }
                } else if (pStack.has(DataComponents.MAP_ID)) {
                    if (flag && this.offHandItem.isEmpty()) {
                        this.renderTwoHandedMap(pPoseStack, pBuffer, pCombinedLight, pPitch, pEquippedProgress, pSwingProgress);
                    } else {
                        this.renderOneHandedMap(pPoseStack, pBuffer, pCombinedLight, pEquippedProgress, humanoidarm, pSwingProgress, pStack);
                    }
                } else if (pStack.getItem() instanceof CrossbowItem) {
                    boolean flag2 = CrossbowItem.isCharged(pStack);
                    boolean flag3 = humanoidarm == HumanoidArm.RIGHT;
                    int j = flag3 ? 1 : -1;
                    if (pPlayer.isUsingItem() && pPlayer.getUseItemRemainingTicks() > 0 && pPlayer.getUsedItemHand() == pHand) {
                        this.applyItemArmTransform(pPoseStack, humanoidarm, pEquippedProgress);
                        pPoseStack.translate((float)j * -0.4785682F, -0.094387F, 0.05731531F);
                        pPoseStack.mulPose(Axis.XP.rotationDegrees(-11.935F));
                        pPoseStack.mulPose(Axis.YP.rotationDegrees((float)j * 65.3F));
                        pPoseStack.mulPose(Axis.ZP.rotationDegrees((float)j * -9.785F));
                        float f7 = (float)pStack.getUseDuration(pPlayer) - ((float)pPlayer.getUseItemRemainingTicks() - pPartialTicks + 1.0F);
                        float f9 = f7 / (float)CrossbowItem.getChargeDuration(pStack, pPlayer);
                        if (f9 > 1.0F) {
                            f9 = 1.0F;
                        }

                        if (f9 > 0.1F) {
                            float f11 = Mth.sin((f7 - 0.1F) * 1.3F);
                            float f13 = f9 - 0.1F;
                            float f14 = f11 * f13;
                            pPoseStack.translate(f14 * 0.0F, f14 * 0.004F, f14 * 0.0F);
                        }

                        pPoseStack.translate(f9 * 0.0F, f9 * 0.0F, f9 * 0.04F);
                        pPoseStack.scale(1.0F, 1.0F, 1.0F + f9 * 0.2F);
                        pPoseStack.mulPose(Axis.YN.rotationDegrees((float)j * 45.0F));
                    } else {
                        this.swingArm(pSwingProgress, pEquippedProgress, pPoseStack, j, humanoidarm);
                        if (flag2 && pSwingProgress < 0.001F && flag) {
                            pPoseStack.translate((float)j * -0.641864F, 0.0F, 0.0F);
                            pPoseStack.mulPose(Axis.YP.rotationDegrees((float)j * 10.0F));
                        }
                    }

                    this.renderItem(
                        pPlayer,
                        pStack,
                        flag3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                        !flag3,
                        pPoseStack,
                        pBuffer,
                        pCombinedLight
                    );
                } else {
                    boolean flag1 = humanoidarm == HumanoidArm.RIGHT;
                    int i = flag1 ? 1 : -1;
                    if (!IClientItemExtensions.of(pStack)
                        .applyForgeHandTransform(pPoseStack, this.minecraft.player, humanoidarm, pStack, pPartialTicks, pEquippedProgress, pSwingProgress)) {
                        if (pPlayer.isUsingItem() && pPlayer.getUseItemRemainingTicks() > 0 && pPlayer.getUsedItemHand() == pHand) {
                            switch (pStack.getUseAnimation()) {
                                case NONE:
                                    this.applyItemArmTransform(pPoseStack, humanoidarm, pEquippedProgress);
                                    break;
                                case EAT:
                                case DRINK:
                                    this.applyEatTransform(pPoseStack, pPartialTicks, humanoidarm, pStack, pPlayer);
                                    this.applyItemArmTransform(pPoseStack, humanoidarm, pEquippedProgress);
                                    break;
                                case BLOCK:
                                    this.applyItemArmTransform(pPoseStack, humanoidarm, pEquippedProgress);
                                    if (!(pStack.getItem() instanceof ShieldItem)) {
                                        pPoseStack.translate((float)i * -0.14142136F, 0.08F, 0.14142136F);
                                        pPoseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
                                        pPoseStack.mulPose(Axis.YP.rotationDegrees((float)i * 13.365F));
                                        pPoseStack.mulPose(Axis.ZP.rotationDegrees((float)i * 78.05F));
                                    }
                                    break;
                                case BOW:
                                    this.applyItemArmTransform(pPoseStack, humanoidarm, pEquippedProgress);
                                    pPoseStack.translate((float)i * -0.2785682F, 0.18344387F, 0.15731531F);
                                    pPoseStack.mulPose(Axis.XP.rotationDegrees(-13.935F));
                                    pPoseStack.mulPose(Axis.YP.rotationDegrees((float)i * 35.3F));
                                    pPoseStack.mulPose(Axis.ZP.rotationDegrees((float)i * -9.785F));
                                    float f = (float)pStack.getUseDuration(pPlayer) - ((float)pPlayer.getUseItemRemainingTicks() - pPartialTicks + 1.0F);
                                    float f1 = f / 20.0F;
                                    f1 = (f1 * f1 + f1 * 2.0F) / 3.0F;
                                    if (f1 > 1.0F) {
                                        f1 = 1.0F;
                                    }

                                    if (f1 > 0.1F) {
                                        float f8 = Mth.sin((f - 0.1F) * 1.3F);
                                        float f10 = f1 - 0.1F;
                                        float f12 = f8 * f10;
                                        pPoseStack.translate(f12 * 0.0F, f12 * 0.004F, f12 * 0.0F);
                                    }

                                    pPoseStack.translate(f1 * 0.0F, f1 * 0.0F, f1 * 0.04F);
                                    pPoseStack.scale(1.0F, 1.0F, 1.0F + f1 * 0.2F);
                                    pPoseStack.mulPose(Axis.YN.rotationDegrees((float)i * 45.0F));
                                    break;
                                case SPEAR:
                                    this.applyItemArmTransform(pPoseStack, humanoidarm, pEquippedProgress);
                                    pPoseStack.translate((float)i * -0.5F, 0.7F, 0.1F);
                                    pPoseStack.mulPose(Axis.XP.rotationDegrees(-55.0F));
                                    pPoseStack.mulPose(Axis.YP.rotationDegrees((float)i * 35.3F));
                                    pPoseStack.mulPose(Axis.ZP.rotationDegrees((float)i * -9.785F));
                                    float f2 = (float)pStack.getUseDuration(pPlayer) - ((float)pPlayer.getUseItemRemainingTicks() - pPartialTicks + 1.0F);
                                    float f3 = f2 / 10.0F;
                                    if (f3 > 1.0F) {
                                        f3 = 1.0F;
                                    }

                                    if (f3 > 0.1F) {
                                        float f4 = Mth.sin((f2 - 0.1F) * 1.3F);
                                        float f5 = f3 - 0.1F;
                                        float f6 = f4 * f5;
                                        pPoseStack.translate(f6 * 0.0F, f6 * 0.004F, f6 * 0.0F);
                                    }

                                    pPoseStack.translate(0.0F, 0.0F, f3 * 0.2F);
                                    pPoseStack.scale(1.0F, 1.0F, 1.0F + f3 * 0.2F);
                                    pPoseStack.mulPose(Axis.YN.rotationDegrees((float)i * 45.0F));
                                    break;
                                case BRUSH:
                                    this.applyBrushTransform(pPoseStack, pPartialTicks, humanoidarm, pStack, pPlayer, pEquippedProgress);
                                    break;
                                case BUNDLE:
                                    this.swingArm(pSwingProgress, pEquippedProgress, pPoseStack, i, humanoidarm);
                            }
                        } else if (pPlayer.isAutoSpinAttack()) {
                            this.applyItemArmTransform(pPoseStack, humanoidarm, pEquippedProgress);
                            pPoseStack.translate((float)i * -0.4F, 0.8F, 0.3F);
                            pPoseStack.mulPose(Axis.YP.rotationDegrees((float)i * 65.0F));
                            pPoseStack.mulPose(Axis.ZP.rotationDegrees((float)i * -85.0F));
                        } else {
                            this.swingArm(pSwingProgress, pEquippedProgress, pPoseStack, i, humanoidarm);
                        }
                    }

                    this.renderItem(
                        pPlayer,
                        pStack,
                        flag1 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                        !flag1,
                        pPoseStack,
                        pBuffer,
                        pCombinedLight
                    );
                }

                pPoseStack.popPose();
            }
        }
    }

    private void swingArm(float pSwingProgress, float pEquippedProgress, PoseStack pPoseStack, int pDirection, HumanoidArm pArm) {

        AttackAnimationEvent attackAnimationEvent = new AttackAnimationEvent(
                0, new Vec3(0, 0, 0), new Vec3(0, 0, 0),
                new Vec3(
                        -0.4F * Mth.sin(Mth.sqrt(pSwingProgress) * (float)Math.PI),
                        0.2F * Mth.sin(Mth.sqrt(pSwingProgress) * ((float)Math.PI * 2F)),
                        -0.2F * Mth.sin(pSwingProgress * (float)Math.PI)
                ), 1
        );
        if(pArm == HumanoidArm.RIGHT)
            EventManager.call(attackAnimationEvent);
        float f = (float) attackAnimationEvent.viewModel.x;
        float f1 = (float) attackAnimationEvent.viewModel.y;
        float f2 = (float) attackAnimationEvent.viewModel.z;
        pPoseStack.translate((float)pDirection * f, f1, f2);
        this.applyItemArmTransform(pPoseStack, pArm, pEquippedProgress * attackAnimationEvent.strength);
        this.applyItemArmAttackTransform(pPoseStack, pArm, pSwingProgress);
    }

    private boolean shouldInstantlyReplaceVisibleItem(ItemStack pOldItem, ItemStack pNewItem) {
        return ItemStack.matches(pOldItem, pNewItem) ? true : !this.itemModelResolver.shouldPlaySwapAnimation(pNewItem);
    }

    public void tick() {
        this.oMainHandHeight = this.mainHandHeight;
        this.oOffHandHeight = this.offHandHeight;
        LocalPlayer localplayer = this.minecraft.player;
        ItemStack itemstack = localplayer.getMainHandItem();
        ItemStack itemstack1 = localplayer.getOffhandItem();
        if (this.shouldInstantlyReplaceVisibleItem(this.mainHandItem, itemstack)) {
            this.mainHandItem = itemstack;
        }

        if (this.shouldInstantlyReplaceVisibleItem(this.offHandItem, itemstack1)) {
            this.offHandItem = itemstack1;
        }

        if (localplayer.isHandsBusy()) {
            this.mainHandHeight = Mth.clamp(this.mainHandHeight - 0.4F, 0.0F, 1.0F);
            this.offHandHeight = Mth.clamp(this.offHandHeight - 0.4F, 0.0F, 1.0F);
        } else {
            float f = localplayer.getAttackStrengthScale(1.0F);
            float f1 = this.mainHandItem != itemstack ? 0.0F : f * f * f;
            float f2 = this.offHandItem != itemstack1 ? 0.0F : 1.0F;
            if (Reflector.ForgeHooksClient_shouldCauseReequipAnimation.exists()) {
                boolean flag = Reflector.callBoolean(
                    Reflector.ForgeHooksClient_shouldCauseReequipAnimation, this.mainHandItem, itemstack, localplayer.getInventory().selected
                );
                boolean flag1 = Reflector.callBoolean(Reflector.ForgeHooksClient_shouldCauseReequipAnimation, this.offHandItem, itemstack1, -1);
                if (!flag && !Objects.equals(this.mainHandItem, itemstack)) {
                    this.mainHandItem = itemstack;
                }

                if (!flag1 && !Objects.equals(this.offHandItem, itemstack1)) {
                    this.offHandItem = itemstack1;
                }

                f1 = flag ? 0.0F : f * f * f;
                f2 = flag1 ? 0.0F : 1.0F;
            }

            this.mainHandHeight = this.mainHandHeight + Mth.clamp(f1 - this.mainHandHeight, -0.4F, 0.4F);
            this.offHandHeight = this.offHandHeight + Mth.clamp(f2 - this.offHandHeight, -0.4F, 0.4F);
        }

        if (this.mainHandHeight < 0.1F) {
            this.mainHandItem = itemstack;
            if (Config.isShaders()) {
                Shaders.setItemToRenderMain(this.mainHandItem);
            }
        }

        if (this.offHandHeight < 0.1F) {
            this.offHandItem = itemstack1;
            if (Config.isShaders()) {
                Shaders.setItemToRenderOff(this.offHandItem);
            }
        }
    }

    public void itemUsed(InteractionHand pHand) {
        if (pHand == InteractionHand.MAIN_HAND) {
            this.mainHandHeight = 0.0F;
        } else {
            this.offHandHeight = 0.0F;
        }
    }

    public static boolean isRenderItemHand() {
        return renderItemHand;
    }

    @VisibleForTesting
    static enum HandRenderSelection {
        RENDER_BOTH_HANDS(true, true),
        RENDER_MAIN_HAND_ONLY(true, false),
        RENDER_OFF_HAND_ONLY(false, true);

        final boolean renderMainHand;
        final boolean renderOffHand;

        private HandRenderSelection(final boolean pRenderMainHand, final boolean pRenderOffHand) {
            this.renderMainHand = pRenderMainHand;
            this.renderOffHand = pRenderOffHand;
        }

        public static ItemInHandRenderer.HandRenderSelection onlyForHand(InteractionHand pHand) {
            return pHand == InteractionHand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
        }
    }
}