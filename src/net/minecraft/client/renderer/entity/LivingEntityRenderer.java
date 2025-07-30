package net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Team;
import net.optifine.Config;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;

public abstract class LivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
    extends EntityRenderer<T, S>
    implements RenderLayerParent<S, M> {
    private static final float EYE_BED_OFFSET = 0.1F;
    public M model;
    protected final ItemModelResolver itemModelResolver;
    protected final List<RenderLayer<S, M>> layers = Lists.newArrayList();
    public float renderLimbSwing;
    public float renderLimbSwingAmount;
    public float renderAgeInTicks;
    public float renderHeadYaw;
    public float renderHeadPitch;
    public static final boolean animateModelLiving = Boolean.getBoolean("animate.model.living");
    private static boolean renderItemHead = false;

    public LivingEntityRenderer(EntityRendererProvider.Context pContext, M pModel, float pShadowRadius) {
        super(pContext);
        this.itemModelResolver = pContext.getItemModelResolver();
        this.model = pModel;
        this.shadowRadius = pShadowRadius;
    }

    public final boolean addLayer(RenderLayer<S, M> pLayer) {
        return this.layers.add(pLayer);
    }

    @Override
    public M getModel() {
        return this.model;
    }

    protected AABB getBoundingBoxForCulling(T p_361472_) {
        AABB aabb = super.getBoundingBoxForCulling(p_361472_);
        if (p_361472_.getItemBySlot(EquipmentSlot.HEAD).is(Items.DRAGON_HEAD)) {
            float f = 0.5F;
            return aabb.inflate(0.5, 0.5, 0.5);
        } else {
            return aabb;
        }
    }

    public void render(S p_364280_, PoseStack p_115311_, MultiBufferSource p_115312_, int p_115313_) {
        if (!Reflector.ForgeEventFactoryClient_onRenderLivingPre.exists()
            || !Reflector.ForgeEventFactoryClient_onRenderLivingPre.callBoolean(p_364280_, this, p_115311_, p_115312_, p_115313_)) {
            if (animateModelLiving) {
                p_364280_.walkAnimationSpeed = 1.5F;
            }

            p_115311_.pushPose();
            if (p_364280_.hasPose(Pose.SLEEPING)) {
                Direction direction = p_364280_.bedOrientation;
                if (direction != null) {
                    float f = p_364280_.eyeHeight - 0.1F;
                    p_115311_.translate((float)(-direction.getStepX()) * f, 0.0F, (float)(-direction.getStepZ()) * f);
                }
            }

            float f2 = p_364280_.scale;
            p_115311_.scale(f2, f2, f2);
            this.setupRotations(p_364280_, p_115311_, p_364280_.bodyRot, f2);
            p_115311_.scale(-1.0F, -1.0F, 1.0F);
            this.scale(p_364280_, p_115311_);
            p_115311_.translate(0.0F, -1.501F, 0.0F);
            this.model.setupAnim(p_364280_);
            if (CustomEntityModels.isActive()) {
                this.renderLimbSwing = p_364280_.walkAnimationPos;
                this.renderLimbSwingAmount = p_364280_.walkAnimationSpeed;
                this.renderAgeInTicks = p_364280_.ageInTicks;
                this.renderHeadYaw = p_364280_.yRot;
                this.renderHeadPitch = p_364280_.xRot;
            }

            boolean flag2 = Config.isShaders();
            boolean flag = this.isBodyVisible(p_364280_);
            boolean flag1 = !flag && !p_364280_.isInvisibleToPlayer;
            RenderType rendertype = this.getRenderType(p_364280_, flag, flag1, p_364280_.appearsGlowing);
            if (rendertype != null) {
                VertexConsumer vertexconsumer = p_115312_.getBuffer(rendertype);
                float f1 = this.getWhiteOverlayProgress(p_364280_);
                if (flag2) {
                    if (p_364280_.hasRedOverlay) {
                        Shaders.setEntityColor(1.0F, 0.0F, 0.0F, 0.3F);
                    }

                    if (f1 > 0.0F) {
                        Shaders.setEntityColor(f1, f1, f1, 0.5F);
                    }
                }

                int i = getOverlayCoords(p_364280_, f1);
                int j = flag1 ? 654311423 : -1;
                int k = ARGB.multiply(j, this.getModelTint(p_364280_));
                this.model.renderToBuffer(p_115311_, vertexconsumer, p_115313_, i, k);
            }

            if (this.shouldRenderLayers(p_364280_)) {
                for (RenderLayer<S, M> renderlayer : this.layers) {
                    if (renderlayer instanceof CustomHeadLayer) {
                        renderItemHead = true;
                    }

                    renderlayer.render(p_115311_, p_115312_, p_115313_, p_364280_, p_364280_.yRot, p_364280_.xRot);
                    renderItemHead = false;
                }
            }

            if (Config.isShaders()) {
                Shaders.setEntityColor(0.0F, 0.0F, 0.0F, 0.0F);
            }

            p_115311_.popPose();
            super.render(p_364280_, p_115311_, p_115312_, p_115313_);
            if (Reflector.ForgeEventFactoryClient_onRenderLivingPost.exists()) {
                Reflector.ForgeEventFactoryClient_onRenderLivingPost.callVoid(p_364280_, this, p_115311_, p_115312_, p_115313_);
            }
        }
    }

    protected boolean shouldRenderLayers(S pRenderState) {
        return true;
    }

    protected int getModelTint(S pRenderState) {
        return -1;
    }

    public abstract ResourceLocation getTextureLocation(S pRenderState);

    @Nullable
    protected RenderType getRenderType(S pRenderState, boolean pIsVisible, boolean pRenderTranslucent, boolean pAppearsGlowing) {
        ResourceLocation resourcelocation = this.getTextureLocation(pRenderState);
        if (this.model.locationTextureCustom != null) {
            resourcelocation = this.model.locationTextureCustom;
        } else if (this.getLocationTextureCustom() != null) {
            resourcelocation = this.getLocationTextureCustom();
        }

        if (pRenderTranslucent) {
            return RenderType.itemEntityTranslucentCull(resourcelocation);
        } else if (pIsVisible) {
            return this.model.renderType(resourcelocation);
        } else if (pAppearsGlowing && !Config.getMinecraft().levelRenderer.shouldShowEntityOutlines()) {
            return this.model.renderType(resourcelocation);
        } else {
            return pAppearsGlowing ? RenderType.outline(resourcelocation) : null;
        }
    }

    public static int getOverlayCoords(LivingEntityRenderState pRenderState, float pOverlay) {
        return OverlayTexture.pack(OverlayTexture.u(pOverlay), OverlayTexture.v(pRenderState.hasRedOverlay));
    }

    protected boolean isBodyVisible(S pRenderState) {
        return !pRenderState.isInvisible;
    }

    private static float sleepDirectionToRotation(Direction pFacing) {
        switch (pFacing) {
            case SOUTH:
                return 90.0F;
            case WEST:
                return 0.0F;
            case NORTH:
                return 270.0F;
            case EAST:
                return 180.0F;
            default:
                return 0.0F;
        }
    }

    protected boolean isShaking(S pRenderState) {
        return pRenderState.isFullyFrozen;
    }

    protected void setupRotations(S pRenderState, PoseStack pPoseStack, float pBodyRot, float pScale) {
        if (this.isShaking(pRenderState)) {
            pBodyRot += (float)(Math.cos((double)((float)Mth.floor(pRenderState.ageInTicks) * 3.25F)) * Math.PI * 0.4F);
        }

        if (!pRenderState.hasPose(Pose.SLEEPING)) {
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F - pBodyRot));
        }

        if (pRenderState.deathTime > 0.0F) {
            float f = (pRenderState.deathTime - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * this.getFlipDegrees()));
        } else if (pRenderState.isAutoSpinAttack) {
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - pRenderState.xRot));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(pRenderState.ageInTicks * -75.0F));
        } else if (pRenderState.hasPose(Pose.SLEEPING)) {
            Direction direction = pRenderState.bedOrientation;
            float f1 = direction != null ? sleepDirectionToRotation(direction) : pBodyRot;
            pPoseStack.mulPose(Axis.YP.rotationDegrees(f1));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees()));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
        } else if (pRenderState.isUpsideDown) {
            pPoseStack.translate(0.0F, (pRenderState.boundingBoxHeight + 0.1F) / pScale, 0.0F);
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }

    protected float getFlipDegrees() {
        return 90.0F;
    }

    protected float getWhiteOverlayProgress(S pRenderState) {
        return 0.0F;
    }

    protected void scale(S pRenderState, PoseStack pPoseStack) {
    }

    protected boolean shouldShowName(T p_115333_, double p_365822_) {
        if (p_115333_.isDiscrete()) {
            float f = 32.0F;
            if (p_365822_ >= 1024.0) {
                return false;
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localplayer = minecraft.player;
        boolean flag = !p_115333_.isInvisibleTo(localplayer);
        if (p_115333_ != localplayer) {
            Team team = p_115333_.getTeam();
            Team team1 = localplayer.getTeam();
            if (team != null) {
                Team.Visibility team$visibility = team.getNameTagVisibility();
                switch (team$visibility) {
                    case ALWAYS:
                        return flag;
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return team1 == null ? flag : team.isAlliedTo(team1) && (team.canSeeFriendlyInvisibles() || flag);
                    case HIDE_FOR_OWN_TEAM:
                        return team1 == null ? flag : !team.isAlliedTo(team1) && flag;
                    default:
                        return true;
                }
            }
        }

        return Minecraft.renderNames() && p_115333_ != minecraft.getCameraEntity() && flag && !p_115333_.isVehicle();
    }

    public static boolean isEntityUpsideDown(LivingEntity pEntity) {
        if (pEntity instanceof Player || pEntity.hasCustomName()) {
            String s = ChatFormatting.stripFormatting(pEntity.getName().getString());
            if ("Dinnerbone".equals(s) || "Grumm".equals(s)) {
                return !(pEntity instanceof Player) || ((Player)pEntity).isModelPartShown(PlayerModelPart.CAPE);
            }
        }

        return false;
    }

    protected float getShadowRadius(S p_363803_) {
        return super.getShadowRadius(p_363803_) * p_363803_.scale;
    }

    public void extractRenderState(T p_368665_, S p_363057_, float p_364497_) {
        super.extractRenderState(p_368665_, p_363057_, p_364497_);
        float f = Mth.rotLerp(p_364497_, p_368665_.yHeadRotO, p_368665_.yHeadRot);
        p_363057_.bodyRot = solveBodyRot(p_368665_, f, p_364497_);
        p_363057_.yRot = Mth.wrapDegrees(f - p_363057_.bodyRot);
        p_363057_.xRot = p_368665_.getXRot(p_364497_);
        p_363057_.customName = p_368665_.getCustomName();
        p_363057_.isUpsideDown = isEntityUpsideDown(p_368665_);
        if (p_363057_.isUpsideDown) {
            p_363057_.xRot *= -1.0F;
            p_363057_.yRot *= -1.0F;
        }

        if (!p_368665_.isPassenger() && p_368665_.isAlive()) {
            p_363057_.walkAnimationPos = p_368665_.walkAnimation.position(p_364497_);
            p_363057_.walkAnimationSpeed = p_368665_.walkAnimation.speed(p_364497_);
        } else {
            p_363057_.walkAnimationPos = 0.0F;
            p_363057_.walkAnimationSpeed = 0.0F;
        }

        if (p_368665_.getVehicle() instanceof LivingEntity livingentity) {
            p_363057_.wornHeadAnimationPos = livingentity.walkAnimation.position(p_364497_);
        } else {
            p_363057_.wornHeadAnimationPos = p_363057_.walkAnimationPos;
        }

        p_363057_.scale = p_368665_.getScale();
        p_363057_.ageScale = p_368665_.getAgeScale();
        p_363057_.pose = p_368665_.getPose();
        p_363057_.bedOrientation = p_368665_.getBedOrientation();
        if (p_363057_.bedOrientation != null) {
            p_363057_.eyeHeight = p_368665_.getEyeHeight(Pose.STANDING);
        }

        p_363057_.isFullyFrozen = p_368665_.isFullyFrozen();
        p_363057_.isBaby = p_368665_.isBaby();
        p_363057_.isInWater = p_368665_.isInWater();
        if (Reflector.ForgeEntity_isInWaterOrSwimmable.exists()) {
            p_363057_.isInWater = Reflector.callBoolean(p_368665_, Reflector.ForgeEntity_isInWaterOrSwimmable);
        }

        label49: {
            p_363057_.isAutoSpinAttack = p_368665_.isAutoSpinAttack();
            p_363057_.hasRedOverlay = p_368665_.hurtTime > 0 || p_368665_.deathTime > 0;
            ItemStack itemstack = p_368665_.getItemBySlot(EquipmentSlot.HEAD);
            if (itemstack.getItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof AbstractSkullBlock abstractskullblock) {
                p_363057_.wornHeadType = abstractskullblock.getType();
                p_363057_.wornHeadProfile = itemstack.get(DataComponents.PROFILE);
                p_363057_.headItem.clear();
                break label49;
            }

            p_363057_.wornHeadType = null;
            p_363057_.wornHeadProfile = null;
            if (!HumanoidArmorLayer.shouldRender(itemstack, EquipmentSlot.HEAD)) {
                this.itemModelResolver.updateForLiving(p_363057_.headItem, itemstack, ItemDisplayContext.HEAD, false, p_368665_);
            } else {
                p_363057_.headItem.clear();
            }
        }

        p_363057_.deathTime = p_368665_.deathTime > 0 ? (float)p_368665_.deathTime + p_364497_ : 0.0F;
        Minecraft minecraft = Minecraft.getInstance();
        p_363057_.isInvisibleToPlayer = p_363057_.isInvisible && p_368665_.isInvisibleTo(minecraft.player);
        p_363057_.appearsGlowing = minecraft.shouldEntityAppearGlowing(p_368665_);
    }

    private static float solveBodyRot(LivingEntity pEntity, float pYHeadRot, float pPartialTick) {
        if (pEntity.getVehicle() instanceof LivingEntity livingentity) {
            float f2 = Mth.rotLerp(pPartialTick, livingentity.yBodyRotO, livingentity.yBodyRot);
            float f = 85.0F;
            float f1 = Mth.clamp(Mth.wrapDegrees(pYHeadRot - f2), -85.0F, 85.0F);
            f2 = pYHeadRot - f1;
            if (Math.abs(f1) > 50.0F) {
                f2 += f1 * 0.2F;
            }

            return f2;
        } else {
            return Mth.rotLerp(pPartialTick, pEntity.yBodyRotO, pEntity.yBodyRot);
        }
    }

    public <T extends RenderLayer> T getLayer(Class<T> cls) {
        List<T> list = this.getLayers(cls);
        return list.isEmpty() ? null : list.get(0);
    }

    public <T extends RenderLayer> List<T> getLayers(Class<T> cls) {
        List<RenderLayer> list = new ArrayList<>();

        for (RenderLayer renderlayer : this.layers) {
            if (cls.isInstance(renderlayer)) {
                list.add(renderlayer);
            }
        }

        return (List<T>)list;
    }

    public void removeLayers(Class cls) {
        Iterator iterator = this.layers.iterator();

        while (iterator.hasNext()) {
            RenderLayer renderlayer = (RenderLayer)iterator.next();
            if (cls.isInstance(renderlayer)) {
                iterator.remove();
            }
        }
    }

    public void replaceLayer(Class cls, RenderLayer layer) {
        int i = this.getLayerIndex(cls);
        this.removeLayers(cls);
        if (i >= 0) {
            this.layers.add(i, layer);
        } else {
            this.layers.add(layer);
        }
    }

    public int getLayerIndex(Class cls) {
        for (int i = 0; i < this.layers.size(); i++) {
            RenderLayer renderlayer = this.layers.get(i);
            if (cls.isInstance(renderlayer)) {
                return i;
            }
        }

        return -1;
    }

    public static boolean isRenderItemHead() {
        return renderItemHead;
    }
}