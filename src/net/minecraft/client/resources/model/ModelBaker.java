package net.minecraft.client.resources.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.VisibleForDebug;
import net.minecraftforge.client.RenderTypeGroup;

public interface ModelBaker {
    BakedModel bake(ResourceLocation pLocation, ModelState pTransform);

    SpriteGetter sprites();

    @VisibleForDebug
    ModelDebugName rootName();

    default RenderTypeGroup renderType() {
        return null;
    }
}