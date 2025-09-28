package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerCapeModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.optifine.Config;

public class CapeLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    private final HumanoidModel<PlayerRenderState> model;
    private final EquipmentAssetManager equipmentAssets;

    public CapeLayer(RenderLayerParent<PlayerRenderState, PlayerModel> pRenderer, EntityModelSet pModelSet, EquipmentAssetManager pEquipmentAssets) {
        super(pRenderer);
        this.model = new PlayerCapeModel<>(pModelSet.bakeLayer(ModelLayers.PLAYER_CAPE));
        this.equipmentAssets = pEquipmentAssets;
    }

    private boolean hasLayer(ItemStack pStack, EquipmentClientInfo.LayerType pLayer) {
        Equippable equippable = pStack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.assetId().isEmpty()) {
            EquipmentClientInfo equipmentclientinfo = this.equipmentAssets.get(equippable.assetId().get());
            return !equipmentclientinfo.getLayers(pLayer).isEmpty();
        } else {
            return false;
        }
    }

    public void render(PoseStack p_116615_, MultiBufferSource p_116616_, int p_116617_, PlayerRenderState p_367257_, float p_116619_, float p_116620_) {
        if (!p_367257_.isInvisible && p_367257_.showCape) {
            PlayerSkin playerskin = p_367257_.skin;
            AbstractClientPlayer abstractclientplayer = p_367257_.entity instanceof AbstractClientPlayer ? (AbstractClientPlayer)p_367257_.entity : null;
            ResourceLocation resourcelocation = abstractclientplayer != null ? abstractclientplayer.getLocationCape() : playerskin.capeTexture();
            if (resourcelocation != null && !this.hasLayer(p_367257_.chestEquipment, EquipmentClientInfo.LayerType.WINGS)) {
                p_116615_.pushPose();
                if (this.hasLayer(p_367257_.chestEquipment, EquipmentClientInfo.LayerType.HUMANOID)) {
                    p_116615_.translate(0.0F, -0.053125F, 0.06875F);
                }

                if (p_367257_.capeFlap < -5.0F) {
                    p_367257_.capeFlap = -5.0F;
                }

                if (abstractclientplayer != null) {
                    float f = Config.getAverageFrameTimeSec() * 20.0F;
                    f = Config.limit(f, 0.02F, 1.0F);
                    abstractclientplayer.capeFlap = Mth.lerp(f, abstractclientplayer.capeFlap, p_367257_.capeFlap);
                    abstractclientplayer.capeLean = Mth.lerp(f, abstractclientplayer.capeLean, p_367257_.capeLean);
                    abstractclientplayer.capeLean2 = Mth.lerp(f, abstractclientplayer.capeLean2, p_367257_.capeLean2);
                    p_367257_.capeFlap = abstractclientplayer.capeFlap;
                    p_367257_.capeLean = abstractclientplayer.capeLean;
                    p_367257_.capeLean2 = abstractclientplayer.capeLean2;
                }

                VertexConsumer vertexconsumer = p_116616_.getBuffer(RenderType.entitySolid(resourcelocation));
                this.getParentModel().copyPropertiesTo(this.model);
                this.model.setupAnim(p_367257_);
                this.model.renderToBuffer(p_116615_, vertexconsumer, p_116617_, OverlayTexture.NO_OVERLAY);
                p_116615_.popPose();
            }
        }
    }
}