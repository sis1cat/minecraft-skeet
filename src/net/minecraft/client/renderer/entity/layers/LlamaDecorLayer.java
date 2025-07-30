package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaDecorLayer extends RenderLayer<LlamaRenderState, LlamaModel> {
    private final LlamaModel adultModel;
    private final LlamaModel babyModel;
    private final EquipmentLayerRenderer equipmentRenderer;

    public LlamaDecorLayer(RenderLayerParent<LlamaRenderState, LlamaModel> pRenderer, EntityModelSet pModels, EquipmentLayerRenderer pEquipmentRenderer) {
        super(pRenderer);
        this.equipmentRenderer = pEquipmentRenderer;
        this.adultModel = new LlamaModel(pModels.bakeLayer(ModelLayers.LLAMA_DECOR));
        this.babyModel = new LlamaModel(pModels.bakeLayer(ModelLayers.LLAMA_BABY_DECOR));
    }

    public void render(PoseStack p_364604_, MultiBufferSource p_363218_, int p_361586_, LlamaRenderState p_367324_, float p_364047_, float p_367997_) {
        ItemStack itemstack = p_367324_.bodyItem;
        Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.assetId().isPresent()) {
            this.renderEquipment(p_364604_, p_363218_, p_367324_, itemstack, equippable.assetId().get(), p_361586_);
        } else if (p_367324_.isTraderLlama) {
            this.renderEquipment(p_364604_, p_363218_, p_367324_, ItemStack.EMPTY, EquipmentAssets.TRADER_LLAMA, p_361586_);
        }
    }

    private void renderEquipment(
        PoseStack pPoseStack, MultiBufferSource pBufferSource, LlamaRenderState pRenderState, ItemStack pStack, ResourceKey<EquipmentAsset> pEquipmentAsset, int pPackedLight
    ) {
        LlamaModel llamamodel = pRenderState.isBaby ? this.babyModel : this.adultModel;
        llamamodel.setupAnim(pRenderState);
        this.equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, pEquipmentAsset, llamamodel, pStack, pPoseStack, pBufferSource, pPackedLight);
    }
}