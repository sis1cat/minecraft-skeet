package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.optifine.Config;
import net.optifine.CustomItems;

public class WingsLayer<S extends HumanoidRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    private final ElytraModel elytraModel;
    private final ElytraModel elytraBabyModel;
    private final EquipmentLayerRenderer equipmentRenderer;

    public WingsLayer(RenderLayerParent<S, M> pRenderer, EntityModelSet pModels, EquipmentLayerRenderer pEquipmentRenderer) {
        super(pRenderer);
        this.elytraModel = new ElytraModel(pModels.bakeLayer(ModelLayers.ELYTRA));
        this.elytraBabyModel = new ElytraModel(pModels.bakeLayer(ModelLayers.ELYTRA_BABY));
        this.equipmentRenderer = pEquipmentRenderer;
    }

    public void render(PoseStack p_362037_, MultiBufferSource p_368252_, int p_364275_, S p_368470_, float p_368401_, float p_362513_) {
        ItemStack itemstack = p_368470_.chestEquipment;
        Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.assetId().isEmpty()) {
            ResourceLocation resourcelocation = getPlayerElytraTexture(p_368470_);
            ElytraModel elytramodel = p_368470_.isBaby ? this.elytraBabyModel : this.elytraModel;
            p_362037_.pushPose();
            p_362037_.translate(0.0F, 0.0F, 0.125F);
            elytramodel.setupAnim(p_368470_);
            this.equipmentRenderer
                .renderLayers(
                    EquipmentClientInfo.LayerType.WINGS,
                    equippable.assetId().get(),
                    elytramodel,
                    itemstack,
                    p_362037_,
                    p_368252_,
                    p_364275_,
                    resourcelocation
                );
            p_362037_.popPose();
        }
    }

    @Nullable
    protected static ResourceLocation getPlayerElytraTexture(HumanoidRenderState pRenderState) {
        if (Config.isCustomItems()) {
            ResourceLocation resourcelocation = CustomItems.getCustomElytraTexture(pRenderState.chestEquipment, null);
            if (resourcelocation != null) {
                return resourcelocation;
            }
        }

        if (pRenderState instanceof PlayerRenderState playerrenderstate) {
            PlayerSkin playerskin = playerrenderstate.skin;
            if (pRenderState.entity instanceof AbstractClientPlayer abstractclientplayer) {
                ResourceLocation resourcelocation1 = abstractclientplayer.getLocationElytra();
                if (resourcelocation1 != null) {
                    return resourcelocation1;
                }
            }

            if (Config.isShowCapes() && playerskin.elytraTexture() != null) {
                return playerskin.elytraTexture();
            }

            if (Config.isShowCapes() && playerskin.capeTexture() != null && playerrenderstate.showCape) {
                return playerskin.capeTexture();
            }
        }

        return null;
    }
}