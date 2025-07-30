package net.optifine.entity.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.util.Either;

public class VirtualEntityRenderer implements IEntityRenderer {
    private Either<EntityType, BlockEntityType> type;
    private ResourceLocation locationTextureCustom;

    @Override
    public Either<EntityType, BlockEntityType> getType() {
        return this.type;
    }

    @Override
    public void setType(Either<EntityType, BlockEntityType> type) {
        this.type = type;
    }

    @Override
    public ResourceLocation getLocationTextureCustom() {
        return this.locationTextureCustom;
    }

    @Override
    public void setLocationTextureCustom(ResourceLocation locationTextureCustom) {
        this.locationTextureCustom = locationTextureCustom;
    }

    @Override
    public void setShadowSize(float shadowSize) {
    }

    public void register() {
    }
}