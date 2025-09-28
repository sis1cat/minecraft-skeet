package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PiglinHeadModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.dragon.DragonHeadModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.reflect.Reflector;

public class SkullBlockRenderer implements BlockEntityRenderer<SkullBlockEntity> {
    private final Function<SkullBlock.Type, SkullModelBase> modelByType;
    private static final Map<SkullBlock.Type, ResourceLocation> SKIN_BY_TYPE = Util.make(Maps.newHashMap(), mapIn -> {
        mapIn.put(SkullBlock.Types.SKELETON, ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png"));
        mapIn.put(SkullBlock.Types.WITHER_SKELETON, ResourceLocation.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png"));
        mapIn.put(SkullBlock.Types.ZOMBIE, ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png"));
        mapIn.put(SkullBlock.Types.CREEPER, ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper.png"));
        mapIn.put(SkullBlock.Types.DRAGON, ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon.png"));
        mapIn.put(SkullBlock.Types.PIGLIN, ResourceLocation.withDefaultNamespace("textures/entity/piglin/piglin.png"));
        mapIn.put(SkullBlock.Types.PLAYER, DefaultPlayerSkin.getDefaultTexture());
    });
    private static Map<SkullBlock.Type, SkullModelBase> globalModels = new HashMap<>();
    private static Map<SkullBlock.Type, Function<EntityModelSet, SkullModelBase>> customModels;

    @Nullable
    public static SkullModelBase createModel(EntityModelSet pModelSet, SkullBlock.Type pType) {
        if (CustomEntityModels.isActive()) {
            SkullModelBase skullmodelbase = globalModels.get(pType);
            if (skullmodelbase != null) {
                return skullmodelbase;
            }
        }

        if (pType instanceof SkullBlock.Types skullblock$types) {
            return (SkullModelBase)(switch (skullblock$types) {
                case SKELETON -> new SkullModel(pModelSet.bakeLayer(ModelLayers.SKELETON_SKULL));
                case WITHER_SKELETON -> new SkullModel(pModelSet.bakeLayer(ModelLayers.WITHER_SKELETON_SKULL));
                case PLAYER -> new SkullModel(pModelSet.bakeLayer(ModelLayers.PLAYER_HEAD));
                case ZOMBIE -> new SkullModel(pModelSet.bakeLayer(ModelLayers.ZOMBIE_HEAD));
                case CREEPER -> new SkullModel(pModelSet.bakeLayer(ModelLayers.CREEPER_HEAD));
                case DRAGON -> new DragonHeadModel(pModelSet.bakeLayer(ModelLayers.DRAGON_SKULL));
                case PIGLIN -> new PiglinHeadModel(pModelSet.bakeLayer(ModelLayers.PIGLIN_HEAD));
            });
        } else if (Reflector.ForgeEventFactoryClient_onCreateSkullModels.exists()) {
            if (customModels == null) {
                customModels = (Map<SkullBlock.Type, Function<EntityModelSet, SkullModelBase>>)Reflector.ForgeEventFactoryClient_onCreateSkullModels.call();
            }

            return customModels.getOrDefault(pType, k -> null).apply(pModelSet);
        } else {
            return null;
        }
    }

    public SkullBlockRenderer(BlockEntityRendererProvider.Context pContext) {
        EntityModelSet entitymodelset = pContext.getModelSet();
        this.modelByType = Util.memoize(typeIn -> createModel(entitymodelset, typeIn));
    }

    public void render(SkullBlockEntity p_112534_, float p_112535_, PoseStack p_112536_, MultiBufferSource p_112537_, int p_112538_, int p_112539_) {
        float f = p_112534_.getAnimation(p_112535_);
        BlockState blockstate = p_112534_.getBlockState();
        boolean flag = blockstate.getBlock() instanceof WallSkullBlock;
        Direction direction = flag ? blockstate.getValue(WallSkullBlock.FACING) : null;
        int i = flag ? RotationSegment.convertToSegment(direction.getOpposite()) : blockstate.getValue(SkullBlock.ROTATION);
        float f1 = RotationSegment.convertToDegrees(i);
        SkullBlock.Type skullblock$type = ((AbstractSkullBlock)blockstate.getBlock()).getType();
        SkullModelBase skullmodelbase = this.modelByType.apply(skullblock$type);
        RenderType rendertype = getRenderType(skullblock$type, p_112534_.getOwnerProfile());
        renderSkull(direction, f1, f, p_112536_, p_112537_, p_112538_, skullmodelbase, rendertype);
    }

    public static void renderSkull(
        @Nullable Direction pDirection,
        float pYRot,
        float pMouthAnimation,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        SkullModelBase pModel,
        RenderType pRenderType
    ) {
        pPoseStack.pushPose();
        if (pDirection == null) {
            pPoseStack.translate(0.5F, 0.0F, 0.5F);
        } else {
            float f = 0.25F;
            pPoseStack.translate(0.5F - (float)pDirection.getStepX() * 0.25F, 0.25F, 0.5F - (float)pDirection.getStepZ() * 0.25F);
        }

        pPoseStack.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(pRenderType);
        pModel.setupAnim(pMouthAnimation, pYRot, 0.0F);
        pModel.renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY);
        pPoseStack.popPose();
    }

    public static RenderType getRenderType(SkullBlock.Type pType, @Nullable ResolvableProfile pProfile) {
        return getRenderType(pType, pProfile, null);
    }

    public static RenderType getRenderType(SkullBlock.Type pType, @Nullable ResolvableProfile pProfile, @Nullable ResourceLocation pTextureOverride) {
        return pType == SkullBlock.Types.PLAYER && pProfile != null
            ? RenderType.entityTranslucent(pTextureOverride != null ? pTextureOverride : Minecraft.getInstance().getSkinManager().getInsecureSkin(pProfile.gameProfile()).texture())
            : RenderType.entityCutoutNoCullZOffset(pTextureOverride != null ? pTextureOverride : SKIN_BY_TYPE.get(pType));
    }

    public static Map<SkullBlock.Type, SkullModelBase> getGlobalModels() {
        return globalModels;
    }
}