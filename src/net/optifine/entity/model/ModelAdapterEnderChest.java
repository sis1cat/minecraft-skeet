package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModelAdapterEnderChest extends ModelAdapterChest {
    public ModelAdapterEnderChest() {
        super(BlockEntityType.ENDER_CHEST, "ender_chest", ModelLayers.CHEST);
    }
}