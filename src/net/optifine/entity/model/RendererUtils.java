package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RendererUtils {
    public static Map<EntityType<?>, EntityRenderer<?, ?>> getEntityRenderMap() {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        return entityrenderdispatcher.getEntityRenderMap();
    }

    public static Map<BlockEntityType, BlockEntityRenderer> getBlockEntityRenderMap() {
        BlockEntityRenderDispatcher blockentityrenderdispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        return blockentityrenderdispatcher.getBlockEntityRenderMap();
    }

    public static Map<SkullBlock.Type, SkullModelBase> getSkullModelMap() {
        return SkullBlockRenderer.getGlobalModels();
    }
}
