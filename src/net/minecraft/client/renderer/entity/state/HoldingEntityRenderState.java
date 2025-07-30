package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HoldingEntityRenderState extends LivingEntityRenderState {
    public final ItemStackRenderState heldItem = new ItemStackRenderState();

    public static void extractHoldingEntityRenderState(LivingEntity pEntity, HoldingEntityRenderState pReusedState, ItemModelResolver pResolver) {
        pResolver.updateForLiving(pReusedState.heldItem, pEntity.getMainHandItem(), ItemDisplayContext.GROUND, false, pEntity);
    }
}