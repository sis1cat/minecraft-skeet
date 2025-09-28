package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.optifine.Config;
import net.optifine.entity.model.CustomStaticModels;
import net.optifine.shaders.Shaders;
import net.optifine.util.ArrayUtils;

public class ParrotOnShoulderLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    private ParrotModel model;
    private final ParrotRenderState parrotState = new ParrotRenderState();
    private ParrotModel parrotModelOriginal;

    public ParrotOnShoulderLayer(RenderLayerParent<PlayerRenderState, PlayerModel> pRenderer, EntityModelSet pModelSet) {
        super(pRenderer);
        this.model = new ParrotModel(pModelSet.bakeLayer(ModelLayers.PARROT));
        this.parrotState.pose = ParrotModel.Pose.ON_SHOULDER;
        this.parrotModelOriginal = this.model;
    }

    public void render(PoseStack p_117318_, MultiBufferSource p_117319_, int p_117320_, PlayerRenderState p_365020_, float p_117322_, float p_117323_) {
        Parrot.Variant parrot$variant = p_365020_.parrotOnLeftShoulder;
        if (parrot$variant != null) {
            this.renderOnShoulder(p_117318_, p_117319_, p_117320_, p_365020_, parrot$variant, p_117322_, p_117323_, true);
        }

        Parrot.Variant parrot$variant1 = p_365020_.parrotOnRightShoulder;
        if (parrot$variant1 != null) {
            this.renderOnShoulder(p_117318_, p_117319_, p_117320_, p_365020_, parrot$variant1, p_117322_, p_117323_, false);
        }
    }

    private void renderOnShoulder(
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        PlayerRenderState pRenderState,
        Parrot.Variant pVariant,
        float pYRot,
        float pXRot,
        boolean pLeftShoulder
    ) {
        Entity entity = pRenderState.entity;
        Entity entity1 = Config.getEntityRenderDispatcher().getRenderedEntity();
        if (entity instanceof AbstractClientPlayer abstractclientplayer) {
            CompoundTag compoundtag = pLeftShoulder ? abstractclientplayer.getShoulderEntityLeft() : abstractclientplayer.getShoulderEntityRight();
            Entity entity2 = pLeftShoulder ? abstractclientplayer.entityShoulderLeft : abstractclientplayer.entityShoulderRight;
            if (entity2 == null) {
                entity2 = this.makeEntity(compoundtag, abstractclientplayer);
                if (entity2 instanceof ShoulderRidingEntity) {
                    if (pLeftShoulder) {
                        abstractclientplayer.entityShoulderLeft = (ShoulderRidingEntity)entity2;
                    } else {
                        abstractclientplayer.entityShoulderRight = (ShoulderRidingEntity)entity2;
                    }
                }
            }

            if (entity2 != null) {
                entity2.xo = entity1.xo;
                entity2.yo = entity1.yo;
                entity2.zo = entity1.zo;
                entity2.setPosRaw(entity1.getX(), entity1.getY(), entity1.getZ());
                entity2.xRotO = entity1.xRotO;
                entity2.yRotO = entity1.yRotO;
                entity2.setXRot(entity1.getXRot());
                entity2.setYRot(entity1.getYRot());
                if (entity2 instanceof LivingEntity && entity1 instanceof LivingEntity) {
                    ((LivingEntity)entity2).yBodyRotO = ((LivingEntity)entity1).yBodyRotO;
                    ((LivingEntity)entity2).yBodyRot = ((LivingEntity)entity1).yBodyRot;
                }

                Config.getEntityRenderDispatcher().setRenderedEntity(entity2);
                if (Config.isShaders()) {
                    Shaders.nextEntity(entity2);
                }
            }
        }

        this.model = ArrayUtils.firstNonNull(CustomStaticModels.getParrotModel(), this.parrotModelOriginal);
        pPoseStack.pushPose();
        pPoseStack.translate(pLeftShoulder ? 0.4F : -0.4F, pRenderState.isCrouching ? -1.3F : -1.5F, 0.0F);
        this.parrotState.ageInTicks = pRenderState.ageInTicks;
        this.parrotState.walkAnimationPos = pRenderState.walkAnimationPos;
        this.parrotState.walkAnimationSpeed = pRenderState.walkAnimationSpeed;
        this.parrotState.yRot = pYRot;
        this.parrotState.xRot = pXRot;
        this.model.setupAnim(this.parrotState);
        this.model
            .renderToBuffer(pPoseStack, pBuffer.getBuffer(this.model.renderType(ParrotRenderer.getVariantTexture(pVariant))), pPackedLight, OverlayTexture.NO_OVERLAY);
        pPoseStack.popPose();
        Config.getEntityRenderDispatcher().setRenderedEntity(entity1);
        if (Config.isShaders()) {
            Shaders.nextEntity(entity1);
        }
    }

    private Entity makeEntity(CompoundTag compoundtag, Player player) {
        Optional<EntityType<?>> optional = EntityType.by(compoundtag);
        if (!optional.isPresent()) {
            return null;
        } else {
            Entity entity = optional.get().create(player.level(), EntitySpawnReason.JOCKEY);
            if (entity == null) {
                return null;
            } else {
                entity.load(compoundtag);
                SynchedEntityData synchedentitydata = entity.getEntityData();
                if (synchedentitydata != null) {
                    synchedentitydata.spawnPosition = player.blockPosition();
                    synchedentitydata.spawnBiome = player.level().getBiome(synchedentitydata.spawnPosition).value();
                }

                return entity;
            }
        }
    }
}