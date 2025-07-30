package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModelAdapterTrappedChestLeft extends ModelAdapterChestDoubleLeft {
    public ModelAdapterTrappedChestLeft() {
        super(BlockEntityType.TRAPPED_CHEST, "trapped_chest_left", ModelLayers.DOUBLE_CHEST_LEFT);
    }
}